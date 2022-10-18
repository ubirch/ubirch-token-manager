/**
  * Copyright 2018 ubirch GmbH
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

ThisBuild / organization := "com.ubirch"
ThisBuild / organizationName := "ubirch GmbH"
ThisBuild / description := "Light library to integrate Token Verification"
developers := List(
  Developer("carlos", "Carlos Sanchez", "carlos.sanchezi@ubirch.com", url("https://www.ubirch.com/"))
)
licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

lazy val scala212 = "2.12.16"
lazy val scala213 = "2.13.10"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / version := "2.2.0-SNAPSHOT"
ThisBuild / scalaVersion := scala213

val HttpClientVersion = "4.5.13"
val UbirchCryptoVersion = "2.1.2"
val CommonsValidatorVersion = "1.7"
val BouncyCastleVersion = "1.70"
val MonixVersion = "3.1.0"
val GuiceVersion = "4.1.0"
val Json4sVersion = "4.0.6"
val TypesafeConfigVersion = "1.3.4"
val Slf4jApiVersion = "1.7.15"
val JclOverSlf4jVersion = "1.7.25"
val LogbackClassicVersion = "1.2.3"
val ScalaLoggingVersion = "3.9.2"
val LogstashLogbackEncoderVersion = "7.2"
val JwtCoreVersion = "4.2.0"
val ScalatestVersion = "3.1.0"

val defaultScalacOptions = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Explain type errors in more detail.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xfuture", //  Turn on future language features.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification", // Enable partial unification in type constructor inference
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard" // Warn when non-Unit expression results are unused.
)

lazy val excludedScalaCOption213 = Set("-Xlint:by-name-right-associative", "-Xlint:nullary-override", "-Xlint:unsound-match", "-Yno-adapted-args", "-Ypartial-unification", "-Ywarn-inaccessible", "-Ywarn-infer-any", "-Ywarn-nullary-override", "-Ywarn-nullary-unit", "-Xfuture")

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % HttpClientVersion,
  "com.ubirch" % "ubirch-crypto" % UbirchCryptoVersion,
  "commons-validator" % "commons-validator" % CommonsValidatorVersion,
  "org.bouncycastle" % "bcprov-jdk15on" % BouncyCastleVersion,
  "org.bouncycastle" % "bcpkix-jdk15on" % BouncyCastleVersion,
  "io.monix" %% "monix" % MonixVersion,
  "com.google.inject" % "guice" % GuiceVersion,
  "org.json4s" %% "json4s-native" % Json4sVersion,
  "org.json4s" %% "json4s-jackson" % Json4sVersion,
  "org.json4s" %% "json4s-ext" % Json4sVersion,
  "com.typesafe" % "config" % TypesafeConfigVersion,
  "org.slf4j" % "slf4j-api" % Slf4jApiVersion,
  "org.slf4j" % "jcl-over-slf4j" % JclOverSlf4jVersion,
  "ch.qos.logback" % "logback-classic" % LogbackClassicVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
  "net.logstash.logback" % "logstash-logback-encoder" % LogstashLogbackEncoderVersion,
  "com.pauldijou" %% "jwt-core" % JwtCoreVersion,
  "org.scalatest" %% "scalatest" % ScalatestVersion % Test
)

lazy val root = (project in file("."))
  .settings(
    name := "ubirch-token-sdk",
    crossScalaVersions := supportedScalaVersions,
    scalacOptions := {
      scalaBinaryVersion.value match {
        case "2.12" => defaultScalacOptions
        case _      => defaultScalacOptions filterNot excludedScalaCOption213
      }
    },
    publishTo :=
        Some("gitlab-maven" at "https://gitlab.com/api/v4/projects/37429227/packages/maven"),
    scalafmtOnCompile := true
  )

credentials += Credentials(Path.userHome / ".sbt" / ".credentials.gitlab")
