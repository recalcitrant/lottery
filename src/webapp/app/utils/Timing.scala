package utils

import play.api.Logger

trait Timing {

  def time[T](fn: => T, msg: String = "job ") = {
    val startTime = System.currentTimeMillis
    val result = fn
    log(result, msg, System.currentTimeMillis - startTime)
  }

  def log[T](result: T, msg: String, duration: Long): T = {
    Logger.debug(msg + " took " + duration + " ms")
    result
  }
}