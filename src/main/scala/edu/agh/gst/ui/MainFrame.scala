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
import java.awt.{Component, Image, BorderLayout, Dimension}
import java.awt.event.{ActionEvent, ActionListener}
import edu.agh.gst.crawler._
import edu.agh.gst.crawler.HttpError
import scala.util.Failure
import scala.util.Success
import edu.agh.gst.crawler.CrawlerEntry
import edu.agh.gst.consumer.{Accumulator, CsvExporter, Consumer}
import scala.reflect.io.Directory
import rx.lang.scala.Observable
import scala.collection.immutable.TreeMap

class MainFrame extends JFrame with SwingHelper {

  private lazy val go = new JButton("Go!")
  private lazy val query = new JTextField
  private lazy val results = new JLabel("0")
  private lazy val theSame = new JCheckBox("Same for all")
  private lazy val controls = go :: query :: theSame :: Nil

  case class Tab(name: String, crawler: Crawler,
                 consumers: List[Consumer])

  private lazy val directory = {
    val fc = new JFileChooser
    fc setFileSelectionMode JFileChooser.DIRECTORIES_ONLY
    fc setDialogTitle "Choose CSV export directory..."
    if (0 == fc.showOpenDialog(this)) {
      Option(Directory(fc.getSelectedFile))
    } else None
  }

  private lazy val crawlers =
    Tab("Google Scholar", new GoogleScholarCrawler(showCaptcha), new Chart :: new CsvExporter(directory, "google") :: Nil) ::
      Tab("Microsoft Academic Search", new MicrosoftCrawler, new Chart :: new CsvExporter(directory, "microsoft") :: Nil) ::
      Nil

  private val totals = new Chart

  laterOnUiThread { () =>
    try { UIManager setLookAndFeel UIManager.getSystemLookAndFeelClassName } catch { case _: Throwable => }

    Option(getClass getResource "/icon.png") foreach
      (i => setIconImage(new ImageIcon(i).getImage))

    setTitle("agh-google-scholar-trends")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setSize(new Dimension(800, 600))
    setMinimumSize(new Dimension(400, 300))
    setLocationRelativeTo(ExplicitNull.Component)

    val _ = directory
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

    tabs addTab("Total", totals)

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
    val _ = p tryComplete (Option(text) match {
      case Some(t) => Success(t)
      case _ => Failure(new Exception)
    })
    p.future
  }

  private def startCrawling() {
    controls foreach (_ setEnabled false)
    val qt = query.getText

    val allEntriesList = for {
      (Tab(_, crawler, consumers), query) <- crawlers zip getQueries(theSame.isSelected, qt)
    } yield {
      val entries = crawler crawl query
      val frames = Accumulator accumulate entries

      consumers foreach (_ refresh (query, TreeMap.empty))

      val _ = frames.subscribe(
        onNext = data => consumers foreach (_ refresh (query, data)),
        onError = {
          case e: HttpError =>
            showError(e.getMessage + "\n\n" + e.response.getResponseBody)
          case e =>
            showError(e.toString)
        },
        onCompleted = () => ()
      )

      entries
    }

    val allEntries: Observable[List[CrawlerEntry]] = allEntriesList.toObservable.flattenDelayError
    val allFrames = Accumulator accumulate allEntries

    {
      val counter = (allEntries scan 0)(_ + _.length)
      val _ = counter subscribe (n => results setText s"$n")
    }

    totals refresh (qt, TreeMap.empty)

    val _ = allFrames.subscribe(
      onNext = data => totals refresh (qt, data),
      onError = _ => (),
      onCompleted = () => {
        showError("No more articles can be found!")
        controls foreach (_ setEnabled true)
        query requestFocus()
      }
    )
  }

  private def buildToolbar() {
    val al = new ActionListener {
      def actionPerformed(e: ActionEvent) = startCrawling()
    }

    implicit class JToolBarOps(t: JToolBar) {
      def xadd(c: Component) { val _ = t add c } // an `add` that is actually a statement =,=
    }

    query addActionListener al
    go addActionListener al

    val tb = new JToolBar
    tb setFloatable false
    tb setLayout new BoxLayout(tb, BoxLayout.X_AXIS) // linux fix for JTextBox in JToolBar
    add(tb, BorderLayout.PAGE_START)

    tb xadd new JLabel("Query: ")

    tb xadd query

    theSame setSelected true
    tb xadd theSame

    tb addSeparator()

    tb xadd go

    tb addSeparator()

    tb xadd new JLabel("Articles processed: ")

    tb xadd results

    ()
  }

  private def getQueries(same: Boolean, default: String): List[String] = {
    if (same) crawlers map (_ => default)
    else crawlers map { cr =>
      val text = JOptionPane.showInputDialog(this, s"Enter a specialized query for ${cr.name}:",
        cr.name, JOptionPane.QUESTION_MESSAGE, ExplicitNull.Icon, ExplicitNull.Objects, default).toString
      Option(text) match {
        case Some(t) => t
        case _ => default
      }
    }
  }

}
