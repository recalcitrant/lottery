package controllers

import play.api.Play.current
import scala.concurrent.{Future, ExecutionContext}
import play.api.cache.{Cache, Cached}
import play.api.mvc._

trait DBAction extends Controller {

  import ExecutionContext.Implicits.global

  def DBAction(r: => Future[SimpleResult]) = Action.async(r)

  def DBCachedAction(duration: Int, future: => Future[SimpleResult]) =
    Cached(request => request.uri, duration) {
      Action.async(future)
    }

  def DBCachedActionWithKey(key: String, expiration: Int)(future: => Future[SimpleResult]) = {
    Action.async {
      Cache.getAs[SimpleResult](key) map (Future(_)) getOrElse {
        future.onSuccess {
          case res => Cache set(key, res, expiration)
        }
        future
      }
    }
  }
}