package edu.agh.gst.crawler

object Crawler {
  val GoogleStep = 10
  val SleepMs = 1000
}

class Crawler {

  import dispatch._, Defaults._, Crawler._

  def crawl[F](query: String)(f: List[Int] => F) {
    def loop(start: Int): Unit =
      request(query, start) onSuccess {
        case r =>
          val (years, more_?) = parse(r)
          f(years)
          if (more_?) {
            Thread sleep SleepMs
            loop(start + GoogleStep)
          }
      }

    loop(0)
  }

  private def parse(r: String): (List[Int], Boolean) = {
    // FIXME
    (1 :: 2 :: 3 :: 4 :: Nil, false)
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
