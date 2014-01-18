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

package edu.agh.gst.ui

import javax.swing.JPanel
import org.jfree.chart.{ChartPanel, ChartFactory}
import org.jfree.data.xy.{XYSeriesCollection, XYSeries}
import org.jfree.chart.plot.PlotOrientation
import java.awt.{BasicStroke, Color, BorderLayout}
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.renderer.xy.StandardXYItemRenderer
import edu.agh.gst.consumer.{YearData, Consumer}
import scala.collection.immutable.TreeMap
import edu.agh.gst.SwingHelper

class Chart extends JPanel with Consumer with SwingHelper {

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

  def refresh(title: String, data: TreeMap[Int, YearData]) = laterOnUiThread { () =>
    chart setTitle title

    articlesSeries clear()
    citationsSeries clear()
    data foreach { case (yr, d) =>
      articlesSeries add(yr.toDouble, d.articles.toDouble)
      citationsSeries add(yr.toDouble, d.citations.toDouble)
    }
  }

}
