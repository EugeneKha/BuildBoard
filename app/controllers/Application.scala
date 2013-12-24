package controllers

import play.api.mvc._
import models._
import play.api.libs.json._
import Writes._
import models.jenkins.CachedJenkinsRepository
import models.Branch

object Application extends Controller with Secured {

  def index = {
    IsAuthorized {
      implicit user =>
        implicit request => Ok(views.html.index(user))
    }
  }

  def branches = IsAuthorized {
    implicit user =>
      implicit request =>
        val branches = new BranchesRepository().getBranches

        Ok(Json.toJson(branches))
  }

  def branch(id:String) = IsAuthorized {
      implicit user =>
        implicit request =>
          val branch: Branch = new BranchesRepository().getBranch(id)

          Ok(Json.toJson(branch))
  }

  val jenkinsRepo = CachedJenkinsRepository

  def builds(branch: String) = IsAuthorized {
    implicit user =>
      val branchEntity = new BranchesRepository().getBranch(branch)
      request => Ok(Json.toJson(jenkinsRepo.getBuilds(branchEntity)))
  }

  def lastBuildInfo(branch: String) = IsAuthorized {
    implicit user =>
      val branchEntity = new BranchesRepository().getBranch(branch)
      request => Ok(Json.toJson(jenkinsRepo.getLastBuild(branchEntity)))
  }

  def lastBuildInfos = IsAuthorized {
    implicit user =>
      val branches = new BranchesRepository().getBranches
      request => Ok(Json.toJson(jenkinsRepo.getLastBuildsByBranch(branches)))
  }

  def build(branch: String, number: Int) = IsAuthorized {
    implicit user =>
      val branchEntity = new BranchesRepository().getBranch(branch)
      request => Ok(Json.toJson(jenkinsRepo.getBuild(branchEntity, number)))
  }
}
