package edu.agh.gst.consumer

import edu.agh.gst.crawler.CrawlerEntry
import rx.lang.scala.Observable
import scala.collection.immutable.TreeMap

case class YearData(articles: Int, citations: Int) {
  def +(that: YearData) = YearData(this.articles + that.articles, this.citations + that.citations)
}

trait Consumer {
  def refresh(title: String, data: TreeMap[Int, YearData])
}

object Accumulator {

  /**
   * Transforms a `CrawlerEntry` with its history into...
   * @param in Input from a `Crawler`.
   * @return Observable of year -> `YearData`.
   */
  def yearData(in: Observable[List[CrawlerEntry]]): Observable[TreeMap[Int, YearData]] = {
    in.scan(TreeMap.empty[Int, YearData]) { (previous, entries) =>
      (entries foldLeft previous) { (acc, entry) =>
        val prevYear = acc getOrElse (entry.year, YearData(0, 0))
        acc + (entry.year -> (prevYear + YearData(1, entry.citations)))
      }
    }
  }

  def accumulate(in: Observable[List[CrawlerEntry]]): Observable[TreeMap[Int, YearData]] = {
    yearData(in) map {
      case tr if tr.isEmpty => tr
      case tr => (tr.tail scanLeft tr.head)((acc, el) => (el._1, acc._2 + el._2))
    }
  }

}
