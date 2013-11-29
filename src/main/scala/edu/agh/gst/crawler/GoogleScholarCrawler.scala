package edu.agh.gst.crawler

import scala.util.{Success, Failure, Try}
import java.awt.Image
import scala.util.matching.Regex
import com.ning.http.client.Cookie
import javax.imageio.ImageIO

object GoogleScholarCrawler {
  val GoogleStep = 10

  val YearRegex = """<div class="gs_a">.*?(\d\d\d\d)""".r
  val YearRegexGroup = 1

  val CaptchaRegex = """src="(/sorry/image[^"]+)""".r
  val CaptchaRegexGroup = 1
  val CFContinueRegex = """<input type="hidden" name="continue" value="([^"]+)""".r
  val CFContinueRegexGroup = 1
  val CFIdRegex = """<input type="hidden" name="id" value="([^"]+)""".r
  val CFIdRegexGroup = 1
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
    def get(r: Regex, group: Int) =
      r findFirstMatchIn resp.getResponseBody map (_ group group) flatMap unescape

    val host = resp.getUri.getScheme + "://" + resp.getUri.getHost

    val x = for {
      id <- get(CFIdRegex, CFIdRegexGroup)
      continue <- get(CFContinueRegex, CFContinueRegexGroup)
      image <- get(CaptchaRegex, CaptchaRegexGroup) map (host + _)
    } yield {
      def submit(answer: String): Future[Boolean] = {
        val svc = url(host + "/sorry/Captcha") <<? Map(
          "continue" -> continue,
          "id" -> id,
          "captcha" -> answer)
        CookieHttp(svc) flatMap { resp =>
          val succeeded_? = (resp.getStatusCode / 100 == 2) &&
            (resp.getResponseBody contains "Redirecting")

          if (succeeded_?) Future(true)
          else tryCaptcha(resp)
        }
      }

      dloadImage(image) flatMap captcha flatMap submit
    }

    x getOrElse (Future failed new Exception("not a captcha request"))
  }

  private def dloadImage(imageUrl: String): Future[Image] =
    CookieHttp(url(imageUrl)) flatMap { resp =>
      if (resp.getStatusCode / 100 == 2)
        Future(ImageIO read resp.getResponseBodyAsStream)
      else Future failed new Exception
    }

  private def parse(r: String): (List[CrawlerEntry], Boolean) = {
    val entries = ((YearRegex findAllIn r).matchData map (_ group YearRegexGroup) map
      (s => Try(s.toInt).toOption)).flatten.toList filter isYear map (CrawlerEntry(_, 0))

    (entries, entries.nonEmpty)
  }

  private def request(q: String, start: Int) = {
    val svc = url("http://scholar.google.pl/scholar") <<? Map(
      "start" -> start.toString,
      "q" -> q,
      "hl" -> "en",
      "as_sdt" -> "0,5"
    )
    CookieHttp(svc).either
  }

}
