/*
 * Author: Roland Meo
 */

package actors

import actors.BaseChecker.CheckIt
import actors.Broker.{NotifyAll, UnregisterUrl}
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

import scala.concurrent.{ExecutionContext, Future}

abstract class  BaseChecker(val url: String, val broker: ActorRef)(implicit executionContext: ExecutionContext)  extends Actor with ActorLogging {

  protected val browser = JsoupBrowser()
  private val emptyCheckable = ""
  private var lastCheckable: String = emptyCheckable

  protected def createCheckable: Future[String]

  def receive: Receive = {
    case CheckIt => checkUrl(url)
    case _ => log.warning(s"${self.path.name} received invalid Command")
  }

  protected def getPageDocument = Future(browser.get(url))

  private def checkUrl(url: String): Unit = {
    val checkable = createCheckable

    checkable.onComplete(potentialCheckable => {
      val checkable = potentialCheckable.getOrElse(emptyCheckable)
      if (!isValid(checkable)) {
        broker ! UnregisterUrl(self.path.name)
        log.warning(s"${self.path.name}'s Urls is not Valid")
        self ! PoisonPill // kill urself!

      } else if (hasUpdate(checkable)) {
        if (lastCheckable != emptyCheckable) {
          broker ! NotifyAll(s"${self.path.name} had an Update")
        }
        log.info(s"${self.path.name} had an Update:\nOldHash: $lastCheckable\nNewHash: $checkable")
        lastCheckable = checkable

      } else {
        log.info(s"${self.path.name} found nothing new..")
      }
    })

    def isValid(hash: String): Boolean = hash != emptyCheckable

    def hasUpdate(hash: String): Boolean = hash != lastCheckable
  }
}

object BaseChecker{

  final case class CheckIt()

}
