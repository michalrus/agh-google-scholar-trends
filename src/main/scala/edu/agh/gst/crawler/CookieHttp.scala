package edu.agh.gst.crawler

object CookieHttp {
  import collection.mutable
  import dispatch._, Defaults._
  import com.ning.http.client.Cookie
  import Crawler._

  private val cookies = new mutable.HashMap[(String, String), Cookie]

  private def addCookies(req: Req): Req = {
    def host(u: String) = { // host("htttttps://www.www.google.com/asd") == "www.www.google.com"
    val np = (u splitAt ((u indexOf "//") + 2))._2
      val i = np indexOf "/"
      if (i < 0) np
      else (np splitAt i)._1
    }

    def fromDomain(c: Cookie) = host(req.url) endsWith c.getDomain

    val raw = cookies.values filter fromDomain map (c => c.getName + "=" + c.getValue)

    if (raw.isEmpty) req
    else req <:< Map("Cookie" -> (raw mkString "; "))
  }

  def apply(req: Req) = {
    val svc = addCookies((req setFollowRedirects true) <:<
      Map(
        "User-Agent" -> UserAgent,
        "Accept-Language" -> AcceptLanguage
      ))

    Http(svc) flatMap { resp =>
      import collection.JavaConversions._
      resp.getCookies foreach { c =>
        cookies += (c.getDomain, c.getName) -> c
      }
      Future(resp)
    }
  }
}
