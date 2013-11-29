/*
 * Copyright 2013 Michał Rus <https://michalrus.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package edu.agh.gst.crawler

import scala.util.{Success, Failure, Try}
import java.awt.Image
import scala.util.matching.Regex
import com.ning.http.client.Cookie
import javax.imageio.ImageIO
import scala.xml.Node

object GoogleScholarCrawler {
  val GoogleStep = 10

  val YearRegex = """(\d\d\d\d)""".r
  val YearRegexGroup = 1
  val CitedRegex = """Cited by (\d+)""".r
  val CitedRegexGroup = 1

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
    import net.liftweb.util.Html5

    def get(n: Node, tag: String, clazz: String) =
      n \\ tag filter (_ attribute "class" exists (_.text.split(" \t\n\r".toCharArray) contains clazz))

    def toInt(s: String) = Try(s.toInt).toOption

    val cs = for {
      e <- (Html5 parse r).toSeq
      r <- get(e, "div", "gs_r") flatMap (get(_, "div", "gs_ri"))
      tyear <- get(r, "div", "gs_a") lift 0 map (_.text)
      year <- YearRegex findFirstMatchIn tyear map (_ group YearRegexGroup) flatMap toInt
      if isYear(year)
    } yield {
      val cited = get(r, "div", "gs_fl") lift 0 map (_.text) flatMap (
        CitedRegex findFirstMatchIn _ map (_ group CitedRegexGroup) flatMap toInt)
      CrawlerEntry(year, cited getOrElse 0)
    }

    val entries = cs.toList

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
