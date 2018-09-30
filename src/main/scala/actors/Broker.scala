/*
 * Author: Roland Meo
 */

package actors

import java.nio.file.Paths
import java.nio.file.StandardOpenOption.{APPEND, CREATE, WRITE}

import actors.BaseChecker.CheckIt
import actors.Broker.{NotifyAll, RegisterChat, StartDistributor, UnregisterUrl}
import actors.Worker.WorkerSendText
import actors.checkers.DomDiffChecker
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Keep, Source}
import akka.util.ByteString
import credentials.FilePaths
import info.mukel.telegrambot4s.api.RequestHandler
import model.CheckerInitData

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class Broker(chatIds: Set[Long], checkerInitDatas: Set[CheckerInitData])
            (implicit val system: ActorSystem,
             request: RequestHandler,
             materializer: Materializer,
             executor: ExecutionContext)
  extends Actor with ActorLogging {

  private val refreshMinutes = 1
  private val maxRefreshDelay = 20
  private val random = Random

  // nasty mutables! °;,,;°
  private val chatIdz: mutable.Set[Long] = mutable.Set(chatIds.toSeq: _*)
  private val workerActors: mutable.Map[Long, ActorRef] = mutable.Map.empty
  private val checkerActors: mutable.Map[String, ActorRef] = mutable.Map.empty

  def receive: Receive = {
    case StartDistributor => setupWorkersAndUrlCheckersAndNotifyChats()

    case RegisterChat(chatId) => registerNewChat(chatId)

    case NotifyAll(note) => notifyChats(note)

    case UnregisterUrl(urlCheckerName) => unregisterUrlChecker(urlCheckerName)

    case _ => log.warning(s"${self.path.name} received invalid Command")
  }

  private def unregisterUrlChecker(urlCheckerName: String): Unit = {
    checkerActors.remove(urlCheckerName)
    log.warning(s"${self.path.name} received invalid Command")
  }

  private def notifyChats(note: String): Unit = workerActors.values.foreach(_ ! WorkerSendText(note))

  private def setupWorkersAndUrlCheckersAndNotifyChats(): Unit = {
    // Start Workers and notify clients
    chatIdz.foreach(id => workerActors.put(id, system.actorOf(Worker.props(id), s"Worker$id")))
    workerActors.values.foreach(_ ! WorkerSendText("The System is up and running!"))
    log.info(s"Started ${self.path.name} with ${workerActors.size} Workers")

    // Start Checkers
    checkerInitDatas.filter(_.isValid)
      .foreach(checkerInitData => {
        val maybeProps = CheckerInitData.props(checkerInitData)(self)
        maybeProps.foreach(props =>
          checkerActors.put(checkerInitData.name, system.actorOf(props, s"${checkerInitData.name}"))
        )
      })

    log.info(s"Started ${self.path.name} with ${checkerActors.size} Checkers")

    system.scheduler
      .schedule(random.nextInt(maxRefreshDelay) seconds, refreshMinutes minutes)(checkerActors.values.foreach(_ ! CheckIt))
  }

  private def registerNewChat(chatId: Long): Unit = {
    if (!chatIdz.contains(chatId)) {
      chatIdz += chatId
      val newWorker = system.actorOf(Worker.props(chatId), s"Worker$chatId")
      workerActors += (chatId -> newWorker)
      newWorker ! WorkerSendText("You are now registered to the System")
      log.info(s"Registered new ${newWorker.path.name}")

      Source(List(chatId))
        .map(id => ByteString(id + "\n"))
        .toMat(FileIO.toPath(Paths.get(FilePaths.chats), Set(WRITE, APPEND, CREATE)))(Keep.right)
        .run()


    } else {
      workerActors(chatId) ! WorkerSendText("You are already registered to the System")
      log.info(s"ChatId $chatId is already registered")
    }
  }

}

object Broker {
  def props(chatIds: Set[Long], checkerInitDatas: Set[CheckerInitData])
           (implicit system: ActorSystem,
            request: RequestHandler,
            materializer: Materializer,
            executor: ExecutionContext): Props = Props(new Broker(chatIds, checkerInitDatas))

  final case class StartDistributor()

  final case class RegisterChat(chatId: Long)

  final case class NotifyAll(note: String)

  final case class RegisterUrl(url: String)

  final case class UnregisterUrl(urlCheckerName: String)

}
