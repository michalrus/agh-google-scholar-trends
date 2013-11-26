package edu.agh.gst.crawler

import scala.util.{Success, Failure, Try}
import java.net.URL
import javax.imageio.ImageIO
import java.awt.Image

object GoogleScholarCrawler {
  val GoogleStep = 10
  def sleepDuration = 1000L

  val YearRegex = """<div class="gs_a">.*?(\d\d\d\d)""".r
  val YearRegexGroup = 1
  def isYear(i: Int) = i >= 1950 && i <= 2050

  val CaptchaRegex = """src="(/sorry/image[^"]+)""".r
  val CaptchaRegexGroup = 1
}

import com.ning.http.client.Response

case class HttpError(response: Response)
  extends Exception(s"Failed with HTTP ${response.getStatusCode} ${response.getStatusText}.")

import dispatch._, Defaults._

class GoogleScholarCrawler(captcha: Image => Future[String]) extends Crawler {

  import Crawler._, GoogleScholarCrawler._

  override def crawl[F](query: String)(f: Try[List[CrawlerEntry]] => F) {
    def loop(start: Int) {
      def getNext = Future {
        Thread sleep sleepDuration
        loop(start + GoogleStep)
      }

      val r = request(query, start)

      for (exc <- r.left)
        f(Failure(exc))

      for (resp <- r.right) {
        if (resp.getStatusCode / 100 == 2) {
          val (entries, more_?) = parse(resp.getResponseBody)
          if (more_?) getNext
          f(Success(entries))
        } else tryCaptcha(resp) onComplete {
          case Success(true) => getNext
          case _ => f(Failure(HttpError(resp)))
        }
      }
    }

    loop(0)
  }

  private def tryCaptcha[F](resp: Response): Future[Boolean] = {
    def unescape(s: String) = Try((xml.XML loadString ("<x>" + s + "</x>")).text).toOption

    val image: Option[String] =
      CaptchaRegex findFirstMatchIn resp.getResponseBody map (_ group CaptchaRegexGroup) flatMap
        unescape map (resp.getUri.getScheme + "://" + resp.getUri.getHost + _)

    def submit(answer: String): Future[Boolean] = ???

    image match {
      case Some(img) => dloadImage(img) flatMap captcha flatMap submit
      case _ => Future failed new Exception("not a captcha request")
    }
  }

  private def dloadImage(url: String): Future[Image] = Future {
    concurrent.blocking {
      ImageIO read new URL(url)
    }
  }

  private def parse(r: String): (List[CrawlerEntry], Boolean) = {
    val entries = ((YearRegex findAllIn r).matchData map (_ group YearRegexGroup) map
      (s => Try(s.toInt).toOption)).flatten.toList filter isYear map (CrawlerEntry(_, 0))

    (entries, entries.nonEmpty)
  }

  private def request(q: String, start: Int) = {
    val svc = url("http://scholar.google.pl/scholar").
      addQueryParameter("start", start.toString).
      addQueryParameter("q", q).
      addQueryParameter("hl", "en").
      addQueryParameter("as_sdt", "0,5").
      setFollowRedirects(followRedirects = true) <:<
      Map("User-Agent" -> UserAgent)
    Http(svc).either
  }

}
