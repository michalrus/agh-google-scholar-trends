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

import scala.util.{Failure, Success, Try}
import com.ning.http.client.Response
import dispatch._ //, Defaults._

object MicrosoftCrawler {
  val MicrosoftStep = 10

  val YearRegex = """(\d\d\d\d).*?""".r
  val YearRegexGroup = 1
  val CitedRegex = """Citations: (\d+)""".r
  val CitedRegexGroup = 1
}

class MicrosoftCrawler extends Crawler {

  import Crawler._, MicrosoftCrawler._

  protected def requestStep = MicrosoftStep

  protected def request(q: String, start: Int) = {
    val svc = url("http://academic.research.microsoft.com/Search") <<? Map(
      "query" -> q,
      "start" -> (start + 1).toString,
      "end" -> (start + MicrosoftStep).toString
    )
    CookieHttp(svc).either
  }

  protected def handleResponse(f: (Try[List[CrawlerEntry]]) => Unit)(resp: Response)(andThen: => Future[Unit]) {
    if (resp.getStatusCode / 100 == 2) {
      val (entries, more_?) = parse(resp.getResponseBody)
      if (more_?) andThen
      f(Success(entries))
    } else f(Failure(HttpError(resp)))
  }

  private def parse(r: String): (List[CrawlerEntry], Boolean) = {
    import net.liftweb.util.Html5

    val cs = for {
      e <- (Html5 parse r).toSeq
      paper <- selector(e, "li", "paper-item")
      tyear = selector(paper, "div", "conference").text
      year <- YearRegex findFirstMatchIn tyear map (_ group YearRegexGroup) flatMap toInt
      if isYear(year)
    } yield {
      val cite = selector(paper, "span", "citation") lift 0 map (_.text) flatMap (
        CitedRegex findFirstMatchIn _ map (_ group CitedRegexGroup) flatMap toInt)

      CrawlerEntry(year, cite getOrElse 0)
    }

    val entries = cs.toList

    (entries, entries.nonEmpty)
  }

}
