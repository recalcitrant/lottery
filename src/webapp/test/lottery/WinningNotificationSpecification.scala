package lottery

import anorm._
import models.auth.User
import models.lottery.Branch
import models.lottery.drawing.Drawing
import org.specs2.execute.{Result, AsResult}
import play.api.db.DB
import play.api.test._
import models.client.{QueryTicketRange, UserQuery, Client}
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

object WinningNotificationSpecification extends PlaySpecification {

	val lotteryId = 1
	val drawingId = 618
	val freigeber = "sfpfreigeber"

	abstract class WithDbData extends WithApplication {

		override def around[T: AsResult](f: => T): Result = super.around {
			initData()
			f
		}

		def initData() {
			DB.withConnection {
				implicit con =>
					SQL("delete from client_history_mail").executeUpdate
					SQL("delete from client_tmp_mail").executeUpdate
					SQL("delete from client").executeUpdate
					SQL("delete from drawing").executeUpdate
					SQL("delete from dbase").executeUpdate
			}
			Seq(
				Client(NotAssigned, "hid", "Herr", "Torin", "testuser", Some("torin.testuser@gmail.com"), Some("secret"), Some(UserQuery(Seq(Some(QueryTicketRange("1", "1", 1, 1)), Some(QueryTicketRange("2", "2", 2, 2)), Some(QueryTicketRange("12", "12", 12, 12))))), Branch(NotAssigned, 94059541, "SFP-Filiale 1", "url", "url", hasWinningNotification = true)),
				Client(NotAssigned, "hid", "Herr", "Torin", "testuser", Some("torin.testuser@web.de"), Some("secret"), Some(UserQuery(Seq(Some(QueryTicketRange("3", "3", 3, 3))))), Branch(NotAssigned, 94059541, "SFP-Filiale 1", "url", "url", hasWinningNotification = true)),
				Client(NotAssigned, "hid", "Herr", "Torin", "testuser", Some("trajectory@web.de"), Some("secret"), Some(UserQuery(Seq(Some(QueryTicketRange("1", "123", 1, 123))))), Branch(NotAssigned, 94059541, "SFP-Filiale 1", "url", "url", hasWinningNotification = true)),
				Client(NotAssigned, "hid", "Herr", "Torin", "testuser", Some("recalcitrant@web.de"), Some("secret"), Some(UserQuery(Seq(Some(QueryTicketRange("1", "1000", 1, 1000)),Some(QueryTicketRange("1001", "2000", 1001, 2000))))), Branch(NotAssigned, 94059541, "SFP-Filiale 1", "url", "url", hasWinningNotification = true))
			).foreach(c => Client.add(c, "secret", "secret"))
		}
	}

	"The WinningNotification Spec" should {

		"send notification mails" in {
			new WithDbData {
				//var server = Config.testString("test.host")
				/*DB.withConnection {
					implicit con =>
						SQL(s"INSERT INTO `dbase` (`id`, `name`, `lottery_id`, `typ`) VALUES (63, 'Monatsauslosung', $lotteryId, 1)").executeUpdate
						SQL( """INSERT INTO `dbase_prize` (`id`, `drawing_base_id`, `typ`, `value`, `title`, `description`, `sort`) VALUES
									      (308, 63, 1, '250', NULL, NULL, 0),
									      (309, 63, 1, '500', NULL, NULL, 1),
									      (310, 63, 1, '2500', NULL, NULL, 2),
									      (311, 63, 1, '50000', NULL, NULL, 3),
									      (312, 63, 1, '500000', NULL, NULL, 4),
									      (313, 63, 1, '5000000', NULL, NULL, 5),
									      (314, 63, 1, '25000000', NULL, NULL, 6)""").executeUpdate
						SQL(s"INSERT INTO `drawing` (`id`, `drawing_date`, `drawing_date_next`, `drawing_base_id`, `date_winning_notification`) VALUES ($drawingId, '2014-08-21', '2014-09-22', 63, NULL);").executeUpdate
						SQL( """INSERT INTO `drawing_prizecount` (`id`, `drawing_id`, `dbase_prize_id`, `times`) VALUES
									      (3501, 618, 308, 534982),
									      (3502, 618, 309, 53290),
									      (3503, 618, 310, 10658),
									      (3504, 618, 311, 516),
									      (3505, 618, 312, 53),
									      (3506, 618, 313, 4),
									      (3507, 618, 314, 0)""").executeUpdate
						SQL( """INSERT INTO `drawing_prize` (`id`, `drawing_id`, `prize_id`) VALUES
									      (5504, 618, 308),
									      (5505, 618, 309),
									      (5506, 618, 310),
									      (5507, 618, 310),
									      (5508, 618, 311),
									      (5509, 618, 312),
									      (5510, 618, 313),
									      (5511, 618, 314)""").executeUpdate
						SQL( """INSERT INTO `drawing_finaldigit` (`id`, `drawing_prize_id`, `digits`) VALUES
									      (4155, 5504, '1'),
									      (4156, 5505, '63'),
									      (4157, 5506, '265'),
									      (4158, 5507, '501'),
									      (4159, 5508, '4693'),
									      (4160, 5509, '74965'),
									      (4161, 5510, '464726'),
									      (4162, 5511, '5308112')""").executeUpdate
						SQL(s"INSERT INTO `lottery`.`workflow_drawing` (`id`, `drawing_id`, `workflow_status_id`) VALUES ('1', '$drawingId', '1');").executeUpdate
				}*/
				User.byActiveUsername(freigeber).map { freigeber =>
					Await.result(Future {
						models.client.Client.addToWinningNotificationQueue(
							clients = models.client.Client.listByLotteryAndBranchesWithActiveNotification(lotteryId),
							drawing = Drawing.byId(drawingId, freigeber).get)
					}, 10 seconds)
				}
				/*
				  val json = Json.obj("action" -> JsString("freigeben"), "emailcomment" -> JsString("freigeben"))
				  User.byActiveUsername(freigeber).map { freigeber =>
					val hash = Crypto.sign(drawingId + "", Play.configuration.getString("form.hash.key").get.getBytes)
					FakeRequest(
						method = Helpers.POST,
						uri = s"/workflow/action/$drawingId/$hash/1",
						headers = FakeHeaders(Seq("Content-type" -> Seq("application/json"))),
						body = json
					).withSession((freigeber.username, "username"))
					status(result) must equalTo(OK)
				}*/
			}
		}
	}
}
