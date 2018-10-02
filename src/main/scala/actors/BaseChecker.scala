/*
 * Author: Roland Meo
 */

package actors

import actors.BaseChecker.CheckIt
import actors.Broker.{NotifyAll, UnregisterUrl}
import actors.DifferenceFinder.FindDifference
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.model.Document

import scala.concurrent.{ExecutionContext, Future}

abstract class BaseChecker(val url: String, val broker: ActorRef, val differenceFinder: ActorRef)(implicit executionContext: ExecutionContext) extends Actor with ActorLogging {

  protected val browser = JsoupBrowser()
  private val emptyCheckable = ""
  private var lastDom: Document = _
  private var lastCheckable: String = emptyCheckable

  protected def createCheckable(newDom: Option[Document]): Option[String]

  def receive: Receive = {
    case CheckIt => checkUrl(url)
    case _ => log.warning(s"${self.path.name} received invalid Command")
  }

  private def checkUrl(url: String): Unit = {
    Future(browser.get(url)).onComplete(newDomTry => {

      val newDom = newDomTry.toOption

      if (newDom.isEmpty) {
        broker ! UnregisterUrl(self.path.name)
        log.error(s"${self.path.name}'s couldn't read new DOM")
        self ! PoisonPill // kill urself!
        return
      }

      val checkable = createCheckable(newDom).getOrElse(emptyCheckable)
      if (!isValid(checkable)) {
        broker ! UnregisterUrl(self.path.name)
        log.warning(s"${self.path.name}'s DOM Parsing Result is not Valid")
        self ! PoisonPill // kill urself!

      } else if (hasUpdate(checkable)) {
        if (!isInitialStateBeforeAnyRealUpdate) {
          newDom.foreach(differenceFinder ! FindDifference(self.path.name, url, lastDom, _))
        }
        log.info(s"${self.path.name} had an Update:\nOldHash: $lastCheckable\nNewHash: $checkable")
        lastCheckable = checkable
        newDom.foreach(lastDom = _)

      } else {
        log.info(s"${self.path.name} found nothing new..")
      }
    })

    def isValid(hash: String): Boolean = hash != emptyCheckable

    def hasUpdate(hash: String): Boolean = hash != lastCheckable

    def isInitialStateBeforeAnyRealUpdate: Boolean = lastCheckable == emptyCheckable
  }
}

object BaseChecker {

  final case class CheckIt()

}
