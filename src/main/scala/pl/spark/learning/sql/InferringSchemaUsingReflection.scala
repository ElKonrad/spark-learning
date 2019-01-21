package pl.spark.learning.sql

import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf

object InferringSchemaUsingReflection {

  def main(args: Array[String]): Unit = {

    val spark = MySparkConf.sparkSession("Spark SQL - Inferring Schema Using Reflection")

    import spark.implicits._

    val peopleDF = spark.sparkContext
      .textFile(ResourceHelper.getResourceFilepath("people.txt"))
      .map(_.split(","))
      .map(attributes => Person(attributes(0), attributes(1).trim.toLong))
      .toDF()

    peopleDF.createOrReplaceTempView("people")
    val teenagersDF = spark.sql("SELECT name, age FROM people WHERE age BETWEEN 13 AND 19")

    // Access by field index
    teenagersDF.map(teenager => "Name: " + teenager(0)).show()
    // +------------+
    // |       value|
    // +------------+
    // |Name: Justin|
    // +------------+


    // Access by field name
    teenagersDF.map(teenager => "Name: " + teenager.getAs[String]("name")).show()
    // +------------+
    // |       value|
    // +------------+
    // |Name: Justin|
    // +------------+


    // No pre-defined encoders for Dataset[Map[K,V]], define explicitly
    implicit val mapEncoder = org.apache.spark.sql.Encoders.kryo[Map[String, Any]]
    // Primitive types and case classes can be also defined as
    // implicit val stringIntMapEncoder: Encoder[Map[String, Any]] = ExpressionEncoder()

    // row.getValuesMap[T] retrieves multiple columns at once into a Map[String, T]
    teenagersDF.map(teenager => teenager.getValuesMap[Any](List("name", "age"))).collect()
    // Array(Map("name" -> "Justin", "age" -> 19))
  }

}
