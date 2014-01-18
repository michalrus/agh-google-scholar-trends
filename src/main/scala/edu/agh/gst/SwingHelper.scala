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

package edu.agh.gst

import javax.swing.SwingUtilities

trait SwingHelper {

  import scala.language.implicitConversions
  implicit def quasiblockToRunnable(f: () => Unit) = new Runnable {
    def run() = f()
  }

  def laterOnUiThread(r: Runnable) = SwingUtilities invokeLater r

  def delayOnUiThread(ms: Long)(r: Runnable) {
    import concurrent._
    import ExecutionContext.Implicits.global

    val _ = future { blocking {
      Thread sleep ms
      SwingUtilities invokeLater r
    }}
  }

}
