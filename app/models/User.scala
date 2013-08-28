package models

import play.api.Play.current
import com.novus.salat.dao._
import com.mongodb.casbah.Imports._
import se.radley.plugin.salat._
import se.radley.plugin.salat.Binders._
import mongoContext._
import models.tp.{ TpUser, TpUserRepo }

trait Login {
  val username: String
  val password: String
}

case class UserCredentials(username: String, password: String) extends Login

case class User(
  id: ObjectId = new ObjectId,

  tpId: Int,

  username: String,
  password: String,

  githubLogin: String = null,
  githubToken: String = null,
  fullName: String = null) extends Login

object User extends UserDAO with TpUserRepo {
  def saveLogged(tpUser: TpUser, login: UserCredentials) = {

    val userFromDb = User.findOneById(tpUser.id)
    val newUser =
      userFromDb match {
        case None => {
          User(tpId = tpUser.id, username = login.username, password = login.password, fullName = tpUser.fullName)
        }
        case Some(user) => {
          user.copy(username = login.username, password = login.password, fullName = tpUser.fullName)
        }
      }
    User.save(newUser)

  }
}

trait UserDAO extends ModelCompanion[User, ObjectId] {
  def collection = mongoCollection("users")

  val dao = new SalatDAO[User, ObjectId](collection) {}

  // Indexes
  collection.ensureIndex(DBObject("tpId" -> 1), "user_id", unique = true)

  // Queries
  def findOneByUsername(username: String): Option[User] = dao.findOne(MongoDBObject("username" -> username))

  def findOneById(id: Int): Option[User] = dao.findOne(MongoDBObject("tpId" -> id))
}



