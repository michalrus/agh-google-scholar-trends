package edu.agh.gst.ui

import javax.swing._
import edu.agh.gst.SwingHelper
import scala.util.{Failure, Try, Success}
import java.awt.{BorderLayout, Dimension}
import java.awt.event.{ActionEvent, ActionListener}
import edu.agh.gst.crawler.{CrawlerEntry, HttpError, GoogleScholarCrawler}

class MainFrame extends JFrame with SwingHelper {

  private val query = new JTextField
  private val results = new JLabel("0")
  private val gsCrawler = new GoogleScholarCrawler
  private val chart = new Chart

  laterOnUiThread {
    Try(UIManager setLookAndFeel UIManager.getSystemLookAndFeelClassName)

    setTitle("agh-google-scholar-trends")
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    setSize(new Dimension(800, 600))
    setMinimumSize(new Dimension(400, 300))
    setLocationRelativeTo(null)
    setVisible(true)

    buildUi()
  }

  private def buildUi() {
    buildToolbar()
    add(chart, BorderLayout.CENTER)
  }

  private def showError(s: String) =
    JOptionPane showMessageDialog(this, s, "Error", JOptionPane.WARNING_MESSAGE)

  private def onCrawled(years: Try[List[CrawlerEntry]]) {
    years match {
      case Success(es) =>
        numProcessed += es.length
        chart addEntries es
      case Failure(e: HttpError) =>
        showError(e.getMessage + "\n\n" + e.response.getResponseBody)
      case Failure(e) =>
        showError(e.toString)
    }
  }

  private def buildToolbar() {
    val tb = new JToolBar
    tb setFloatable false
    add(tb, BorderLayout.PAGE_START)

    tb add new JLabel("Query: ")

    tb add query

    val go = new JButton("Go!")
    go addActionListener new ActionListener {
      def actionPerformed(e: ActionEvent) = {
        (go :: query :: Nil) foreach (_ setEnabled false)
        numProcessed = 0
        val q = query.getText
        chart setTitle q
        (gsCrawler crawl q)(onCrawled)
      }
    }
    tb add go

    tb addSeparator()

    tb add new JLabel("Articles processed: ")

    tb add results
  }

  private var _numProcessed = 0
  private def numProcessed = _numProcessed
  private def numProcessed_=(v: Int) = {
    _numProcessed = v
    results setText v.toString
  }

}
