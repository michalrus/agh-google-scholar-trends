package edu.agh.gst.crawler

import scala.util.Try

case class CrawlerEntry(year: Int, citations: Int)

object Crawler {

  private val rng = new util.Random
  private val MinSleep = 1000
  private val MaxSleep = 4000
  def sleepDuration = MinSleep + rng.nextInt(MaxSleep - MinSleep)

  val UserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36"

  def unescape(s: String) = Try((xml.XML loadString ("<x>" + s + "</x>")).text).toOption

}

trait Crawler {

  def crawl[F](query: String)(f: Try[List[CrawlerEntry]] => F)

}
