name := """recommend"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.6"

Common.settings

//playEbeanModels in Compile := Seq("ua.dirproy.profelumno.user.models.*")

libraryDependencies ++= Common.dependencies
libraryDependencies += "joda-time" % "joda-time" % "2.9"
