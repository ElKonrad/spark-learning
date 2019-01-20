package pl.spark.learning.rdd

import pl.spark.learning.conf.MySparkConf
import org.apache.spark.{SparkConf, SparkContext}
import pl.spark.learning.ResourceHelper

import scala.util.Random

object Aggregation {

  val airlineRates = List("emirates", "iberia", "qantas", "lot", "iberia", "iberia", "qantas", "lot", "iberia")
    .map(airline => (airline, Random.nextInt(10)))

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = MySparkConf.sparkConf("spark-aggregation")
    val sc = new SparkContext(sparkConf)

    val input = sc.textFile(ResourceHelper.getResourceFilepath("randomtext.txt"))

    val wordsCount = input.flatMap(line => line.split(" "))
      .map(_.replaceAll("[-+.^:,]", ""))
      .map(word => (word, 1))
      .reduceByKey((x, y) => x + y)
      .sortBy(_._2, false)
    val topThreeOccurringWords = wordsCount.take(3)
    topThreeOccurringWords.foreach { case (x, y) => println(x, y) }


    val readyToCalculateAirlinesRateAverage = sc.parallelize(airlineRates)
      .mapValues(rate => (rate, 1))
      .reduceByKey((a, b) => (a._1 + b._1, a._2 + b._2))
    readyToCalculateAirlinesRateAverage.foreach(println)


    val readyToCalculateAirlinesRateAverageUsingCombining = sc.parallelize(airlineRates)
      .combineByKey(
        value => (value, 1),
        (acc: (Int, Int), element: Int) => (acc._1 + element, acc._2 + 1),
        (acc1: (Int, Int), acc2: (Int, Int)) => (acc1._1 + acc2._1, acc1._2 + acc2._2)
      )
    readyToCalculateAirlinesRateAverageUsingCombining.foreach(println)

    sc.stop()
  }
}
