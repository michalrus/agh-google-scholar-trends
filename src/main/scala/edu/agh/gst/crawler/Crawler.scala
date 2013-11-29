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

import scala.util.Try

case class CrawlerEntry(year: Int, citations: Int)

object Crawler {

  private val MinYear = 1980
  private val MaxYear = java.util.Calendar.getInstance.get(java.util.Calendar.YEAR) + 1
  def isYear(i: Int) = i >= MinYear && i <= MaxYear

  private val rng = new util.Random
  private val MinSleep = 1000
  private val MaxSleep = 4000
  def sleepDuration = MinSleep + rng.nextInt(MaxSleep - MinSleep)

  val UserAgent = "Mozilla/5.0 (Windows NT 6.0; rv:15.0) Gecko/20100101 Firefox/15.0.1"
  val AcceptLanguage = "en-us,en;q=0.5"

  def unescape(s: String) = Try((xml.XML loadString ("<x>" + s + "</x>")).text).toOption

}

trait Crawler {

  def crawl[F](query: String)(f: Try[List[CrawlerEntry]] => F)

}
