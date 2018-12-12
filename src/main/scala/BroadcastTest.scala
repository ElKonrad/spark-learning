import org.apache.spark.{SparkConf, SparkContext}

object BroadcastTest {
  def main(args: Array[String]) {

    val sparkConf = new SparkConf().setMaster("local").setAppName("spark-broadcast")
    val sc = new SparkContext(sparkConf)

    val slices = 2
    val num = 1000000

    val arr1 = (0 until num).toArray

    for (i <- 0 until 3) {
      println(s"Iteration $i")
      println("===========")
      val startTime = System.nanoTime
      val barr1 = sc.broadcast(arr1)
      val observedSizes = sc.parallelize(1 to 10, slices)
        .map(_ => barr1.value.length)

      observedSizes.collect().foreach(println(_))
      println("Iteration %d took %.0f milliseconds".format(i, (System.nanoTime - startTime) / 1E6))
    }

    Thread.sleep(10000000)
    sc.stop()
  }
}