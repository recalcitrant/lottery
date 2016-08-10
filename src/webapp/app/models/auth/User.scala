package models.auth

import _root_.utils.FieldEncrypt
import org.mindrot.jbcrypt.BCrypt
import play.api.db._
import play.api.Play.current
import anorm.SqlParser._
import anorm._
import java.util.Date
import controllers.auth.{InsertUser, UpdateUser}
import models.lottery.Lottery
import utils.mail.Mailer

case class User(id: Pk[Long],
                hid: String,
                username: String,
                email: String,
                password: String,
                lottery: Lottery,
                active: Boolean = true,
                created: Option[Date] = Some(new Date),
                permissions: Set[Permission] = Set[Permission]()) extends PermissionBundle {

  def hasPermission(id: Long) = permissions exists (_.id.get == id)

  def hasEitherPermission(ps: Seq[Long]) = ps.exists(p => permissions exists (_.id.get == p))

  def addPermission(p: Permission) = copy(permissions = permissions + p)

  def delPermission(p: Permission) = copy(permissions = permissions filterNot (_.id.get == p.id.get))

  def hasPermissionOrIsSuperAdmin(id: Long) = hasPermission(id) || hasPermission(PERMISSION_SUPER_ADMIN)
}

object User extends PermissionBundle {

  val MAX_LOGIN_ATTEMPTS = 3

  val sql = """ select * from user_account ua
								join user_account_permission up on ua.id = up.user_account_id
								join permission p on up.permissions_id = p.id """

  val sqlByPerm = sql + " where lottery_id = {lid} and permissions_id = {pid} "

  val sqlSuperAdmin = sql + " and permissions_id = " + PERMISSION_SUPER_ADMIN

  val order = " order by ua.id desc"

  private val instance = get[Pk[Long]]("user_account.id") ~ get[Long]("user_account.lottery_id") ~ get[String]("user_account.username") ~ get[String]("user_account.email") ~ get[String]("user_account.password") ~ get[Long]("user_account.active") ~ get[Date]("user_account.created") map {
    case id ~ lid ~ username ~ email ~ pw ~ active ~ created => User(id, FieldEncrypt.sign(id.get), username, email, pw, Lottery.byId(lid).get, 1 == active, Some(created))
  }

  private val withLotteryAndPermissions = (User.instance ~ Permission.instance *).map {
    _.groupBy(_._1).toSeq.headOption.map {
      case (u, ps) => u.copy(permissions = ps.map(_._2).toSet)
    }
  }

  private val listWithLotteryAndPermissions = (User.instance ~ Permission.instance *).map {
    _.groupBy(_._1).toSeq.map {
      case (u, ps) => u.copy(permissions = ps.map(_._2).toSet)
    }
  }

  def listByLotteryAndPermission(lid: Long, perm: Long) = DB.withConnection(implicit con => {
    SQL(sqlByPerm).onParams(lid, perm).as(User.listWithLotteryAndPermissions)
  })

  def listSuperAdmins = DB.withConnection(implicit con => {
    SQL(sqlSuperAdmin).as(User.listWithLotteryAndPermissions)
  })

  def authenticate(username: String, password: String) =
    byActiveUsername(username) filter (user => {
      try {
        if (!BCrypt.checkpw(password, user.password)) incrementMaxLogins(username) else true
        //  Invalid salt version:
      } catch {
        case ex:Throwable => false
      }
    }) orElse None

  def incrementMaxLogins(username: String) = {
    DB.withConnection {
      implicit con =>
        getLoginAttempts(username).filter(_ < MAX_LOGIN_ATTEMPTS).map {
          less => SQL("update user_account set loginattempts = loginattempts + 1 where username = {username}").onParams(username).executeUpdate()
        } orElse {
          SQL("select lottery_id from user_account where username = {username}").onParams(username).as(scalar[Long].singleOpt).map {
            lid => {
              SQL("update user_account set active = 0 where username = {username}").onParams(username).executeUpdate()
              // add superadmins to recipients in case it's the admin himself who got locked out:
              (listSuperAdmins ++ listByLotteryAndPermission(lid, + PERMISSION_ADMIN)).map(u => sendNotification(username, u.email))
            }
          }
        }
    }
    false
  }

  def resetMaxLogins(username: String) = {
    DB.withConnection {
      implicit con => SQL("update user_account set loginattempts = 0 where username = {username}").onParams(username).executeUpdate()
    }
  }

  def sendNotification(username: String, email: String) = Mailer.userLockedOut(username, email)

  def getLoginAttempts(username: String) = DB.withConnection {
    implicit con => SQL("select loginattempts from user_account where username = {username}").onParams(username).as(scalar[Int].singleOpt)
  }

  def byId(id: Long): Option[User] = DB.withConnection(implicit con =>
    SQL(sql + "and ua.id = {id}" + order).on("id" -> id).as(User.withLotteryAndPermissions))

  def byUsername(username: String): Option[User] = DB.withConnection(implicit con =>
    SQL(sql + " and username = {username}").onParams(username).as(User.withLotteryAndPermissions))

  def byEmail(email: String): Option[User] = DB.withConnection(implicit con =>
    SQL(sql + " and email = {email}").onParams(email).as(User.withLotteryAndPermissions))

  def byActiveUsername(username: String) = DB.withConnection(implicit con =>
    SQL(sql + "and username = {username} and active = 1" + order).onParams(username).as(User.withLotteryAndPermissions))

  def listByLottery(lid: Long) = DB.withConnection(implicit con =>
    SQL(sql + " where lottery_id = {lid}" + order).onParams(lid).as(User.listWithLotteryAndPermissions))

  def delete(id: Long) = DB.withConnection {implicit con => SQL("delete from user_account where id = {id}").onParams(id).executeUpdate()}

  def add(user: InsertUser, lid: Long) = {
    DB.withTransaction {
      implicit con =>
        SQL("insert into user_account(lottery_id, username, email, password, created, active) values ( {lottery_id}, {username}, {email}, {pw}, {created}, {active})")
          .onParams(lid, user.username, user.email, BCrypt.hashpw(user.password, BCrypt.gensalt()), new Date(), 1).executeUpdate()
        val id = SQL("SELECT last_insert_id()").as(scalar[java.math.BigInteger].single)
        SQL("insert into user_account_permission ( user_account_id, permissions_id ) values ( {id},{perm} )").onParams(id.longValue(), user.permission).executeUpdate()
    }
    true
  }

  def update(user: UpdateUser, mayUpdate: Boolean = false) {
    DB.withTransaction(implicit con => {
      if ("" != user.password) SQL("update user_account set username = {username},  email = {email}, password = {pw} where id = {id}").onParams(user.username, user.email, BCrypt.hashpw(user.password, BCrypt.gensalt()), user.id).executeUpdate()
      else SQL("update user_account set username = {username}, email = {email} where id = {id}").onParams(user.username, user.email, user.id).executeUpdate()
      if (mayUpdate) {
        SQL("delete from user_account_permission where user_account_id = {id}").onParams(user.id).executeUpdate()
        SQL("insert into user_account_permission ( user_account_id, permissions_id ) values ( {id},{perm} )").onParams(user.id, user.permission.get).executeUpdate()
        SQL("update user_account set loginattempts = 0, active = 1 where username = {username}").onParams(user.username).executeUpdate()
      }
    })
  }

  def hasPermission(name: String, perm: Seq[Long]) = byUsername(name).exists(u => perm.exists(u.hasPermissionOrIsSuperAdmin))
}

case class Permission(id: Pk[Long], name: String)

object Permission {

  def byId(id: Long) =
    DB.withConnection(implicit con => SQL("select * from permission where id = {id}").onParams(id).as(Permission.instance.single))

  protected[auth] val instance = {
    get[Pk[Long]]("permission.id") ~ get[String]("permission.name") map {
      case id ~ name => Permission(id, name)
    }
  }
}