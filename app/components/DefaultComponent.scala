package components

import models.github.GithubRepositoryComponentImpl
import models.services.BranchServiceComponentImpl
import models.tp.{UserRepositoryComponentImpl, TargetprocessComponentImpl}
import models.jenkins.JenkinsRepositoryComponentImpl
import models.{BuildRepositoryComponentImpl, BranchRepositoryComponentImpl}

trait DefaultComponent
  extends AuthInfoProviderComponent
  with GithubRepositoryComponentImpl
  with BranchServiceComponentImpl
  with TargetprocessComponentImpl
  with JenkinsRepositoryComponentImpl
  with BranchRepositoryComponentImpl
  with BuildRepositoryComponentImpl


object Registry extends UserRepositoryComponentImpl