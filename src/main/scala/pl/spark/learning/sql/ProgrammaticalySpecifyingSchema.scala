package pl.spark.learning.sql

import org.apache.spark.sql.Row
import pl.spark.learning.ResourceHelper
import pl.spark.learning.conf.MySparkConf
import org.apache.spark.sql.types._

object ProgrammaticalySpecifyingSchema {

  def main(args: Array[String]): Unit = {
    val spark = MySparkConf.sparkSession("Spark SQL - Inferring Schema Using Reflection")

    import spark.implicits._

    val peopleRDD = spark.sparkContext
      .textFile(ResourceHelper.getResourceFilepath("people.txt"))

    // The schema is encoded in a string
    val schemaString = "name age"

    val fields = schemaString.split(" ").map(fieldName => StructField(fieldName, StringType, nullable = true))
    val schema = StructType(fields)

    // Convert records of the RDD (people) to Rows
    val rowRDD = peopleRDD
      .map(_.split(","))
      .map(attributes => Row(attributes(0), attributes(1).trim))

    val peopleDF = spark.createDataFrame(rowRDD, schema)
    peopleDF.createOrReplaceTempView("people")

    val results = spark.sql("SELECT name FROM people")

    // The results of SQL queries are DataFrames and support all the normal RDD operations
    // The columns of a row in the result can be accessed by field index or by field name
    results.map(attributes => "Name: " + attributes(0)).show()
    // +-------------+
    // |        value|
    // +-------------+
    // |Name: Michael|
    // |   Name: Andy|
    // | Name: Justin|
    // +-------------+
  }
}
