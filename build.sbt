name := "spark-learning"

version := "0.1"

scalaVersion := "2.11.12"

libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-sql" % "2.4.0"
libraryDependencies += "org.apache.spark" %% "spark-mllib" % "2.4.0"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.490"
libraryDependencies += "net.liftweb" %% "lift-json" % "3.3.0"