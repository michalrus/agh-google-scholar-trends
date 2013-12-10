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

import javax.swing._
import edu.agh.gst.SwingHelper
import scala.util.Try
import java.awt.{Image, BorderLayout, Dimension}
import java.awt.event.{ActionEvent, ActionListener}
import edu.agh.gst.crawler._
import edu.agh.gst.crawler.HttpError
import scala.util.Failure
import scala.util.Success
import edu.agh.gst.crawler.CrawlerEntry
import edu.agh.gst.consumer.{Accumulator, CsvExporter, Consumer}

class MainFrame extends JFrame with SwingHelper {

  private lazy val go = new JButton("Go!")
  private lazy val query = new JTextField
  private lazy val results = new JLabel("0")
  private lazy val theSame = new JCheckBox("Same for all")
  private lazy val controls = go :: query :: theSame :: Nil

  case class Tab(name: String, crawler: Crawler,
                 consumers: List[Consumer], var finished: Boolean = false) {
    val accumulator = new Accumulator
  }

  lazy val directory = {
    val fc = new JFileChooser
    fc setFileSelectionMode JFileChooser.DIRECTORIES_ONLY
    fc setDialogTitle "Choose CSV export directory..."
    if (0 == fc.showOpenDialog(this)) {
      Option(fc.getSelectedFile)
    } else None
  }

  private lazy val crawlers =
    Tab("Google Scholar", new GoogleScholarCrawler(showCaptcha), new Chart :: new CsvExporter(directory) :: Nil) ::
      Tab("Microsoft Academic Search", new MicrosoftCrawler, new Chart :: new CsvExporter(directory) :: Nil) ::
      Nil

  private val totalAcc = new Accumulator
  private val total = new Chart

  laterOnUiThread {
    Try(UIManager setLookAndFeel UIManager.getSystemLookAndFeelClassName)

    Option(getClass getResource "/icon.png") foreach
      (i => setIconImage(new ImageIcon(i).getImage))

    setTitle("agh-google-scholar-trends")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setSize(new Dimension(800, 600))
    setMinimumSize(new Dimension(400, 300))
    setLocationRelativeTo(null)

    directory

    buildUi()

    setVisible(true)
  }

  private def buildUi() {
    buildToolbar()
    buildCharts()
  }

  private def buildCharts() {
    val tabs = new JTabbedPane
    add(tabs, BorderLayout.CENTER)

    tabs addTab("Total", total)

    crawlers foreach { c =>
      c.consumers foreach {
        case ch: Chart => tabs addTab(c.name, ch)
        case _ =>
      }
    }
  }

  private def showError(s: String) =
    JOptionPane showMessageDialog(this, s, "Error", JOptionPane.WARNING_MESSAGE)

  import concurrent._

  private def showCaptcha(img: Image) = {
    val p = Promise[String]()
    val text = JOptionPane showInputDialog (this, new JLabel(new ImageIcon(img)), "Enter the captcha", JOptionPane.QUESTION_MESSAGE)
    p complete (Option(text) match {
      case Some(t) => Success(t)
      case _ => Failure(new Exception)
    })
    p.future
  }

  private def onCrawled(years: Try[List[CrawlerEntry]], crawler: Tab) = laterOnUiThread {
    def finish() {
      crawler.finished = true
      if (crawlers forall (_.finished)) {
        showError("No more articles can be found!")
        controls foreach (_ setEnabled true)
        query requestFocus()
      }
    }

    years match {
      case Success(es) if es.isEmpty =>
        finish()
      case Success(es) =>
        numProcessed += es.length
        crawler.accumulator consume es
        crawler.consumers foreach (_ refresh (query.getText, crawler.accumulator))
        totalAcc consume es
        total refresh (query.getText, totalAcc)
      case Failure(e: HttpError) =>
        showError(e.getMessage + "\n\n" + e.response.getResponseBody)
        finish()
      case Failure(e) =>
        showError(e.toString)
        finish()
    }
  }

  private def startCrawling() {
    controls foreach (_ setEnabled false)
    numProcessed = 0
    val qt = query.getText
    totalAcc reset()
    total refresh (qt, totalAcc)
    (crawlers zip getQueries(theSame.isSelected, qt)) foreach { case (c, q) =>
      c.finished = false
      c.accumulator reset()
      c.consumers foreach (_ refresh (qt, c.accumulator))
      (c.crawler crawl q)(onCrawled(_, c))
    }
  }

  private def buildToolbar() {
    val al = new ActionListener {
      def actionPerformed(e: ActionEvent) = startCrawling()
    }

    query addActionListener al
    go addActionListener al

    val tb = new JToolBar
    tb setFloatable false
    add(tb, BorderLayout.PAGE_START)

    tb add new JLabel("Query: ")

    tb add query

    theSame setSelected true
    tb add theSame

    tb addSeparator()

    tb add go

    tb addSeparator()

    tb add new JLabel("Articles processed: ")

    tb add results

    ()
  }

  private def getQueries(same: Boolean, default: String): List[String] = {
    if (same) crawlers map (_ => default)
    else crawlers map { cr =>
      val text = JOptionPane.showInputDialog(this, s"Enter a specialized query for ${cr.name}:",
        cr.name, JOptionPane.QUESTION_MESSAGE, null, null, default).toString
      Option(text) match {
        case Some(t) => t
        case _ => default
      }
    }
  }

  private var _numProcessed = 0
  private def numProcessed = _numProcessed
  private def numProcessed_=(v: Int) = {
    _numProcessed = v
    results setText v.toString
  }

}
