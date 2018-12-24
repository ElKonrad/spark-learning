import conf.MySparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object SparkFirst {

  case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = MySparkConf.get("spark-frist")
    val sc = new SparkContext(sparkConf)
    val people: RDD[String] = sc.textFile(ResourceHelper.getResourceFilepath("people.txt"))
    val maturePeopleCount = people.map(line => line.split(","))
      .map(x => Person(x(0), x(1).trim.toInt))
      .filter(_.age >= 18)
      .count()

    println(maturePeopleCount)

    sc.stop()
  }
}