package actors.checkers

import actors.BaseChecker
import akka.actor.{ActorRef, Props}
import utils.Md5Hasher

import scala.concurrent.{ExecutionContext, Future}

class DomDiffChecker(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext)
  extends BaseChecker(url,broker) {

  override def createCheckable: Future[String] =
    getPageDocument
    .map(_.body.toString)
    // remove all line breaks
    .map(_.replaceAll("^ | $|\\n", ""))
    // remove all script tags and their content
    .map(_.replaceAll("""<script[^>]*>(.*?)<\/script>""", ""))
    // replace other tags except links
    .map(_.replaceAll("""<(?!\/?a(?=>|\s.*>))\/?.*?>""", ""))
    .map(Md5Hasher.hash)

}

object DomDiffChecker {
  def props(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext): Props = Props(new DomDiffChecker(url, broker))
}