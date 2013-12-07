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

object CookieHttp {
  import collection.mutable
  import dispatch._, Defaults._
  import Crawler._

  private case class CookieKey(domain: String, name: String)
  private case class CookieVal(value: String)

  private val cookies = new mutable.HashMap[CookieKey, CookieVal] with
    mutable.SynchronizedMap[CookieKey, CookieVal]

  private def hostOfUrl(u: String): String = { // host("htttttps://www.www.google.com/asd") == "www.www.google.com"
    val np = (u splitAt ((u indexOf "//") + 2))._2
    val i = np indexOf "/"
    if (i < 0) np
    else (np splitAt i)._1
  }

  private def addCookies(req: Req): Req = {
    val raw = cookies filter {
      case (k, _) => hostOfUrl(req.url) endsWith k.domain
    } map {
      case (k, v) => k.name + "=" + v.value
    }

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
        val domain = Option(c.getDomain) getOrElse hostOfUrl(svc.url)
        cookies += CookieKey(domain, c.getName) -> CookieVal(c.getValue)
      }
      Future(resp)
    }
  }
}
