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
ThisBuild / developers := List(
  Developer("carlos", "Carlos Sanchez", "carlos.sanchezi@ubirch.com", url("https://www.ubirch.com/"))
)
/** @see [[https://www.scala-sbt.org/1.x/docs/Publishing.html#Version+scheme]] */
ThisBuild / versionScheme := Some("semver-spec")
licenses := Seq("Apache 2" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

lazy val scala212 = "2.12.16"
lazy val scala213 = "2.13.10"
lazy val supportedScalaVersions = List(scala213, scala212)

ThisBuild / version := "2.2.0"
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
    // push pom
    publishMavenStyle := true,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials.gitlab"),
    GitlabPlugin.autoImport.gitlabProjectId := Some(37429227),
    scalafmtOnCompile := true
  )
