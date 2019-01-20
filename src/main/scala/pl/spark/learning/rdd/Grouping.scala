package pl.spark.learning.rdd

import pl.spark.learning.conf.MySparkConf
import org.apache.spark.{SparkConf, SparkContext}

object Grouping {

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = MySparkConf.sparkConf("spark-grouping")
    val sc = new SparkContext(sparkConf)

    val storeAddress = sc.parallelize(List(
      (Store("Ritual"), "1026 Valencia St"),
      (Store("Philz"), "748 Van Ness Ave"),
      (Store("Philz"), "3101 24th St"),
      (Store("Starbucks"), "Seattle")))
    val storeRating = sc.parallelize(List((Store("Ritual"), 4.9), (Store("Philz"), 4.8)))

    val groupByKeyExample = storeAddress.groupByKey()
    val joinExample = storeAddress.join(storeRating)
    val leftJoinExample = storeAddress.leftOuterJoin(storeRating)
    val rightJoinExample = storeAddress.rightOuterJoin(storeRating)

    groupByKeyExample.foreach(println)
    joinExample.foreach(println)
    leftJoinExample.foreach(println)
    rightJoinExample.foreach(println)
  }
}

case class Store(name: String)