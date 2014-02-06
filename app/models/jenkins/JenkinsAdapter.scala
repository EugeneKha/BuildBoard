package models.jenkins

import scala.util.Try
import scalaj.http.Http
import play.api.Play
import models._
import java.io.File
import scala.io.Source
import play.api.Play.current
import scala.xml.{Node, XML}
import models.BuildNode
import scala.Some
import models.TestCase
import models.Build
import models.TestCasePackage
import org.joda.time.DateTime

object JenkinsAdapter extends BuildsRepository with JenkinsApi {
  private val SCREENSHOT: String = "screenshot"
  private val directory = Play.configuration.getString("jenkins.data.path").get

  override def getBuilds: List[Build] = new File(directory)
    .listFiles
    .filter(_.isDirectory)
    .map(getBuild _)
    .toList

  private def getBuild(f: File): Build = {
    val prRegex = "pr_(\\d+)_(\\d+)".r
    val branchRegex = "(\\w+)_(\\d+)".r
    val (number, branch) = f.getName match {
      case prRegex(prId, number) => (number.toInt, s"pr/$prId")
      case branchRegex(branch, number) => (number.toInt, branch)
    }
    val node = getBuildNode(new File(f, "Build"))

    Build(number, branch, node.status, node.statusUrl, node.timestamp, node)
  }

  private def getBuildNode(f: File): BuildNode = {
    def getBuildNodeInner(folder: File, path: String): BuildNode = {
      val complexNameRegex = "(.+)_(.+)".r
      val contents = folder.listFiles.sortBy(_.getName).toList
      val (startedStatus, statusUrl, timestamp) = contents.filter(file => file.getName.endsWith("started")) match {
        case file :: Nil =>
          val (statusUrl, ts) = read(file)
            .map(fc => {
            val rows = fc.split('\n')
            val statusUrl = rows(0)
            val ts = if (rows.length > 1) Some(new java.text.SimpleDateFormat("MM/dd/yyyy hh:mm:ss").parse(rows(1)).getTime) else None
            (Some(statusUrl), ts)
          })
            .getOrElse((None, Some(file.lastModified)))
          (None, statusUrl, ts.getOrElse(file.lastModified))
        case Nil => (Some("FAILURE"), None, folder.lastModified)
      }
      val status = if (startedStatus.isDefined) startedStatus
      else contents.filter(f => f.getName.endsWith("finished")) match {
        case file :: Nil => read(file)
        case Nil => startedStatus
      }
      val children = contents
        .filter(f => f.isDirectory && !f.getName.startsWith("."))
        .map(f => getBuildNodeInner(f, folder.getPath)).toList

      val artifacts = getArtifacts(contents)

      //todo: associate screenshots with tests

      folder.getName match {
        case complexNameRegex(runName, name) => BuildNode(name, runName, status, statusUrl.getOrElse(""), artifacts, new DateTime(timestamp), children)
        case name => BuildNode(name, name, status, statusUrl.getOrElse(""), artifacts, new DateTime(timestamp), children)
      }
    }

    //todo: add artifacts to root node
    getBuildNodeInner(new File(f, rootJobName), f.getPath)
  }

  private def getArtifacts(contents: List[File]): List[Artifact] = {
    def getArtifactsInner(file: File, filter: File => Boolean, artifactName: String): List[Artifact] = file.listFiles
      .filter(filter(_))
      .map(_.getPath.substring(directory.length + 1))
      .map(Artifact(artifactName, _))
      .toList

    contents.map(file => file.getName match {
      case name if name == ".TestResults" => getArtifactsInner(file, f => f.getName.endsWith(".xml"), "testResults")
      case name if name == ".Logs" => getArtifactsInner(file, f => f.getName.startsWith("SessionLogs"), "logs")
      case name if name == ".Screenshots" => getArtifactsInner(file, _ => true, SCREENSHOT)
      case _ => List()
    })
      .flatten
  }

  private def read(f: File): Option[String] = Try {
    Some(Source.fromFile(f).mkString)
  }.getOrElse(None)

  def getTestCasePackages(testRunBuildNode: BuildNode): List[TestCasePackage] = {
    val screenshots = testRunBuildNode.artifacts.filter(a => a.name == SCREENSHOT)

    def getTestCasePackage(node: Node): TestCasePackage = {
      def getTestCasePackageInner(node: Node, namespace: String = ""): TestCasePackage = {
        val name = node.attribute("name").get.head.text
        val currentNamespace = getAttribute(node, "type") match {
          case Some("Namespace") => {
            if (namespace.isEmpty) name else s"$namespace.$name"
          }
          case _ => namespace
        }
        val children = (node \ "results" \ "test-suite")
          .filter(n => getAttribute(n, "result").get != "Inconclusive")
          .map(n => getTestCasePackageInner(n, currentNamespace))
          .toList
        val testCases = (node \ "results" \ "test-case").map(tcNode => {
          val executed = getAttribute(tcNode, "executed").get.toBoolean
          val result = if (!executed) "Ignored" else if (getAttribute(tcNode, "success").get != "True") "Failure" else "Success"
          val (message, stackTrace) = if (result == "Failure") ((tcNode \\ "message").headOption.map(_.text), (tcNode \\ "stack-trace").headOption.map(_.text)) else (None, None)
          val tcName: String = getAttribute(tcNode, "name").get
          val testNameRegex = ".*\\.(\\w+).(\\w+)$".r
          val tcScreenshots = (tcName match {
            case testNameRegex(className, methodName) => {
              val screenshotFileNameRegex = s"$className\\.$methodName-[\\d|-]*".r
              screenshots.filter(s => s.name match {
                case screenshotFileNameRegex() => true
                case _ => false
              })
            }
            case _ => Nil
          }).map(s => s.url)

          TestCase(tcName, result, getAttribute(tcNode, "time").getOrElse("0").toDouble, message, tcScreenshots, stackTrace)
        }).toList

        TestCasePackage(if (currentNamespace.isEmpty) name else s"$namespace.$name", children, testCases)
      }

      getTestCasePackageInner(node)
    }

    testRunBuildNode.artifacts.find(a => a.name == "testResults") match {
      case Some(file) => read(this.getArtifact(file.url)) match {
        case None => List()
        case Some(xmlString) =>
          val xml = XML.loadString(xmlString)
          (xml \ "test-suite").map(getTestCasePackage _).toList
      }
      case None => Nil
    }
  }

  def getArtifact(file: String): File = new File(directory, file)

  private def getAttribute(n: Node, key: String): Option[String] = n.attribute(key).map(_.headOption.map(_.text)).flatten
}

trait JenkinsApi {
  private val jenkinsUrl = Play.configuration.getString("jenkins.url").get
  val rootJobName = "StartBuild"

  def forceBuild(action: models.BuildAction) = Try {
    Http.post(s"$jenkinsUrl/job/$rootJobName/buildWithParameters")
      .params(action.parameters)
      .asString
  }
}
