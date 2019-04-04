package pl.spark.learning.sql

import java.util.Objects.nonNull
import java.util.concurrent.TimeUnit

import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.{DataFrame, Dataset, Encoder, Encoders, SparkSession}
import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf

import scala.util.Try

object EventsLogProcessing {

  def init(runOnCluster: Boolean): (SparkSession, DataFrame) = {
    if (runOnCluster) {
      val spark = SparkSession.builder().appName("spark-sql-events-log-processing").getOrCreate()
      val eventsDF = spark.read.json("s3a://spark.example.bucket/logsexample.json")
      (spark, eventsDF)
    } else {
      val spark = MySparkConf.sparkSession("SQL Events log processor")
      val eventsDF = spark.read.json(ResourceHelper.getResourceFilepath("logsexample.json"))
      (spark, eventsDF)
    }
  }

  def main(args: Array[String]): Unit = {
    val runOnCluster = Try(args(0).toBoolean).getOrElse(false)
    val runFirstUseCase = Try(args(1).toBoolean).getOrElse(false)
    val runSecondUseCase = Try(args(2).toBoolean).getOrElse(false)
    val runThirdUseCase = Try(args(3).toBoolean).getOrElse(false)
    val runFourthUseCase = Try(args(4).toBoolean).getOrElse(false)

    val (spark, eventsDF) = init(runOnCluster)

    import org.apache.spark.sql.functions._
    import spark.implicits._

    val orderData = eventsDF.as[OrderData]

    def groupedOrderEventsCount(orderData: Dataset[OrderData]): Unit = {
      orderData
        .groupBy("eventName")
        .count()
        .orderBy("count")
        .show()
    }

    def ordersLongestTimeLastedInDays(orderData: Dataset[OrderData]): Unit = {
      val orderUDAF = OrdersLongestTimeLastedInDays.toColumn.name("ordersLongestTimeLastedInDays")
      orderData
        .groupByKey(_.orderId)
        .agg(orderUDAF)
        .filter(_._2 >= 0)
        .orderBy(desc("ordersLongestDuration"))
        .limit(10)
        .show(false)
    }

    def ordersLongestTimeLastedFromApprovedToShippedInDays(orderData: Dataset[OrderData]): Unit = {
      val orderUDAF = OrdersLongestTimeLastedFromApprovedToShippedInDays.toColumn
        .name("ordersLongestTimeLastedFromApprovedToShippedInDays")

      orderData
        .groupByKey(_.orderId)
        .agg(orderUDAF)
        .filter(_._2 >= 0)
        .orderBy(desc("ordersLongestDurationFromApprovedToShipped"))
        .limit(10)
        .show(false)
    }

    def customersWithMostOrders(orderData: Dataset[OrderData]): Unit = {
      orderData
        .filter($"customerId".isNotNull)
        .groupBy("customerId")
        .count()
        .orderBy(desc("count"))
        .limit(10)
        .show(false)
    }

    if(runFirstUseCase)
      groupedOrderEventsCount(orderData)

    if (runSecondUseCase)
      ordersLongestTimeLastedInDays(orderData)

    if (runThirdUseCase)
      ordersLongestTimeLastedFromApprovedToShippedInDays(orderData)

    if (runFourthUseCase)
      customersWithMostOrders(orderData)

    spark.stop()
  }

  case class OrderFromCreatedToCompleted(var id: String, var createdDate: Long, var completedDate: Long)

  object OrdersLongestTimeLastedInDays extends Aggregator[OrderData, OrderFromCreatedToCompleted, Long] {

    def zero = OrderFromCreatedToCompleted(null, 0L, 0L)

    def reduce(buffer: OrderFromCreatedToCompleted,
               orderData: OrderData): OrderFromCreatedToCompleted = {
      buffer.id = orderData.orderId
      if (nonNull(orderData.createdDate)) {
        buffer.createdDate = toDate(orderData.createdDate)
      }
      if (nonNull(orderData.completedDate)) {
        buffer.completedDate = toDate(orderData.completedDate)
      }
      buffer
    }

    def merge(buffer1: OrderFromCreatedToCompleted,
              buffer2: OrderFromCreatedToCompleted): OrderFromCreatedToCompleted = {
      if (nonNull(buffer2.id))
        buffer1.id = buffer2.id
      if (buffer2.createdDate != 0L)
        buffer1.createdDate = buffer2.createdDate
      if (buffer2.completedDate != 0L)
        buffer1.completedDate = buffer2.completedDate
      buffer1
    }

    def finish(reduction: OrderFromCreatedToCompleted): Long =
      TimeUnit.MILLISECONDS.toDays(reduction.completedDate - reduction.createdDate)

    def bufferEncoder: Encoder[OrderFromCreatedToCompleted] = Encoders.product

    def outputEncoder: Encoder[Long] = Encoders.scalaLong
  }

  case class OrderFromApprovedToShipped(var id: String, var approvedDate: Long, var shippedDate: Long)

  object OrdersLongestTimeLastedFromApprovedToShippedInDays extends Aggregator[OrderData, OrderFromApprovedToShipped, Long] {
    def zero: OrderFromApprovedToShipped = OrderFromApprovedToShipped(null, 0L, 0L)

    def reduce(buffer: OrderFromApprovedToShipped, orderData: OrderData): OrderFromApprovedToShipped = {
      buffer.id = orderData.orderId
      if (nonNull(orderData.approvedDate)) {
        buffer.approvedDate = toDate(orderData.approvedDate)
      }
      if (nonNull(orderData.shippedDate)) {
        buffer.shippedDate = toDate(orderData.shippedDate)
      }
      buffer
    }

    def merge(b1: OrderFromApprovedToShipped, b2: OrderFromApprovedToShipped): OrderFromApprovedToShipped = {
      if (nonNull(b2.id))
        b1.id = b2.id
      if (nonNull(b2.approvedDate))
        b1.approvedDate = b2.approvedDate
      if (nonNull(b2.shippedDate))
        b1.shippedDate = b2.shippedDate
      b1
    }

    def finish(reduction: OrderFromApprovedToShipped): Long = TimeUnit.MILLISECONDS.toDays(reduction.shippedDate - reduction.approvedDate)

    def bufferEncoder: Encoder[OrderFromApprovedToShipped] = Encoders.product

    def outputEncoder: Encoder[Long] = Encoders.scalaLong
  }

  def toDate(d: String): Long = {
    import java.text.SimpleDateFormat
    import java.util.{Calendar, TimeZone}
    val tz = TimeZone.getTimeZone("Europe/Warsaw")
    val cal = Calendar.getInstance(tz)
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    sdf.setCalendar(cal)
    cal.setTime(sdf.parse(d))
    cal.getTime.getTime
  }

}

case class OrderData(id: String,
                     eventName: String,
                     status: String,
                     orderId: String,
                     customerId: String,
                     items: List[Item],
                     createdDate: String,
                     validatedDate: String,
                     approvedDate: String,
                     shippedDate: String,
                     completedDate: String)

case class Item(id: String, description: String, quantity: String, price: Double, category: String)