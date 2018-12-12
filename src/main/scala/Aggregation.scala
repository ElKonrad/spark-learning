import org.apache.spark.{SparkConf, SparkContext}

object Aggregation {

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf().setMaster("local").setAppName("spark-frist")
    val sc = new SparkContext(sparkConf)

    val input = sc.textFile("/home/konrad/IdeaProjects/spark-learning/src/main/resources/randomtext.txt")
    val wordCount = input.flatMap(line => line.split(" ")).map(word => (word, 1)).reduceByKey((x, y) => x + y)
    wordCount.foreach { case (x, y) => println(x, y) }

    sc.stop()
  }
}