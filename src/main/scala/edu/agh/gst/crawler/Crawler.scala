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
