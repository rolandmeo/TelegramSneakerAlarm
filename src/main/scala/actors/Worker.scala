/*
 * Author: Roland Meo
 */

package actors

import actors.Worker.WorkerSendText
import akka.actor.{Actor, ActorLogging, Props}
import info.mukel.telegrambot4s.api.RequestHandler
import info.mukel.telegrambot4s.methods.{ParseMode, SendMessage}

class Worker(chatId: Long)(implicit req: RequestHandler) extends Actor with ActorLogging {
  def receive: Receive = {
    case WorkerSendText(msg) =>
      req(SendMessage(chatId, msg, Some(ParseMode.Markdown)))
      log.info(s"${self.path.name} sent (to Chat:$chatId) WorkMessage: $msg")
    case _ =>
      log.warning(s"${self.path.name} received invalid Command")
  }
}

object Worker {
  def props(chatId: Long)(implicit req: RequestHandler): Props = Props(new Worker(chatId))

  final case class WorkerSendText(msg: String)

}