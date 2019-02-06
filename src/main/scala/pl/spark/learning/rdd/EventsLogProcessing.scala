package pl.spark.learning.rdd

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

    def extractLog(line: String): Option[Event] = {
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(date, time, _, logLevel, pid, threadName, className, logMessage)) =>
          implicit val formats = Serialization.formats(NoTypeHints) + new UUIDserializer
          className match {
            case "OrderCreated" =>
              Some(Serialization.read[OrderCreated](logMessage))
            case "OrderValidated" =>
              Some(Serialization.read[OrderValidated](logMessage))
            case "OrderApproved" =>
              Some(Serialization.read[OrderApproved](logMessage))
            case "OrderShipped" =>
              Some(Serialization.read[OrderShipped](logMessage))
            case "OrderCompleted" =>
              Some(Serialization.read[OrderCompleted](logMessage))
            case _ => None
          }
        case _ => None
      }
    }

    dataSet.map(line => extractLog(line))
      .filter(_.isDefined)
      .map(_.get)
      .map(p => (p.eventName, 1))
      .reduceByKey((a, b) => a + b)
      .sortBy(p => p._2, false)
      .foreach(println)

    sc.stop()
  }
}