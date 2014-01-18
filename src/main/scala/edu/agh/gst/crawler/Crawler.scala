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

import scala.util.{Failure, Success, Random, Try}
import com.ning.http.client.Response
import dispatch._, Defaults._
import java.awt.Image
import javax.imageio.ImageIO
import scala.xml.Node
import rx.lang.scala.{Subject, Observable}

case class HttpError(response: Response)
  extends Exception(s"Failed with HTTP ${response.getStatusCode} ${response.getStatusText}.")

object CrawlerEntry { val zero = CrawlerEntry(0, 0) }
case class CrawlerEntry(year: Int, citations: Int)

object Crawler {

  private val MinYear = 1980
  private val MaxYear = java.util.Calendar.getInstance.get(java.util.Calendar.YEAR) + 1
  def isYear(i: Int) = i >= MinYear && i <= MaxYear

  private val rng = new Random
  private val MinSleep = 1000
  private val MaxSleep = 4000
  def sleepDuration = ((MinSleep min MaxSleep) + rng.nextInt(MaxSleep - MinSleep)).toLong

  val UserAgent = "Mozilla/5.0 (Windows NT 6.0; rv:15.0) Gecko/20100101 Firefox/15.0.1"
  val AcceptLanguage = "en-us,en;q=0.5"

  def unescape(s: String) = Try((xml.XML loadString ("<x>" + s + "</x>")).text).toOption

  def dloadImage(imageUrl: String): Future[Image] =
    CookieHttp(url(imageUrl)) flatMap { resp =>
      if (resp.getStatusCode / 100 == 2)
        Future(ImageIO read resp.getResponseBodyAsStream)
      else Future failed new Exception
    }

  def selector(n: Node, tag: String, clazz: String) =
    n \\ tag filter (_ attribute "class" exists (_.text.split(" \t\n\r".toCharArray) contains clazz))

  def toInt(s: String) = Try(s.toInt).toOption

}

trait Crawler {

  protected object AndThen extends Enumeration {
    val Finished, HasMore, RerunLast = Value
  }
  protected case class ParsedResponse(entries: List[CrawlerEntry], andThen: AndThen.Value)

  final def crawl(query: String): Observable[CrawlerEntry] = {
    val subject = Subject[CrawlerEntry]()

    def loop(start: Int, delayed: Boolean) { val _ = Future {
      if (delayed) concurrent.blocking(Thread sleep Crawler.sleepDuration)

      val r = request(query, start)

      for (exc <- r.left)
        subject onError exc

      for (resp <- r.right)
        handleResponse(resp) onComplete {
          case Failure(why) => subject onError why
          case Success(ParsedResponse(entries, andThen)) =>
            entries foreach subject.onNext
            andThen match {
              case AndThen.Finished => subject.onCompleted()
              case AndThen.HasMore => loop(start + requestStep, delayed = true)
              case AndThen.RerunLast => loop(start + 0, delayed = true)
            }
        }
    }}
    loop(0, delayed = false)

    subject
  }

  protected def handleResponse(resp: Response): Future[ParsedResponse]

  protected def request(q: String, start: Int): Future[Either[Throwable, Response]]

  protected def requestStep: Int

}
