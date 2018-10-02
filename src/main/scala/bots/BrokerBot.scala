/*
 * Author: Roland Meo
 */

package bots

import actors.Broker
import actors.Broker.{RegisterChat, StartDistributor}
import akka.actor.ActorRef
import akka.http.scaladsl.model.DateTime
import com.lightbend.emoji.Emoji
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.methods.ParseMode
import model.CheckerInitData

import scala.concurrent.ExecutionContext

class BrokerBot(telegramBotToken: String, chatIds: Set[Long], checkerInitDatas: Set[CheckerInitData])
               (implicit executionContext: ExecutionContext)
  extends TelegramBot with Polling with Commands {

  implicit val requestHandler: RequestHandler = request

  private val aliveSince = DateTime.now
  private val singleBroker = system.actorOf(Broker.props(chatIds, checkerInitDatas), "Broker")

  def broker: Option[ActorRef] = Some(singleBroker)


  onCommand("/register", "/Register", "/start", "/Start") { implicit msg =>
    broker.foreach(_ ! RegisterChat(msg.chat.id))
  }

  onCommand("/alive", "/Alive"){ implicit  msg =>
    reply(s"The Sneaker Alarm is running since *${aliveSince.toIsoLikeDateTimeString}* " +
      s"${Emoji.get(0x1F412).getOrElse("")}", Some(ParseMode.Markdown))
  }

  onCommand("/help","/Help"){ implicit msg =>
    reply(
      s"""
        | Hi this is the ${Emoji.get(0x1F45F).getOrElse("")} *SnkrBotAlarm* ${Emoji.get(0x1F45F).getOrElse("")}
        |
        |   to *register* just send /register
        |
        |   to *unregister* send /unregister
        |
        |   if you want to know if the bots is even running,
        |   send /alive and if so it will reply
        |
        |   and obviously for *help*..
        |   send /help and this message will pop up. ${Emoji.get(0x1F618).getOrElse("")}
      """.stripMargin, Some(ParseMode.Markdown))
  }

  override def run(): Unit = {
    super.run()
    // start broker!
    broker.foreach(_ ! StartDistributor)
  }

  def token: String = this.telegramBotToken
}
