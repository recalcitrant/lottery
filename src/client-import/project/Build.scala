import sbt._
import Keys._
import sbtassembly.Plugin._

object ClientImportSettings {

	val buildOrganization = "yogasurftech"
	val buildScalaVersion = "2.10.1"
	val buildVersion = "0.0.1"

	val projectSettings = Defaults.defaultSettings ++ Seq(
		name := "clientimport",
		version := buildVersion,
		organization := buildOrganization,
		scalaVersion := buildScalaVersion,
		resolvers ++= Seq(
			"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
		),
		libraryDependencies ++= Seq(
			"com.typesafe.slick" %% "slick" % "1.0.0",
			"mysql" % "mysql-connector-java" % "5.1.24",
			"org.slf4j" % "slf4j-nop" % "1.7.5",
			"org.mindrot" % "jbcrypt" % "0.3m"
		))
}

object MonitoringBuild extends Build {

	import ClientImportSettings._

	lazy val root = Project("clientimport", file("."), settings = projectSettings ++  assemblySettings ++ Seq(description := "A lottery-client import application"))
}