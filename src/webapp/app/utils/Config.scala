package utils

import play.api.Play.current
import java.util.Properties

object Config {
  
  private val conf = current.configuration  
  private val tFile = this.getClass.getClassLoader.getResourceAsStream("test.properties")
  private val testConf = new Properties()
  testConf.load(tFile)
  
  def testString(key:String) = testConf.getProperty(key)

  def getString(key:String) = conf.getString(key).getOrElse("config.key.not.found")
  def getLong(key:String) = conf.getInt(key).map(_.toLong).getOrElse(-1l)

}