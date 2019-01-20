package pl.spark.learning.sql

import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import pl.spark.learning.conf.MySparkConf

object DataFrameExamples {

  implicit class DataFrameFlattener(df: DataFrame) {
    def flattenSchema: DataFrame = {
      df.select(flatten(Nil, df.schema): _*)
    }

    protected def flatten(path: Seq[String], schema: DataType): Seq[Column] = schema match {
      case s: StructType => s.fields.flatMap(f => flatten(path :+ f.name, f.dataType))
      case other => col(path.map(n => s"`$n`").mkString(".")).as(path.mkString(".")) :: Nil
    }
  }

  val department1 = Department("123456", "Computer Science")
  val department2 = Department("789012", "Mechanical Engineering")
  val department3 = Department("345678", "Theater and Drama")
  val department4 = Department("901234", "Indoor Recreation")

  val employee1 = Employee("michael", "armbrust", "no-reply@berkeley.edu", 100000)
  val employee2 = Employee("xiangrui", "meng", "no-reply@stanford.edu", 120000)
  val employee3 = Employee("matei", null, "no-reply@waterloo.edu", 140000)
  val employee4 = Employee(null, "wendell", "no-reply@princeton.edu", 160000)

  val departmentWithEmployees1 = DepartmentWithEmployees(department1, Seq(employee1, employee2))
  val departmentWithEmployees2 = DepartmentWithEmployees(department2, Seq(employee3, employee4))
  val departmentWithEmployees3 = DepartmentWithEmployees(department3, Seq(employee1, employee4))
  val departmentWithEmployees4 = DepartmentWithEmployees(department4, Seq(employee2, employee3))

  val departmentsWithEmployeesSeq1: Seq[DepartmentWithEmployees] = Seq(departmentWithEmployees1, departmentWithEmployees2)
  val departmentsWithEmployeesSeq2: Seq[DepartmentWithEmployees] = Seq(departmentWithEmployees3, departmentWithEmployees4)

  def main(args: Array[String]): Unit = {

    val spark: SparkSession = MySparkConf.sparkSession("Data frames Examples")
    import spark.implicits._

    val df1: DataFrame = spark.createDataFrame(departmentsWithEmployeesSeq1)
    val df2: DataFrame = spark.createDataFrame(departmentsWithEmployeesSeq2)

    //Union two DataFrames
    val unionDF = df1.union(df2)
    unionDF.show(false)

    //Flatten and explode employees column
    val employeesDF = unionDF.select(explode($"employees"))
    employeesDF.show()
    val flattenDF = employeesDF.flattenSchema
    flattenDF.show()
    val columnsRenamed = Seq("firstName", "lastName", "email", "salary")
    val explodeDF = flattenDF.toDF(columnsRenamed: _*)
    explodeDF.show()

    //Print schema in nice tree format and select specified columns
    unionDF.printSchema()
    unionDF.select("department.name", "employees.email").show(false)
  }
}

case class Department(id: String, name: String)

case class Employee(firstName: String, lastName: String, email: String, salary: Int)

case class DepartmentWithEmployees(department: Department, employees: Seq[Employee])