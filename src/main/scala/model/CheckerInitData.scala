package model

import actors.checkers.{DomDiffChecker, HttpStringChecker}
import akka.actor.{ActorRef, Props}

import scala.concurrent.ExecutionContext

case class CheckerInitData(
                            name: String,
                            url: String,
                            checkerType: String,
                            isValid: Boolean = true
                          )

object CheckerInitData {

  def props(checkerInitData: CheckerInitData)(broker: ActorRef, differenceFinder: ActorRef)
           (implicit executionContext: ExecutionContext): Option[Props] = {
    val httpStringChecker = classOf[HttpStringChecker].getSimpleName
    val domDiffChecker   = classOf[DomDiffChecker].getSimpleName

    checkerInitData.checkerType match {
      case `httpStringChecker` => Some(HttpStringChecker.props(checkerInitData.url, broker, differenceFinder))
      case `domDiffChecker` => Some(DomDiffChecker.props(checkerInitData.url, broker, differenceFinder))
      case _ => None
    }
  }

}
