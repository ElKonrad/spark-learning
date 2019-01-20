package pl.spark.learning.conf

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

object MySparkConf {
  def sparkConf(appName: String): SparkConf = new SparkConf()
    .setMaster("local[4]")
    .setAppName(appName)
    .set("spark.eventLog.enabled", "true")
    .set("spark.eventLog.dir", "file:///home/konrad/spark/logs")

  def sparkSession(appName: String): SparkSession = SparkSession
    .builder()
    .master("local")
    .appName(appName)
    .getOrCreate()
}
