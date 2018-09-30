package actors.checkers

import actors.BaseChecker
import akka.actor.{ActorRef, Props}
import utils.Md5Hasher

import scala.concurrent.{ExecutionContext, Future}

class HttpStringChecker (url: String, broker: ActorRef)(implicit executionContext: ExecutionContext)
  extends BaseChecker(url,broker) {

  override def createCheckable: Future[String] = {
    getPageDocument
        .map(_.body)
        .map(_.toString)
        .map(Md5Hasher.hash)
  }

}

object HttpStringChecker {
  def props(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext): Props = Props(new HttpStringChecker(url, broker))
}
