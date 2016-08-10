package models.lottery.drawing

import java.io.File
import play.api.Play.current

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.libs.Files
import play.api.mvc.MultipartFormData
import utils.{FileUtils, Config}

case class WinningNotification(id: Pk[Long],
                               url: Option[String],
                               desc: Option[String]) {
}

object WinningNotification {

  private val uploadPath = Config.getString("upload.folder")

  private val sql = " SELECT * FROM winning_notification_content where drawing_id = {did}"

  def byDrawing(did: Long) = DB.withConnection(implicit con =>
    SQL(sql).on("did" -> did).as(WinningNotification.instance.singleOpt))

  def getDescription(pid: Long) = DB.withConnection(implicit con => SQL("SELECT description from winning_notification_content p where p.id = {id}")
    .onParams(pid).as(scalar[Option[String]].single))

  def updateDescription(did: Long, desc: String) = {
    DB.withConnection {
      implicit con =>
        val rows = isNew(did) match {
          case true =>
            SQL("insert into winning_notification_content(drawing_id, description) values ( {did}, {description} )").onParams(did, desc).executeUpdate()
          case false =>
            SQL("update winning_notification_content set description = {description} where drawing_id = {did}").onParams(desc, did).executeUpdate()
        }
        if (0 == rows) None
        else Some(rows)
    }
  }

  def fileByDrawingId(lid: Long, did: Long): Option[(String, File)] = DB.withConnection(implicit con => {
    val urlopt = SQL("SELECT url from winning_notification_content where drawing_id = {did}").onParams(did).as(scalar[Option[String]].singleOpt)
    urlopt.map {
      url =>
        Option((url.getOrElse(""), new File(uploadPath + lid.toString + "/" + did + "_" + url.getOrElse(""))))
    }.getOrElse(None)
  })

  def deleteByDrawingId(lid: Long, did: Long) = DB.withConnection(implicit con => {
    val urlopt = SQL("SELECT url from winning_notification_content where drawing_id = {did}").onParams(did).as(scalar[Option[String]].singleOpt)
    urlopt.map {
      url =>
        new File(uploadPath + lid.toString + "/" + did + "_" + url).delete()
        SQL("delete from winning_notification_content where drawing_id = {did}").onParams(did).executeUpdate()
    }
  })


  def deleteUpload(lid: Long, did: Long) = DB.withConnection(implicit con => {
    val urlopt = SQL("SELECT url from winning_notification_content where drawing_id = {id}").onParams(did).as(scalar[Option[String]].singleOpt)
    urlopt.map {
      url =>
        new File(uploadPath + lid.toString + "/" + did + "_" + url).delete()
        SQL("update winning_notification_content set url = NULL where drawing_id = {id}").onParams(did).executeUpdate()
    }
  })

  def upload(lid: Long, did: Long, fp: MultipartFormData.FilePart[Files.TemporaryFile]) = DB.withConnection(implicit con => {
    val rows = isNew(did) match {
      case true =>
        SQL("insert into winning_notification_content(drawing_id, url) values ( {did}, {url} )").onParams(did, fp.filename).executeUpdate()
      case false =>
        SQL("update winning_notification_content set url = {url} where drawing_id = {did}").onParams(fp.filename, did).executeUpdate()
    }
    if (0 == rows) None
    else {
      if (1024 * 1024 * 4 >= fp.ref.file.length()) {
        val filename = did.toString + "_" + fp.filename
        val lotteryDir = uploadPath + lid.toString
        val lotteryNotificationDir = uploadPath + lid.toString + "/"
        val lotteryFile = new java.io.File(lotteryDir)
        if (!lotteryFile.exists()) new File(lotteryNotificationDir).mkdirs()
        else lotteryFile.listFiles().map(_.delete())
        val filepath = lotteryNotificationDir + filename
        FileUtils.copyfile(fp.ref.file, filepath)
        Some(true)
      } else Some(false)
    }
  })

  def isNew(did: Long) = DB.withConnection(implicit con =>
    0 == SQL("select count(*) from winning_notification_content where drawing_id = {did}").onParams(did).as(scalar[Long].single)
  )


  private def instance = get[Pk[Long]]("winning_notification_content.id") ~ get[Option[String]]("winning_notification_content.url") ~ get[Option[String]]("winning_notification_content.description") map {
    case id ~ url ~ desc => WinningNotification(id, url, desc)
  }
}
