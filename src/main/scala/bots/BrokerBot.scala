/*
 * Author: Roland Meo
 */

package bots

import actors.Broker
import actors.Broker.{RegisterChat, StartDistributor}
import akka.actor.ActorRef
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands

import scala.concurrent.ExecutionContext

class BrokerBot(telegramBotToken: String, chatIds: Set[Long], urls: Set[(String, String, Boolean)])
               (implicit executionContext: ExecutionContext)
  extends TelegramBot with Polling with Commands {

  implicit val requestHandler: RequestHandler = request

  private val singleBroker = system.actorOf(Broker.props(chatIds, urls), "Broker")

  def broker: Option[ActorRef] = Some(singleBroker)


  onCommand("/register") { implicit msg =>
    broker.foreach(_ ! RegisterChat(msg.chat.id))
  }

  override def run(): Unit = {
    super.run()
    // start broker!
    broker.foreach(_ ! StartDistributor)
  }

  def token: String = this.telegramBotToken
}
