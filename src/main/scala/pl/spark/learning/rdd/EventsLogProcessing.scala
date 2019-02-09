package pl.spark.learning.rdd

import java.util.concurrent.TimeUnit
import java.util.{Date, UUID}

import net.liftweb.json._
import org.apache.spark.SparkContext
import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf
import pl.spark.learning.generators._

object EventsLogProcessing {
  def main(args: Array[String]) {

    val sc = new SparkContext(MySparkConf.sparkConf("Events log processor"))

    val dataSet = sc.textFile(ResourceHelper.getResourceFilepath("logsexample.txt"))
    val apacheLogRegex =
      """(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}.\d{3})(\s*)([^ ]*) ([^ ]*) --- \[(.*)\] ([A-Za-z0-9$_]+):(.*)$""".r

    def extractLog(line: String): Option[OrderData] = {
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(date, time, _, logLevel, pid, threadName, className, logMessage)) =>
          implicit val formats = Serialization.formats(NoTypeHints) + new UUIDserializer
          className match {
            case "OrderCreated" =>
              val orderCreated = Serialization.read[OrderCreated](logMessage)
              val orderData = OrderData(
                orderCreated.id, orderCreated.eventName, orderCreated.status, orderCreated.orderId,
                customerId = Some(orderCreated.customerId),
                items = Some(orderCreated.items),
                createdDate = Some(orderCreated.createdDate)
              )
              Some(orderData)
            case "OrderValidated" =>
              val orderValidated = Serialization.read[OrderValidated](logMessage)
              val orderData = OrderData(
                orderValidated.id, orderValidated.eventName, orderValidated.status, orderValidated.orderId,
                validatedDate = Some(orderValidated.validatedDate)
              )
              Some(orderData)
            case "OrderApproved" =>
              val orderApproved = Serialization.read[OrderApproved](logMessage)
              val orderData = OrderData(
                orderApproved.id, orderApproved.eventName, orderApproved.status, orderApproved.orderId,
                approvedDate = Some(orderApproved.approvedDate)
              )
              Some(orderData)
            case "OrderShipped" =>
              val orderShipped = Serialization.read[OrderShipped](logMessage)
              val orderData = OrderData(
                orderShipped.id, orderShipped.eventName, orderShipped.status, orderShipped.orderId,
                shippedDate = Some(orderShipped.shippedDate)
              )
              Some(orderData)
            case "OrderCompleted" =>
              val orderCompleted = Serialization.read[OrderCompleted](logMessage)
              val orderData = OrderData(
                orderCompleted.id, orderCompleted.eventName, orderCompleted.status, orderCompleted.orderId,
                completedDate = Some(orderCompleted.completedDate)
              )
              Some(orderData)
            case _ => None
          }
        case _ => None
      }
    }

    def groupedOrderEventsCount(): Unit = {
      dataSet.map(extractLog)
        .filter(_.isDefined)
        .map(_.get)
        .map(o => (o.eventName, 1))
        .reduceByKey((a, b) => a + b)
        .sortBy(p => p._2, false)
        .foreach(println)
    }

    def ordersLongestTimeLasted(orderNumberToTake: Int): Unit = {
      dataSet.map(extractLog)
        .filter(_.isDefined)
        .map(_.get)
        .groupBy(_.orderId)
        .filter(isCompleted)
        .map(orderTimeInMillis)
        .sortBy(p => p._2, false)
        .map(orderTimeToDays)
        .take(orderNumberToTake)
        .foreach(println)
    }

    groupedOrderEventsCount()
    ordersLongestTimeLasted(orderNumberToTake = 5)

    sc.stop()
  }

  private def isCompleted(tuple: (UUID, Iterable[OrderData])): Boolean = {
    tuple._2.size == 5
  }

  private def orderTimeInMillis(order: (UUID, Iterable[OrderData])): (UUID, Long) = {
    val orders = order._2
    val diff = orders.last.completedDate.get.getTime - orders.head.createdDate.get.getTime
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