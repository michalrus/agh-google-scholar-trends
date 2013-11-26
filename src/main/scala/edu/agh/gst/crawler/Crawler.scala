package edu.agh.gst.crawler

import scala.util.Try

object Crawler {
  private val rng = new java.util.Random

  val GoogleStep = 10
  val UserAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36"
  private val SleepMin = 3
  private val SleepMax = 7
  def randomSleepDuration = 1000L * (SleepMin + rng.nextInt % (SleepMax - SleepMin))

  val YearRegex = """<div class="gs_a">.*?(\d\d\d\d)""".r
  val YearRegexGroup = 1
  def isYear(i: Int) = i >= 1950 && i <= 2050
}

class Crawler {

  import dispatch._, Defaults._, Crawler._

  def crawl[F](query: String)(f: Try[List[Int]] => F) {
    def loop(start: Int): Unit =
      request(query, start) onComplete { res => f(res map { r =>
        val (years, more_?) = parse(r)
        if (more_?) Future {
          Thread sleep randomSleepDuration
          loop(start + GoogleStep)
        }
        years
      })}

    loop(0)
  }

  private def parse(r: String): (List[Int], Boolean) = {
    val years = ((YearRegex findAllIn r).matchData map (_ group YearRegexGroup) map
      (s => Try(s.toInt).toOption)).flatten.toList filter isYear

    (years, years.nonEmpty)
  }

  private def request(q: String, start: Int) = {
    val svc = url("http://scholar.google.pl/scholar").
      addQueryParameter("start", start.toString).
      addQueryParameter("q", q).
      addQueryParameter("hl", "en").
      addQueryParameter("as_sdt", "0,5") <:<
      Map("User-Agent" -> UserAgent)
    Http(svc OK as.String)
  }

}
