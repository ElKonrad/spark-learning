package pl.spark.learning.rdd

import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}

import net.liftweb.json._
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf
import pl.spark.learning.generators._

import scala.util.Try

object EventsLogProcessing {
  def init(runOnCluster: Boolean): (SparkContext, RDD[String]) = {
    if (runOnCluster) {
      val spark = new SparkContext(new SparkConf().setAppName("spark-rdd-events-log-processing"))
      val dataSet = spark.textFile("s3a://spark.example.bucket/logsexample.txt")
      (spark, dataSet)
    } else {
      val spark = new SparkContext(MySparkConf.sparkConf("RDD Events log processor"))
      val dataSet = spark.textFile(ResourceHelper.getResourceFilepath("logsexample.txt"))
      (spark, dataSet)
    }
  }

  def main(args: Array[String]) {

    val runOnCluster = Try(args(0).toBoolean).getOrElse(false)
    val runFirstUseCase = Try(args(1).toBoolean).getOrElse(false)
    val runSecondUseCase = Try(args(2).toBoolean).getOrElse(false)
    val runThirdUseCase = Try(args(3).toBoolean).getOrElse(false)
    val runFourthUseCase = Try(args(4).toBoolean).getOrElse(false)

    val (spark, dataSet) = init(runOnCluster)

    val apacheLogRegex =
      """(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}.\d{3})(\s*)([^ ]*) ([^ ]*) --- \[(.*)\] ([A-Za-z0-9$_]+):(.*)$""".r

    def extractLog(line: String): Option[OrderData] = {
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(date, time, _, logLevel, pid, threadName, className, logMessage)) =>
          className match {
            case "OrderCreated" =>
              OrderEventDeserializer.toOrderCreated(logMessage)
            case "OrderValidated" =>
              OrderEventDeserializer.toOrderValidated(logMessage)
            case "OrderApproved" =>
              OrderEventDeserializer.toOrderApproved(logMessage)
            case "OrderShipped" =>
              OrderEventDeserializer.toOrderShipped(logMessage)
            case "OrderCompleted" =>
              OrderEventDeserializer.toOrderCompleted(logMessage)
            case _ => None
          }
        case _ => None
      }
    }

    def groupedOrderEventsCount(): Unit = {
      dataSet.map(extractLog)
        .filter(_.isDefined)
        .map(_.get)
        .map(order => (order.eventName, 1))
        .reduceByKey((a, b) => a + b)
        .sortBy(eventTypeCount => eventTypeCount._2, ascending = false)
        .foreach(println)
    }

    def ordersLongestTimeLastedInDays(ordersToTake: Int): Unit = {
      dataSet.map(extractLog)
        .filter(_.isDefined)
        .map(_.get)
        .groupBy(_.orderId)
        .filter(isCompleted)
        .map(orderTimeInMillis)
        .map(orderTimeToDays)
        .sortBy(orderTimeInDays => orderTimeInDays._2, ascending = false)
        .take(ordersToTake)
        .foreach(println)
    }

    def ordersLongestTimeLastedFromApprovedToShippedInDays(ordersToTake: Int): Unit = {
      dataSet.map(extractLog)
        .filter(_.isDefined)
        .map(_.get)
        .groupBy(_.orderId)
        .filter(order => isShipped(order) || isCompleted(order))
        .map(orderTimeFromApprovedToShippedInMillis)
        .map(orderTimeToDays)
        .sortBy(orderTimeInDays => orderTimeInDays._2, ascending = false)
        .take(ordersToTake)
        .foreach(println)
    }

    def customersWithMostOrders(customersToTake: Int): Unit = {
      dataSet.map(extractLog)
        .filter(_.isDefined)
        .map(_.get)
        .flatMap(_.customerId)
        .map(customer => (customer, 1))
        .reduceByKey((a, b) => a + b)
        .sortBy(customerCount => customerCount._2, ascending = false)
        .take(customersToTake)
        .foreach(println)
    }

    if (runFirstUseCase)
      groupedOrderEventsCount()
    if (runSecondUseCase)
      ordersLongestTimeLastedInDays(ordersToTake = 10)
    if (runThirdUseCase)
      ordersLongestTimeLastedFromApprovedToShippedInDays(ordersToTake = 10)
    if (runFourthUseCase)
      customersWithMostOrders(customersToTake = 10)

    spark.stop()
  }

  private def isCompleted(tuple: (UUID, Iterable[OrderData])): Boolean = {
    tuple._2.size == 5
  }

  private def isShipped(tuple: (UUID, Iterable[OrderData])): Boolean = {
    tuple._2.size == 4
  }

  private def orderTimeInMillis(order: (UUID, Iterable[OrderData])): (UUID, Long) = {
    val orders = order._2
    val completedOrder = orders.filter(_.completedDate.isDefined).head
    val createdOrder = orders.filter(_.createdDate.isDefined).head

    val diff = completedOrder.completedDate.get.getTime - createdOrder.createdDate.get.getTime
    (order._1, diff)
  }

  private def orderTimeFromApprovedToShippedInMillis(order: (UUID, Iterable[OrderData])): (UUID, Long) = {
    val orders = order._2.toList
    val approved = orders.filter(_.approvedDate.isDefined).head
    val shipped = orders.filter(_.shippedDate.isDefined).head
    val diff = shipped.shippedDate.get.getTime - approved.approvedDate.get.getTime
    (order._1, diff)
  }

  private def orderTimeToDays(p: (UUID, Long)): (UUID, Long) = {
    val (orderId: UUID, timeInMillis: Long) = p
    (orderId, TimeUnit.MILLISECONDS.toDays(timeInMillis))
  }

}

case class OrderData(id: UUID,
                     eventName: String,
                     status: String,
                     orderId: UUID,
                     customerId: Option[UUID] = None,
                     items: Option[List[Item]] = None,
                     createdDate: Option[Date] = None,
                     validatedDate: Option[Date] = None,
                     approvedDate: Option[Date] = None,
                     shippedDate: Option[Date] = None,
                     completedDate: Option[Date] = None
                    )