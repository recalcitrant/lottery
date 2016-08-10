import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

	val appName = "lottery"
	val appVersion = "2.0"

	resolvers += "clojars" at "http://clojars.org/repo/"

	val appDependencies = Seq(
		jdbc,
		anorm,
		cache,
		"mysql" % "mysql-connector-java" % "5.1.34",
		"org.seleniumhq.selenium" % "selenium-firefox-driver" % "2.39.0" % "test",
		"org.seleniumhq.selenium" % "selenium-java" % "2.39.0" % "test",
		"org.apache.commons" % "commons-email" % "1.3.3",
		"org.subethamail" % "subethasmtp" % "3.1.7",
		"org.mindrot" % "jbcrypt" % "0.3m",
		"org.clojure" % "clojure" % "1.6.0"
		//"com.typesafe" %% "play-plugins-mailer" % "2.1-RC2"
	)

	val main = play.Project(appName, appVersion, appDependencies).settings(
		scalacOptions ++= Seq("-feature", "-language:postfixOps", "-language:existentials")
	)
}
