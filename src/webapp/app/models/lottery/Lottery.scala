package models.lottery

import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._
import utils.{Config, FieldEncrypt}
import java.io.File

case class Lottery(id: Pk[Long],
                   hid: String,
                   name: String,
                   nameshort: String,
                   lotteryName: String,
                   address: String,
                   zip: String,
                   city: String,
                   tel: String,
                   email: String,
                   lotteryType: LotteryType,
                   drawingsVisible: Int,
                   template: Int,
                   templateSuffix: String,
                   notificationConfig: WinningNotificatonConfig)

object Lottery {

  val sql = "select * from lottery l, lottery_type lt where lottery_type_id = lt.id "
  val sqlByBranch = "select * from lottery l, lottery_type lt, branch b where lottery_type_id = lt.id and b.lottery_id = l.id"
  val sqlId = "select l.id from lottery l, branch b where b.lottery_id = l.id and b.code = {bid}"
  val order = "order by l.id"

  private val uploadPath = Config.getString("upload.folder")

  // todo remove as being called from js in IF?
	def privacyTerms(bid: Long): Option[File] = DB.withConnection {
    implicit con =>
      val lid = SQL("SELECT l.id FROM lottery l, branch b WHERE b.lottery_id = l.id AND b.code = {bcode}").onParams(bid).as(scalar[Long].singleOpt)
      lid.map(id => new File(uploadPath + id.toString + "/misc/datenschutz.pdf"))
  }

  def getDrawingsVisible(lid: Long) = DB.withConnection {
    implicit con => SQL("select drawings_visible from lottery where id = {lid}").onParams(lid).as(scalar[Int].singleOpt)
  }

  def updateDrawingsVisible(dv: Int, lid: Long) = DB.withConnection(implicit con => {
    val res = SQL("update lottery set drawings_visible = {dv} where id = {lid}").onParams(dv, lid).executeUpdate()
    if (res > 0) Some(true) else None
  })


  def byId(id: Long): Option[Lottery] = DB.withConnection(implicit con =>
    SQL(sql + " and l.id = {id}").onParams(id).as(Lottery.withLotteryType.singleOpt))

  def byBranchCode(bcode: Long): Option[Lottery] = DB.withConnection(implicit con =>
    SQL(sqlByBranch + " and b.code = {bid}").onParams(bcode).as(Lottery.withLotteryType.singleOpt))

  def IdbyBranchCode(bcode: String): Option[Long] = DB.withConnection(implicit con =>
    SQL(sqlId).onParams(bcode).as(scalar[Long].singleOpt))

  def list = DB.withConnection(implicit con => SQL(sql).as(Lottery.withLotteryType *))

  private val instance = get[Pk[Long]]("lottery.id") ~ get[String]("lottery.name") ~ get[String]("lottery.name_short") ~ get[String]("lottery.name_lottery") ~
    get[String]("lottery.address") ~ get[String]("lottery.zip") ~ get[String]("lottery.city") ~
    get[String]("lottery.tel") ~ get[String]("lottery.email") ~ get[Long]("lottery.lottery_type_id") ~ get[Int]("lottery.drawings_visible") ~
    get[Int]("lottery.template") ~ get[String]("lottery.notification_server") ~ get[Int]("lottery.notification_port") ~ get[String]("lottery.notification_user") ~
    get[String]("lottery.notification_pass") ~ get[Int]("lottery.notification_tls") ~ get[String]("lottery.notification_from_address") ~
    get[String]("lottery.template_suffix") map {
    case id ~ name ~ nameshort ~ namelot ~ address ~ zip ~ city ~ tel ~ email ~ typeid ~ drawingsVisible ~ tmpl ~ server ~ port ~ user ~ pass ~ tls ~ from ~ tsuff =>
      Lottery(
        id,
        FieldEncrypt.sign(id.get),
        name,
        nameshort,
        namelot,
        address,
        zip,
        city,
        tel,
        email,
        LotteryType.byId(typeid),
        drawingsVisible,
        tmpl,
        tsuff,
        WinningNotificatonConfig(server, port, user, pass, 1 == tls, from))
  }

  private val withLotteryType = Lottery.instance ~ LotteryType.instance map {
    case lottery ~ lottery_type => lottery.copy(lotteryType = lottery_type)
  }
}