package edu.agh.gst

import javax.swing.SwingUtilities

trait SwingHelper {

  implicit def blockToRunnable[A](f: => A) = new Runnable {
    def run() = f
  }

  def laterOnUiThread(r: Runnable) = delayOnUiThread(0)(r)

  def delayOnUiThread(ms: Long)(r: Runnable) {
    import concurrent._
    import ExecutionContext.Implicits.global

    future {
      Thread sleep ms
      SwingUtilities invokeLater r
    }
  }

}
