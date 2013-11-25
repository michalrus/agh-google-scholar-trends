package edu.agh.gst.ui

import javax.swing.JPanel
import org.jfree.chart.{ChartPanel, ChartFactory, JFreeChart}
import org.jfree.data.xy.{XYSeriesCollection, XYSeries}
import org.jfree.chart.plot.PlotOrientation
import java.awt.BorderLayout

class Chart extends JPanel {

  val series = new XYSeries("Google Scholar")
  val dataset = new XYSeriesCollection
  dataset addSeries series
  val jfc = ChartFactory createXYLineChart ("Accumulated article counts", "Year",
    "Number so far", dataset, PlotOrientation.VERTICAL, true, true, false)
  add(new ChartPanel(jfc), BorderLayout.CENTER)

  import collection.mutable
  val data = new mutable.HashMap[Int, Int]

  def addYears (years: List[Int]) {
    years foreach { y =>
      val old = data getOrElse (y, 0)
      data += y -> (old + 1)
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

    series clear()
    accd foreach { case (y, num) => series add(y, num) }
  }

}
