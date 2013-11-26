package edu.agh.gst.crawler

import scala.util.Try

class MicrosoftCrawler extends Crawler {

  def crawl[F](query: String)(f: (Try[List[CrawlerEntry]]) => F) {}

}
