package pl.spark.learning.sql

import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf

object SqlExamples {

  def main(args: Array[String]): Unit = {
    val spark = MySparkConf.sparkSession("Spark SQL basic example")
    import spark.implicits._

    //Creating DataFrame
    val peopleDF = spark.read.json(ResourceHelper.getResourceFilepath("people.json"))
    peopleDF.show()

    /*
    Untyped Dataset Operations (aka DataFrame Operations)
     */
    peopleDF.printSchema()
    // root
    // |-- age: long (nullable = true)
    // |-- name: string (nullable = true)


    peopleDF.select("name").show()
    peopleDF.select($"name", $"age" + 1).show()
    peopleDF.groupBy("age").avg("age").show()

    /*
    Running SQL Queries Programmatically
     */
    peopleDF.createOrReplaceTempView("people")
    val sqlDF = spark.sql("SELECT * FROM people")
    sqlDF.show()
    // +----+-------+
    // | age|   name|
    // +----+-------+
    // |null|Michael|
    // |  30|   Andy|
    // |  19| Justin|
    // +----+-------+

    spark.sql("SELECT age, COUNT(*) FROM people GROUP BY age").show()
    //+----+--------+
    //| age|count(1)|
    //  +----+--------+
    //|  19|       1|
    //|null|       1|
    //|  30|       1|
    //+----+--------+

    /*
    Creating Datasets
     */
    val caseClassDS = Seq(Person("Andy", 32)).toDS()
    caseClassDS.printSchema()
    caseClassDS.show()
    // +----+---+
    // |name|age|
    // +----+---+
    // |Andy| 32|
    // +----+---+

    val datasetFromDataframe = peopleDF.as[Person]
  }
}