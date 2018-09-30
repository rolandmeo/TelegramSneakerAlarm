/*
 * Author: Roland Meo
 */

package app

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Framing}
import akka.util.ByteString
import bots.BrokerBot
import credentials.{BotCredentials, FilePaths}
import model.CheckerInitData

import scala.concurrent.{ExecutionContext, Future}

object BotApp extends App {
  implicit val system: ActorSystem = ActorSystem("BotSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executor: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val token = BotCredentials.fullToken

  println("Starting BotApp")

  private val chatIds: Future[List[Long]] = FileIO.fromPath(Paths.get(FilePaths.chats))
    .via(Framing.delimiter(ByteString("\n"), 256).map(_.utf8String))
    .map(_.toLong)
    .runFold(List.empty[Long])((list, item) => item :: list)

  private val urlTriples: Future[List[CheckerInitData]] = FileIO.fromPath(Paths.get(FilePaths.urls))
    .via(Framing.delimiter(ByteString("\n"), 256).map(_.utf8String))
    .prefixAndTail(1)
    .flatMapConcat(_._2)
    .map { string =>
      val strings = string.split(",")
      CheckerInitData(
        strings(0).replace(" ", ""),
        strings(1),
        strings(2),
        strings(3).toBoolean
      )
    }
    .runFold(List.empty[CheckerInitData])((list, item) => item :: list)

  for {
    chats <- chatIds
    checkerInitDatas <- urlTriples
  } yield {
    println("Done Fetching Urls and ChatIds")
    new BrokerBot(token, chats.toSet, checkerInitDatas.toSet).run()
  }

}
