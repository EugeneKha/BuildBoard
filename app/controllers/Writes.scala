package controllers

import play.api.libs.json._
import models._
import models.Assignment
import models.Build
import models.PullRequestStatus
import models.Assignment
import models.BuildNode
import models.Branch
import models.Build
import play.api.libs.functional.syntax._
import models.PullRequestStatus
import models.Assignment
import models.BuildNode
import models.Branch
import models.Build

object Writes {
  implicit var buildNodeWrite: Writes[BuildNode] = null
  buildNodeWrite = Json.writes[BuildNode]
  implicit val buildWrite = Json.writes[Build]
  implicit val entityAssignment = Json.writes[Assignment]
  implicit val entityStateWrite = Json.writes[EntityState]
  implicit val entityWrite = Json.writes[Entity]
  implicit val prWrite = Json.writes[PullRequest]
  implicit val branchWrite = Json.writes[Branch]
  implicit val statusWrites = (
    (__ \ "isMergeable").write[Boolean] ~
      (__ \ "isMerged").write[Boolean])(unlift(PullRequestStatus.unapply))
}
