import java.text.SimpleDateFormat
import java.util.Date

import conf.MySparkConf
import org.apache.spark.{SparkConf, SparkContext}

object Order {

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = MySparkConf.get("spark-orders")
    val sc = new SparkContext(sparkConf)
    val dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    val ordersInput = sc.textFile(ResourceHelper.getResourceFilepath("orders.txt"))
    val visitsInput = sc.textFile(ResourceHelper.getResourceFilepath("visits.txt"))

    val orders = ordersInput.map(line => line.split(" "))
      .map(w => Order(dateFormat.parse(w(0) + " " + w(1)), w(2), w(3), w(4)))
      .map(order => ((order.clientId, order.productId), order))

    val visits = visitsInput.map(line => line.split(" "))
      .map(w => Visit(dateFormat.parse(w(0) + " " + w(1)), w(2), w(3)))
      .map(visit => ((visit.clientId, visit.productId), visit))

    val ordersWithVisits = orders.leftOuterJoin(visits)
      .filter(r => r._2._1.date.after(r._2._2.get.date))

    val visitsCountPerClientAndBoughtProduct = ordersWithVisits.map(r => (r._1, 1))
      .reduceByKey((a, b) => a + b)

    visitsCountPerClientAndBoughtProduct.foreach(println)

    sc.stop()
  }
}

case class Order(date: Date, clientId: String, productId: String, processId: String)

case class Visit(date: Date, clientId: String, productId: String)
