package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index(message: String) = Action {
    implicit request =>
      Ok(views.html.index(message))
  }

}