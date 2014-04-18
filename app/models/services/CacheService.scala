package models.services

import scala.concurrent.duration._
import scala.util.Try
import rx.lang.scala.{Subscription, Observable}

import scala.util.Success
import scala.util.Failure
import play.api.{Logger, Play}
import play.api.Play.current
import models.{Build, AuthInfo}
import src.Utils.watch
import components.DefaultRegistry


object CacheService {
  val authInfo: AuthInfo = (for {
    tpToken <- Play.configuration.getString("cache.user.tp.token")
    gToken <- Play.configuration.getString("cache.user.github.token")
  }

  yield new AuthInfo {
      override val githubToken: String = gToken
      override val token: String = tpToken
    }).get


  val repository = new DefaultRegistry(authInfo)

  val githubInterval = Play.configuration.getInt("github.cache.interval").getOrElse(600).seconds
  val jenkinsInterval = Play.configuration.getInt("jenkins.cache.interval").getOrElse(60).seconds


  def start = {
    val jenkinsRepository = repository.jenkinsRepository
    val githubSubscription = Observable.timer(0 seconds, githubInterval)
      .map(_ => Try {
      repository.branchService.getBranches
    })
      .subscribe({
      case Success(data) =>
        val branches = repository.branchRepository.getBranches
        watch("removing obsolete branches") {
          Try {
            branches
              .filter(b => !data.exists(_.name == b.name))
              .foreach(branch => {
              repository.branchRepository.remove(branch)
              repository.buildRepository.removeAll(branch)
            })
          }
        }
        watch("updating branches") {
          Try {
            data.foreach(branch => repository.branchRepository.update(branch))
          }
        }
      case Failure(e) => play.Logger.error("Error", e)
    },
    error => {
      play.Logger.error("Error in githubSubscription", error)
    })

    val jenkinsSubscription = Observable.timer(0 seconds, jenkinsInterval)
      .subscribe(_ => Try {
      val branches = repository.branchRepository.getBranches

      watch("updating builds") {
        branches.foreach(branch => {
          Logger.info(s"updating builds for ${branch.name}")
          val existingBuilds = repository.buildRepository.getBuilds(branch)
          val builds = jenkinsRepository.getBuilds(branch)

          builds.foreach(build => {
            val existingBuild = existingBuilds.find(_.number == build.number)
            val toggled = existingBuild.map(_.toggled).getOrElse(build.toggled)

            val updatedBuild: Build = build.copy(toggled = toggled)

            repository.buildRepository.update(branch, updatedBuild)


          })
        })
      }
    },
        error => {
          play.Logger.error("Error in jenkinsSubscription", error)
        })

    Subscription {
      githubSubscription.unsubscribe()
      jenkinsSubscription.unsubscribe()
    }
  }
}
