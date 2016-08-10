package utils.db

import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.concurrent.Akka
import java.sql.Connection
import play.api.db.DB
import play.api.Play.current

trait BlockingJDBC {

	// dbExcon is passed implicitly to Future.apply()
	private implicit val dbExcon:ExecutionContext = Akka.system.dispatchers.lookup("play.akka.actor.db")

	def sqlWithFuture[T](sql: Connection => T) = Future(DB.withConnection(con => sql(con)))

	def sqlTransactionWithFuture[T](sql: Connection => T) = Future(DB.withTransaction(con => sql(con)))

}
