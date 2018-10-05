package actors

import java.net.URL

import actors.Broker.NotifyAll
import actors.DifferenceFinder.FindDifference
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.lightbend.emoji.Emoji
import net.ruippeixotog.scalascraper.model.Document
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils

import scala.util.Try

class DifferenceFinder(broker: ActorRef) extends Actor with ActorLogging {

  def receive: Receive = {
    case FindDifference(contractorName, url, oldDocument, newDocument) => findDiff(contractorName, url, oldDocument, newDocument)
    case _ => log.warning(s"${self.path.name} received invalid Command")
  }

  def findDiff(contractorName: String, url: String, oldDocument: Document, newDocument: Document): Unit =
    if (oldDocument != null && newDocument != null)
      findDiff(contractorName, url, oldDocument.body.toString, newDocument.body.toString)

  def findDiff(contractorName: String, url: String, oldDocument: String, newDocument: String): Unit = {
    if (oldDocument == null || newDocument == null) {
      return
    }

    val linksHttpOld = DifferenceFinder.findAllHttpStrings(oldDocument, url)
    val linksLinkTagsOld = DifferenceFinder.findAllLinkTags(oldDocument, url)

    val linksHttpNew = DifferenceFinder.findAllHttpStrings(newDocument, url)
    val linksLinkTagsNew = DifferenceFinder.findAllLinkTags(newDocument, url)

    val allUniqueLinksOld = DifferenceFinder.filterUniqueUrls(linksHttpOld, linksLinkTagsOld)
    val allUniqueLinksNew = DifferenceFinder.filterUniqueUrls(linksHttpNew, linksLinkTagsNew)

    val result = allUniqueLinksNew.diff(allUniqueLinksOld)

    if(result.nonEmpty) {
      val messageHead = s"*$contractorName* had an update ${Emoji.get(0x1F631).getOrElse("")}\n\n"
      val messageBody = result.map(DifferenceFinder.getUrl)
        .filter(_.nonEmpty)
        .map(_.get)
        .map(url => {
          val path = url.getPath
          val symbolAfterLastSlash = path.lastIndexOf('/' + 1)
          s"[${
            path.substring(
              // wenn mindestens noch 3 zeichen vorhanden sind nach dem slash und vorhanden (> -1)
              if (symbolAfterLastSlash < path.length - 3 && symbolAfterLastSlash >= 0) symbolAfterLastSlash else 0
            )
          }](${url.toString})\n"
        })
        .reduce(_ + _)

      log.warning(s"${self.path.name} found ${result.size} new Links on $contractorName!!!")
      broker ! NotifyAll(messageHead + messageBody)
    } else {
      log.warning(s"${self.path.name} couldn't find any new Links on $contractorName...")
    }
  }

}

object DifferenceFinder {
  def props(broker: ActorRef): Props = Props(new DifferenceFinder(broker))

  final case class FindDifference(contractorName: String, url: String, oldDocument: Document, newDocument: Document)

  def findAllHttpStrings(diff: String, baseUrl: String): Set[String] = {
    val hostOpt = getHostDomain(baseUrl)
    hostOpt.map(host =>
      (""""(https?:\/\/w{0,3}\.?""" + host.replaceFirst("www", "") +"""[\/\S]{1,})\"""").r
        .findAllIn(diff)
        .toSet[String]
        .map(_.replaceAll("\"", "").replace("://www.", "://"))
    ) getOrElse Set.empty[String]
  }

  def findAllLinkTags(diff: String, baseUrl: String): Set[String] = {
    val url = getUrl(baseUrl)

    url.map(implicit u => {
      val textWitLinks = diff
        .replaceAll("""<script[^>]*>(.*?)<\/script>""", "")
        .replaceAll("""<(?!\/?a(?=>|\s.*>))\/?.*?>""", "")


      "<a href=\"([\\/\\S]{1,})\">".r
        .findAllIn(textWitLinks)
        .toSet[String]
        .map(_.replace("<a href=\"", "").replace("\">", ""))
        .map(concatHost)
    }) getOrElse Set.empty[String]
  }

  private def concatHost(potentialRelLink: String)(implicit url: URL): String =
    if (potentialRelLink.contains(url.getHost)) potentialRelLink // is absolute Link
    else s"${url.getProtocol}://${url.getHost}$potentialRelLink"

  def filterUniqueUrls(linkStrings: Set[String]*): Set[String] = {
    linkStrings
      .reduce(_ ++ _)
      .map(getUrl)
      .filter(_.nonEmpty)
      .filter(url => FilenameUtils.getExtension(url.get.getPath) == "")
      .map(urlOpt => (urlOpt.get, urlOpt.get.getPath))
      .groupBy(_._2)
      .mapValues(_.head._1)
      .values
      .map(_.toString)
      .toSet[String]
  }

  def getHostDomain(baseUrl: String): Option[String] =
    getUrl(baseUrl)
      .map(_.getHost)
      .map(_.replaceFirst("www.", ""))

  private def getUrl(baseUrl: String): Option[URL] = {
    Try {
      new URL(baseUrl)
    }.toOption
  }
}
