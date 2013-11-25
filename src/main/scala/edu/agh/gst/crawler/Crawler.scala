package edu.agh.gst.crawler

class Crawler {

  def crawl[F](query: String)(f: List[Int] => F) {
    f(1 :: 2 :: 3 :: Nil)
  }

}
