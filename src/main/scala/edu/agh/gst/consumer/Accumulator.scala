package edu.agh.gst.consumer

import edu.agh.gst.crawler.CrawlerEntry

case class YearData(articles: Int, citations: Int) {
  def +(that: YearData) = YearData(this.articles + that.articles, this.citations + that.citations)
}

trait Consumer {
  def refresh(title: String, source: Accumulator)
}

final class Accumulator {

  def data = accd

  def consume(entries: List[CrawlerEntry]) = {
    entries foreach { e =>
      val old = raw getOrElse (e.year, YearData(0, 0))
      raw += e.year -> (old + YearData(1, e.citations))
    }
    recalculate()
  }

  def reset() {
    raw clear()
    recalculate()
  }

  import collection.mutable
  private val raw = new mutable.HashMap[Int, YearData]
  private var accd: Vector[(Int, YearData)] = Vector.empty

  private def recalculate() {
    val vec = raw.toVector.sortWith {
      case ((y1, _), (y2, _)) => y1 < y2
    }

    accd = if (vec.isEmpty) Vector.empty
    else (vec.tail scanLeft vec.head) {
      case ((_, acc), (y, num)) => (y, acc + num)
    }
  }

}
