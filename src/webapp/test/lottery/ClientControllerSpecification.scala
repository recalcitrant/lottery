package lottery

import models.client.Client
import org.specs2.execute.{AsResult, Result}
import play.api.libs.json._
import play.api.libs.ws.WS
import play.api.test._
import utils.Config

object ClientControllerSpecification extends PlaySpecification {

  val TEST_ID = "TEST_ID"
  val TEST_FIRSTNAME = "TEST_FIRST_NAME"
  val TEST_LASTNAME = "TEST_LAST_NAME"
  val TEST_BID = "TEST_BID"
  val TEST_HID = "TEST_HID"
  val TEST_PASSWORD = "TEST_PASSWORD"
  val TEST_MAIL = "TEST_MAIL"

  implicit val clientJsonFormat = models.client.ClientJsonFormat.ClientFormat
  private val CONTENT_TYPE_JSON = ("Content-Type", "application/json; charset=utf-8")

  private var server = ""

  abstract class WithDbData extends WithApplication {
    override def around[T: AsResult](t: => T): Result = super.around {
      setupData()
      t
    }

    def setupData() {
    }
  }


  "The Client Controller Spec" should {

    "pass all tests" in {
      new WithDbData {
        server = Config.testString("test.host")


        /*val fact = new TestHandlerFactory(new MessageValidator("Anmeldung zur Gewinnbenachrichtigung", MessageValidator.TEST_SUBJECT))
        BasicSMTPServer.start(fact)*/

        val client = testClientAdd(94059541.toString, "testuser@example.com", "Joe", "Last-Name-Jena")
        testClientAddFailure(94059541.toString, "testuser@example.com")


        val client2 = testClientAdd(24150001.toString, "testuser@example.com", "Joe", "Last-Name-Emmerich-Rees")
        testClientAddFailure(24150001.toString, "testuser@example.com")

        testClientLoginFailure(client)
        testClientLoginFailure(client2)

        testClientSendPasswordFailure(80055008.toString, "testuser@example.com")
        testClientSendPasswordFailure(15051732.toString, "testuser@example.com")

        testClientSendPasswordSuccess(24150001.toString, "testuser@example.com")
        testClientSendPasswordSuccess(94059541.toString, "testuser@example.com")

        testClientUpdate(client.id.get, client.hid, client.branch.code)
        testClientUpdate(client2.id.get, client2.hid, client2.branch.code)

        testClientSendPasswordSuccess(24150001.toString, "testuser@example.com")
        testClientSendPasswordSuccess(94059541.toString, "testuser@example.com")

        testClientDelete(client)
        testClientDelete(client2)

        testClientSendPasswordFailure(24150001.toString, "testuser@example.com")
        testClientSendPasswordFailure(94059541.toString, "testuser@example.com")
      }
    }
  }

  private def testClientAdd(bid: String, email: String, firstname: String, lastname: String) = {
    val postJson = Config.testString("test.client.add").replace(TEST_BID, bid).
      replace(TEST_FIRSTNAME, firstname).
      replace(TEST_MAIL, email).
      replace(TEST_LASTNAME, lastname)
    val res = await(jsonRequest("client/add").post(postJson), 10000).body
    val json = Json.parse(res) \ "client"
    val client = json.as[models.client.Client]
    client.firstName mustEqual firstname
    client.lastName mustEqual lastname
    client
  }

  private def testClientAddFailure(bid: String, email: String) {
    val json = Config.testString("test.client.add.failure").replace(TEST_BID, bid).replace(TEST_MAIL, email)
    val res = await(jsonRequest("client/add").post(json)).body
    res mustEqual Config.testString("test.client.add.failure.expected.result")
  }

  private def testClientSendPasswordFailure(bid: String, email: String) {
    val json = Config.testString("test.client.send.password.failure").replace(TEST_BID, bid).replace(TEST_MAIL, email)
    val res = await(jsonRequest("client/sendpassword").post(json)).body
    res mustEqual Config.testString("test.client.send.password.failure.expected.result")
  }

  private def testClientSendPasswordSuccess(bid: String, email: String) {
    val json = Config.testString("test.client.send.password.success").replace(TEST_BID, bid).replace(TEST_MAIL, email)
    val res = await(jsonRequest("client/sendpassword").post(json)).body
    res mustEqual Config.testString("test.client.send.password.success.expected.result")
  }

  private def testClientUpdate(id: Long, hid: String, bid: Long) {
    val json = Config.testString("test.client.update").replace(TEST_ID, id.toString).
      replace(TEST_HID, hid).
      replace(TEST_BID, bid.toString)
    val res = await(jsonRequest("client/update").post(json)).body
    res mustEqual Config.testString("test.client.update.expected.result")
  }

  private def testClientDelete(client: Client) {
    val json = Config.testString("test.client.delete").replace(TEST_ID, client.id.get.toString).
      replace(TEST_HID, client.hid).replace(TEST_BID, client.branch.code.toString).replace(TEST_LASTNAME, client.lastName)
    val res = await(jsonRequest("client/update").post(json)).body
    res mustEqual Config.testString("test.client.delete.expected.result")
  }

  private def testClientLoginFailure(client: Client) {
    val json = Config.testString("test.client.login.failure").replace(TEST_PASSWORD, client.password.getOrElse("undefined")).
      replace(TEST_BID, client.branch.code.toString).
      replace(TEST_MAIL, client.email.getOrElse("testuser@example.com"))
    val res = await(jsonRequest("client/login").post(json)).body
    res mustEqual Config.testString("test.client.login.failure.expected.result")
  }

  private def testLatestDrawingsByBranchId() {
    val res = await(WS.url(server + "client/drawing/35850000").get()).body
    res must contain("8.999.999.991,00 €")
  }

  private def testDrawingQuery() {
    val res = await(jsonRequest("client/drawing/14").post(Config.testString("test.query.anonymous"))).body
    res must contain("{\"finaldigits\":\"211817\",\"type\":\"1\",\"prize\":\"50,00 €\"}")
  }

  private def testClientLogin(pw: String) {
    val json = Config.testString("test.client.login.success").replace(TEST_PASSWORD, pw)
    val res = await(jsonRequest("client/login").post(json)).body
    res must contain(Config.testString("test.client.login.success.expected.result"))
  }

  private def jsonRequest(url: String) = WS.url(server + url).withHeaders(CONTENT_TYPE_JSON)

  private def checkLoginScreenPresent(b: TestBrowser) = {
    b.goTo(server)
    b.pageSource must contain("username")
    b.pageSource must contain("password")
  }
}
