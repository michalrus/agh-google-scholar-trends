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

import java.awt.Image
import scala.util.matching.Regex
import com.ning.http.client.Response
import dispatch._, Defaults._

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

class GoogleScholarCrawler(captcha: Image => Future[String]) extends Crawler {

  import Crawler._, GoogleScholarCrawler._

  protected def requestStep = GoogleStep

  protected def request(q: String, start: Int) = {
    val svc = url("http://scholar.google.pl/scholar") <<? Map(
      "start" -> start.toString,
      "q" -> q,
      "hl" -> "en",
      "as_sdt" -> "0,5"
    )
    CookieHttp(svc).either
  }

  protected def handleResponse(resp: Response): Future[ParsedResponse] = {
    if (resp.getStatusCode / 100 == 2) {
      val (entries, more_?) = parse(resp.getResponseBody)
      val andThen = if (more_?) AndThen.HasMore else AndThen.Finished
      Future successful ParsedResponse(entries, andThen)
    } else {
      tryCaptcha(resp) map (_ => ParsedResponse(Nil, AndThen.RerunLast))
    }
  }

  private def parse(r: String): (List[CrawlerEntry], Boolean) = {
    import net.liftweb.util.Html5

    val cs = for {
      e <- (Html5 parse r).toSeq
      r <- selector(e, "div", "gs_r") flatMap (selector(_, "div", "gs_ri"))
      tyear <- selector(r, "div", "gs_a") lift 0 map (_.text)
      year <- YearRegex findFirstMatchIn tyear map (_ group YearRegexGroup) flatMap toInt
      if isYear(year)
    } yield {
      val cited = selector(r, "div", "gs_fl") lift 0 map (_.text) flatMap (
        CitedRegex findFirstMatchIn _ map (_ group CitedRegexGroup) flatMap toInt)
      CrawlerEntry(year, cited getOrElse 0)
    }

    val entries = cs.toList

    (entries, entries.nonEmpty)
  }

  /**
   * Tries to send a user-recognized CAPTCHA to Google.
   * @param resp A response asking for the CAPTCHA
   * @return A successful Future if Google accepted the CAPTCHA solution.
   */
  private def tryCaptcha(resp: Response): Future[Unit] = {
    def get(r: Regex, group: Int) =
      r findFirstMatchIn resp.getResponseBody map (_ group group) flatMap unescape

    val host = resp.getUri.getScheme + "://" + resp.getUri.getHost

    val x = for {
      id <- get(CFIdRegex, CFIdRegexGroup)
      continue <- get(CFContinueRegex, CFContinueRegexGroup)
      image <- get(CaptchaRegex, CaptchaRegexGroup) map (host + _)
    } yield {
      def submit(answer: String): Future[Unit] = {
        val svc = url(host + "/sorry/Captcha") <<? Map(
          "continue" -> continue,
          "id" -> id,
          "captcha" -> answer)
        CookieHttp(svc) flatMap { resp =>
          val succeeded_? = (resp.getStatusCode / 100 == 2) &&
            (resp.getResponseBody contains "Redirecting")

          if (succeeded_?) Future successful {()}
          else tryCaptcha(resp)
        }
      }

      dloadImage(image) flatMap captcha flatMap submit
    }

    x getOrElse (Future failed new Exception("not a captcha request"))
  }

}
