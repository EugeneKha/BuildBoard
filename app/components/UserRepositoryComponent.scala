package components

import models.{TpUser, Entity, EntityState}
import scala.util.Try

trait UserRepositoryComponent {
  def userRepository: UserRepository
  trait UserRepository {
    def authenticate(username: String, password: String): Try[(TpUser, String)]
  }
 }
