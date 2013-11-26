package edu.agh.gst.crawler

import scala.util.Try

object Crawler {
  val GoogleStep = 10
  val SleepMs = 1000

  //val YearRegex = """<div class="gs_a">[^<]*?(\d\d\d\d)""".r
  val YearRegex = """<div class="gs_a">.*?(\d\d\d\d)""".r
  val YearRegexGroup = 1
  def isYear(i: Int) = i >= 1950 && i <= 2050
}

class Crawler {

  import dispatch._, Defaults._, Crawler._

  def crawl[F](query: String)(f: List[Int] => F) {
    def loop(start: Int): Unit =
      request(query, start) onSuccess {
        case r =>
          val (years, more_?) = parse(r)
          if (more_?) {
            Thread sleep SleepMs
            loop(start + GoogleStep)
          }
          f(years)
      }

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
      addQueryParameter("as_sdt", "0,5")
    Http(svc OK as.String)
  }

}
