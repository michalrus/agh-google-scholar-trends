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

package edu.agh.gst.consumer

import scala.reflect.io.{File, Directory}

object CsvExporter {

  val NonPathCharacter = """[^A-Za-z0-9_\-.]""".r
  val Extension = ".csv"

  def cleanUp(title: String) =
    File(NonPathCharacter replaceAllIn (title, "_"))

  def csvFrom(source: Accumulator): String = {
    val d = source.data map {
      case (year, YearData(articles, citations)) => s"$year,$articles,$citations"
    } mkString "\n"

    "Year,Articles,Citations\n" + d + "\n"
  }

}

class CsvExporter(directory: Option[Directory], engine: String) extends Consumer {
  import CsvExporter._

  def refresh(title: String, source: Accumulator) = directory foreach { directory =>
    val file = directory / cleanUp(title) addExtension (engine + Extension)

    file writeAll csvFrom(source)
  }

}
