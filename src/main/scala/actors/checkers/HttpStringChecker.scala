package actors.checkers

import java.net.URL

import actors.BaseChecker
import akka.actor.{ActorRef, PoisonPill, Props}
import app.TestStufff.baseDomain
import utils.Md5Hasher

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.matching.Regex

class HttpStringChecker(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext)
  extends BaseChecker(url, broker) {

  private val baseDomain = Try {
    new URL(url)
  }.toOption.map(_.getHost)
  private val regex: Option[Regex] = baseDomain.map(h =>
    (""""(https?:\/\/w{0,3}\.?""" + h.replaceFirst("www", "") +"""[\/\S]{1,})\"""").r
  )

  override def createCheckable: Future[String] = {
    getPageDocument
      .map(_.body.toString)
      .filter(_ => regex.nonEmpty)
      .map(regex.get.findAllIn(_).toSet.toList.sorted.reduce(_ + _))
      .map(Md5Hasher.hash)
  }

  override def receive: Receive = {
    if (baseDomain.isEmpty) self ! PoisonPill
    super.receive
  }
}

object HttpStringChecker {
  def props(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext): Props = Props(new HttpStringChecker(url, broker))
}
