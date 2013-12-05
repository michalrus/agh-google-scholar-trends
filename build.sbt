import AssemblyKeys._ // put this at the top of the file

assemblySettings

name := "agh-google-scholar-trends"

version := "1.0"

scalaVersion := "2.10.3"

scalacOptions in Compile ++= Seq("-feature", "-deprecation", "-Yno-adapted-args", "-Ywarn-all", "-Xfatal-warnings",
  "-Xlint", "-Ywarn-value-discard", "-Ywarn-numeric-widen", "-Ywarn-dead-code", "-unchecked")

libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.0"

libraryDependencies += "org.jfree" % "jfreechart" % "1.0.15"

libraryDependencies += "net.liftweb" %% "lift-util" % "2.5.1"
