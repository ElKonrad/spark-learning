package conf

import org.apache.spark.SparkConf

object MySparkConf {
  def get(appName: String): SparkConf = new SparkConf()
    .setMaster("local")
    .setAppName(appName)
    .set("spark.eventLog.enabled", "true")
    .set("spark.eventLog.dir", "file:///home/konrad/spark/logs")
}
