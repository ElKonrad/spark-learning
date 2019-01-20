package pl.spark.learning.rdd

import pl.spark.learning.conf.MySparkConf
import org.apache.spark.SparkContext
import pl.spark.learning.ResourceHelper

object LogQuery {

  def main(args: Array[String]) {

    val sc = new SparkContext(MySparkConf.sparkConf("Log Query"))

    val dataSet = sc.textFile(ResourceHelper.getResourceFilepath("sl4j-spring-boot.txt"))
    val apacheLogRegex =
      """(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}.\d{3})(\s*)([^ ]*) ([^ ]*) --- \[(.*)\] ((?:[a-zA-Z0-9-]+\.)+[A-Za-z0-9$_]+)(\s*):(.*)$""".r

    class Stats(val count: Int, val logLevel: String) extends Serializable {
      def merge(other: Stats): Stats = new Stats(count + other.count, logLevel)

      override def toString: String = "logLevel=%s\tn=%s".format(logLevel, count)
    }

    def extractKey(line: String): Option[(String, String, String)] =
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(date, time, _, logLevel, pid, threadName, className, _, logMessage)) =>
          Some((className, threadName, date))
        case _ => None
      }

    def extractStats(line: String): Stats =
      apacheLogRegex.findFirstIn(line) match {
        case Some(apacheLogRegex(date, time, _, logLevel, pid, threadName, className, _, logMessage)) =>
          new Stats(1, logLevel)
        case _ => new Stats(1, "")
      }

    dataSet.map(line => (extractKey(line), extractStats(line)))
      .filter(_._1.isDefined)
      .reduceByKey((a, b) => a.merge(b))
      .collect().foreach {
      case (user, query) => println("%s\t%s".format(user, query))
    }

    sc.stop()
  }
}
