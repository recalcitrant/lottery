package models.lottery.prize

import _root_.utils.{Config, FileUtils}
import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._
import java.sql.Connection
import play.api.mvc.MultipartFormData
import play.api.libs.Files
import java.io.File

case class PrizeUpload(id: Pk[Long], url: String)

object PrizeUpload {

  private val uploadPath = Config.getString("upload.folder")

  private val sql = " select * from dbase_prizeupload pu "

  def fileById(lid: Long, id: Long): Option[(String, File)] = DB.withConnection(implicit con => {
    val urlopt = SQL("SELECT url from dbase_prizeupload where id = {id}").onParams(id).as(scalar[String].singleOpt)
    urlopt.map {
      url =>
        Option((url, new File(uploadPath + lid.toString + "/prize/" + id + "_" + url)))
    }.getOrElse(None)
  })

  def deleteByPrize(implicit connection: Connection, lid: Long, pid: Long) = {
    byPrizeId(pid).foreach {upload => new File(uploadPath + lid.toString + "/prize/" + upload.id.get + "_" + upload.url).delete()}
    SQL("delete from dbase_prizeupload where prize_id = {id}").onParams(pid).executeUpdate()
  }

  def deleteById(lid: Long, id: Long) = DB.withConnection(implicit con => {
    val urlopt = SQL("SELECT url from dbase_prizeupload where id = {id}").onParams(id).as(scalar[String].singleOpt)
    urlopt.map {
      url =>
        new File(uploadPath + lid.toString + "/prize/" + id + "_" + url).delete()
        SQL("delete from dbase_prizeupload where id = {id}").onParams(id).executeUpdate()
    }
  })

  def add(lid: Long, pid: Long, fp: MultipartFormData.FilePart[Files.TemporaryFile]) = DB.withConnection(implicit con => {
    val rows = SQL("insert into dbase_prizeupload(prize_id, url) values ( {pid}, {url} )").onParams(pid, fp.filename).executeUpdate()
    if (0 == rows) None
    else {
      val newid = SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].singleOpt)
      val back: Option[Boolean] = newid.map(uploadid => {
        val filename = uploadid.longValue().toString + "_" + fp.filename
        val lotteryDir = uploadPath + lid.toString
        val lotteryPrizeDir = uploadPath + lid.toString + "/prize/"
        val lotteryFile = new java.io.File(lotteryDir)
        if (!lotteryFile.exists()) new File(lotteryPrizeDir).mkdirs()
        val filepath = lotteryPrizeDir + filename
        FileUtils.copyfile(fp.ref.file, filepath)
      })
      back
    }
  })

  def byPrizeId(pid: Long) = DB.withConnection(implicit con =>
    SQL(sql + " where prize_id = {pid} order by id").onParams(pid).as(PrizeUpload.instance *))

  def count(pid: Long) = DB.withConnection(implicit con =>
    SQL("select count(*) from dbase_prizeupload pu where prize_id = {pid}").onParams(pid).as(scalar[Long].singleOpt))

  private def instance = get[Pk[Long]]("dbase_prizeupload.id") ~ get[String]("dbase_prizeupload.url") map {
    case id ~ url => PrizeUpload(id, url)
  }
}