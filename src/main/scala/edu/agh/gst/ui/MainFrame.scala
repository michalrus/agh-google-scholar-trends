package edu.agh.gst.ui

import javax.swing._
import edu.agh.gst.SwingHelper
import scala.util.Try
import java.awt.{BorderLayout, Dimension}
import java.awt.event.{ActionEvent, ActionListener}
import edu.agh.gst.crawler._
import edu.agh.gst.crawler.HttpError
import scala.util.Failure
import scala.util.Success
import edu.agh.gst.crawler.CrawlerEntry

class MainFrame extends JFrame with SwingHelper {

  private val query = new JTextField
  private val results = new JLabel("0")

  case class Tab(name: String, crawler: Crawler, chart: Chart)

  private val crawlers = Tab("Google Scholar", new GoogleScholarCrawler(showCaptcha), new Chart) ::
    Tab("Microsoft Something", new MicrosoftCrawler, new Chart) ::
    Nil

  private val total = new Chart

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
    buildCharts()
  }

  private def buildCharts() {
    val tabs = new JTabbedPane
    add(tabs, BorderLayout.CENTER)

    crawlers foreach { c =>
      tabs addTab(c.name, c.chart)
    }

    tabs addTab("Total", total)
  }

  private def showError(s: String) =
    JOptionPane showMessageDialog(this, s, "Error", JOptionPane.WARNING_MESSAGE)

  import concurrent._

  private def showCaptcha(img: String) = {
    val p = Promise[String]()
    p.future
  }

  private def onCrawled(years: Try[List[CrawlerEntry]], crawler: Tab) = laterOnUiThread {
    years match {
      case Success(es) =>
        numProcessed += es.length
        crawler.chart addEntries es
        total addEntries es
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
        crawlers foreach { c =>
          c.chart setTitle q
          (c.crawler crawl q)(onCrawled(_, c))
        }
        total setTitle q
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
