/*
 * Author: Roland Meo
 */

package actors.checkers

import actors.Broker.{NotifyAll, UnregisterUrl}
import actors.checkers.SimpleUrlDiffChecker.CheckIt
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import utils.Md5Hasher

import scala.concurrent.{ExecutionContext, Future}

class SimpleUrlDiffChecker(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext) extends Actor with ActorLogging {

  val browser = JsoupBrowser()
  val emptyHash = ""
  var lastContentHash: String = emptyHash

  def receive: Receive = {
    case CheckIt => checkUrl(url)
    case _ => log.warning(s"${self.path.name} received invalid Command")
  }

  def checkUrl(url: String): Unit = {
    val eventualHash = Future(browser.get(url))
      .map(_.body.toString)
      // remove all line breaks
      // .map(_.replaceAll("^ | $|\\n", ""))
      // remove all script tags and their content
      .map(_.replaceAll("""<script[^>]*>(.*?)<\/script>""", ""))
      // replace other tags except links
      .map(_.replaceAll("""<(?!\/?a(?=>|\s.*>))\/?.*?>""", ""))
      .map(Md5Hasher.hash)

    eventualHash.onComplete(potentialHash => {
      val hash = potentialHash.getOrElse(emptyHash)
      if (!isValid(hash)) {
        broker ! UnregisterUrl(self.path.name)
        log.warning(s"${self.path.name}'s Urls is not Valid")
        self ! PoisonPill // kill urself!

      } else if (hasUpdate(hash)) {
        if (lastContentHash != emptyHash) {
          broker ! NotifyAll(s"${self.path.name} had an Update")
        }
        log.info(s"${self.path.name} had an Update:\nOldHash: $lastContentHash\nNewHash: $hash")
        lastContentHash = hash

      } else {
        log.info(s"${self.path.name} found nothing new..")
      }
    })
  }

  def isValid(hash: String): Boolean = hash != emptyHash

  def hasUpdate(hash: String): Boolean = hash != lastContentHash
}

object SimpleUrlDiffChecker {
  def props(url: String, broker: ActorRef)(implicit executionContext: ExecutionContext): Props = Props(new SimpleUrlDiffChecker(url, broker))

  final case class CheckIt()

}