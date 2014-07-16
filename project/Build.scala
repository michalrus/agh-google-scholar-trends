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

import sbt._
import Keys._

import wartremover.WartRemover.autoImport._

object Build extends Build {

  lazy val root = Project(id = "root", base = file(".")).settings(

    name := "agh-google-scholar-trends",
    version := "1.0",
    scalaVersion := "2.10.4",

    javacOptions in Compile ++= Seq("-Xlint:deprecation"),
    scalacOptions in Compile ++= Seq("-feature", "-deprecation", "-Yno-adapted-args", "-Ywarn-all", "-Xfatal-warnings",
      "-Xlint", "-Ywarn-value-discard", "-Ywarn-numeric-widen", "-Ywarn-dead-code", "-unchecked"),

    wartremoverErrors ++= Warts.allBut(Wart.NoNeedForMonad, Wart.Nothing, Wart.AsInstanceOf, Wart.JavaConversions),

    libraryDependencies ++= Seq(
      "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
      "org.jfree" % "jfreechart" % "1.0.15",
      "net.liftweb" %% "lift-util" % "2.5.1",
      "com.netflix.rxjava" % "rxjava-scala" % "0.16.1"
    )

  )

}
