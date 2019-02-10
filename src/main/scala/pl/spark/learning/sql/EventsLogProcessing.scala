package pl.spark.learning.sql

import java.util.concurrent.TimeUnit

import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.{Encoder, Encoders}
import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf

object EventsLogProcessing {

  def main(args: Array[String]): Unit = {
    val spark = MySparkConf.sparkSession("SQL Events log processor")
    import org.apache.spark.sql.functions._
    import spark.implicits._

    val eventsDF = spark.read.json(ResourceHelper.getResourceFilepath("logsexample.json"))

    val orderData = eventsDF.as[OrderData]

    orderData
      .groupBy("eventName")
      .count()
      .orderBy($"value")
      .show()

    val orderAgg = OrderAgg.toColumn.name("order_agg")
    val result = orderData
      .groupByKey(_.orderId)
      .agg(orderAgg)
      .filter(_._2 >= 0)
      .orderBy(desc("order_agg"))
      .limit(10)
    result.show()

  }

  case class OrderTemp(var id: String, var createdDate: Long, var completedDate: Long)

  object OrderAgg extends Aggregator[OrderData, OrderTemp, Long] {
    def zero: OrderTemp = OrderTemp(null, 0L, 0L)

    def reduce(buffer: OrderTemp, orderData: OrderData): OrderTemp = {
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

      buffer.id = orderData.orderId
      if (orderData.createdDate != null) {
        buffer.createdDate = toDate(orderData.createdDate)
      }
      if (orderData.completedDate != null) {
        buffer.completedDate = toDate(orderData.completedDate)
      }
      buffer
    }

    def merge(b1: OrderTemp, b2: OrderTemp): OrderTemp = {
      if (b2.id != null)
        b1.id = b2.id
      if (b2.createdDate != 0L)
        b1.createdDate = b2.createdDate
      if (b2.completedDate != 0L)
        b1.completedDate = b2.completedDate
      b1
    }

    def finish(reduction: OrderTemp): Long = TimeUnit.MILLISECONDS.toDays(reduction.completedDate - reduction.createdDate)

    def bufferEncoder: Encoder[OrderTemp] = Encoders.product

    def outputEncoder: Encoder[Long] = Encoders.scalaLong
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
                     completedDate: String
                    )

case class Item(id: String, description: String, quantity: String, price: Double, category: String)