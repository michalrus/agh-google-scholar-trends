package edu.agh.gst.ui

import javax.swing.JPanel
import org.jfree.chart.{ChartPanel, ChartFactory, JFreeChart}
import org.jfree.data.xy.{XYSeriesCollection, XYSeries}
import org.jfree.chart.plot.{XYPlot, PlotOrientation}
import java.awt.{BasicStroke, Color, BorderLayout}
import org.jfree.chart.axis.{NumberTickUnit, NumberAxis}
import edu.agh.gst.crawler.CrawlerEntry
import org.jfree.chart.renderer.xy.StandardXYItemRenderer

class Chart extends JPanel {

  private val articlesSeries = new XYSeries("Number of articles")
  private val citationsSeries = new XYSeries("Number of citations")
  private val articleDataset, citeDataset = new XYSeriesCollection
  articleDataset addSeries articlesSeries
  citeDataset addSeries citationsSeries
  private val chart = ChartFactory createXYLineChart ("Accumulated article counts", "Year",
    "Number of articles", articleDataset, PlotOrientation.VERTICAL, true, true, false)

  private val plot = chart.getXYPlot
  private val xAxis = plot.getDomainAxis.asInstanceOf[NumberAxis]
  xAxis setStandardTickUnits NumberAxis.createIntegerTickUnits
  private val articleAxis = plot.getRangeAxis.asInstanceOf[NumberAxis]
  articleAxis setStandardTickUnits NumberAxis.createIntegerTickUnits

  plot.getRenderer setSeriesStroke(0, new BasicStroke(2.0f))

  private val citeAxis = new NumberAxis("Number of citations")
  citeAxis setStandardTickUnits NumberAxis.createIntegerTickUnits
  plot setRangeAxis (1, citeAxis)
  plot setDataset (1, citeDataset)
  plot mapDatasetToRangeAxis (1, 1)
  private val citeRenderer = new StandardXYItemRenderer
  citeRenderer setSeriesPaint (0, Color.BLUE)
  citeRenderer setSeriesStroke (0, new BasicStroke(2.0f))
  plot setRenderer (1, citeRenderer)

  setLayout(new BorderLayout)
  add(new ChartPanel(chart), BorderLayout.CENTER)

  case class YearData(articles: Int, citations: Int) {
    def +(that: YearData) = YearData(this.articles + that.articles, this.citations + that.citations)
  }

  import collection.mutable
  private val data = new mutable.HashMap[Int, YearData]

  def setTitle(s: String) = chart setTitle s

  def addEntries (entries: List[CrawlerEntry]) {
    entries foreach { e =>
      val old = data getOrElse (e.year, YearData(0, 0))
      data += e.year -> (old + YearData(1, e.citations))
    }
    refresh()
  }

  private def refresh() {
    val vec = data.toVector.sortWith {
      case ((y1, _), (y2, _)) => y1 < y2
    }

    val accd = if (vec.isEmpty) Vector.empty
    else (vec.tail scanLeft vec.head) {
      case ((_, acc), (y, num)) => (y, acc + num)
    }

    articlesSeries clear()
    citationsSeries clear()
    accd foreach { case (y, d) =>
      articlesSeries add(y, d.articles)
      citationsSeries add(y, d.citations)
    }
  }

}
