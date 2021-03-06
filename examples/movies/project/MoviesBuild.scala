/*
 * Copyright 2014 Treode, Inc.
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
 */

import sbtassembly.Plugin.AssemblyKeys
import sbtassembly.Plugin.assemblySettings

import sbt._
import sbtassembly.Plugin._
import AssemblyKeys._
import Keys._

object MoviesBuild extends Build {

  val versionString = "0.2.0-SNAPSHOT"

  val commonSettings = Seq (

      version := versionString,
      scalaVersion := "2.10.4",

      unmanagedSourceDirectories in Compile <<=
        (baseDirectory ((base: File) => Seq (base / "src"))),

      unmanagedSourceDirectories in Test <<=
        (baseDirectory ((base: File) => Seq (base / "test"))),

      resolvers += "Twitter" at "http://maven.twttr.com",

      resolvers += Resolver.url (
        "treode-oss",
        new URL ("https://oss.treode.com/ivy")) (Resolver.ivyStylePatterns))

  // Shared by server and spark projects.
  lazy val common =
    Project ("common", file ("common"))
    .settings (commonSettings: _*)
    .settings (

      name := "movies-common",

      libraryDependencies ++= Seq (
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.2" % "provided"))

  // The respository server.
  lazy val server =
    Project ("server", file ("server"))
    .dependsOn (common)
    .settings (assemblySettings: _*)
    .settings (commonSettings: _*)
    .settings (

      name := "movies-server",

      libraryDependencies ++= Seq (
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.4.2",
        "com.treode" %% "store" % versionString % "compile;test->stub",
        "com.treode" %% "finatra" % versionString,
        "com.treode" %% "twitter-app" % versionString,
        "com.treode" %% "jackson" % versionString),

      jarName in assembly := "movies-server.jar",

      mainClass in assembly := Some ("movies.Main"),

      test in assembly := {}
    )

  // The Spark connector.
  lazy val spark =
    Project ("spark", file ("spark"))
    .dependsOn (common)
    .settings (assemblySettings: _*)
    .settings (commonSettings: _*)
    .settings (

      name := "movies-spark",

      libraryDependencies ++= Seq (
        "org.apache.spark" %% "spark-core" % "1.1.0" % "provided",
        // Use Jackson 2.3.1 because spark-core does.
        "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.3.1",
        "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.3.1"),

      jarName in assembly := "movies-spark.jar",

      test in assembly := {}
    )

  lazy val root =
    Project ("root", file ("."))
    .aggregate (server, spark)
}
