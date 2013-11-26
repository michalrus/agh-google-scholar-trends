package edu.agh.gst.crawler

import scala.util.{Success, Failure, Try}

object GoogleScholarCrawler {
  private val rng = new java.util.Random

  val GoogleStep = 10
  private val SleepMin = 3
  private val SleepMax = 7
  def randomSleepDuration = 1000L * (SleepMin + rng.nextInt % (SleepMax - SleepMin))

  val YearRegex = """<div class="gs_a">.*?(\d\d\d\d)""".r
  val YearRegexGroup = 1
  def isYear(i: Int) = i >= 1950 && i <= 2050
}

case class HttpError(response: com.ning.http.client.Response)
  extends Exception(s"Failed with HTTP ${response.getStatusCode} ${response.getStatusText}.")

class GoogleScholarCrawler extends Crawler {

  import dispatch._, Defaults._, Crawler._, GoogleScholarCrawler._

  override def crawl[F](query: String)(f: Try[List[CrawlerEntry]] => F) {
    def loop(start: Int) {
      val r = request(query, start)

      for (exc <- r.left)
        f(Failure(exc))

      for (resp <- r.right) {
        if (resp.getStatusCode / 100 == 2) {
          val (entries, more_?) = parse(resp.getResponseBody)
          if (more_?) Future {
            Thread sleep randomSleepDuration
            loop(start + GoogleStep)
          }
          f(Success(entries))
        } else f(Failure(HttpError(resp)))
      }
    }

    loop(0)
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
