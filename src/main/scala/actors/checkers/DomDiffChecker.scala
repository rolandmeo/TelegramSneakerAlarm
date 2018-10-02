package actors.checkers

import actors.BaseChecker
import akka.actor.{ActorRef, Props}
import net.ruippeixotog.scalascraper.model.Document
import utils.Md5Hasher

import scala.concurrent.{ExecutionContext, Future}

class DomDiffChecker(url: String, broker: ActorRef, differenceFinder: ActorRef)
                    (implicit executionContext: ExecutionContext)
  extends BaseChecker(url, broker, differenceFinder) {

  override def createCheckable(newDom: Option[Document]): Option[String] =
    newDom
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
  def props(url: String, broker: ActorRef, differenceFinder: ActorRef)
           (implicit executionContext: ExecutionContext): Props =
    Props(new DomDiffChecker(url, broker, differenceFinder))
}