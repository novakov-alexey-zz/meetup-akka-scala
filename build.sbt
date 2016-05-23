import sbt.Keys._

name := "meetup-akka-scala"

version := "1.0"

scalaVersion := "2.11.8"

autoScalaLibrary := false

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test",
  "com.typesafe.akka" %% "akka-actor" % "2.4.5",
  "com.typesafe.akka" %% "akka-persistence" % "2.4.5",
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "mysql" % "mysql-connector-java" % "5.1.16",
  "org.mybatis.scala" % "mybatis-scala-core_2.11" % "1.0.3",
  "com.typesafe.akka" %% "akka-testkit"  % "2.4.5"% "test",
  "com.google.inject" % "guice" % "4.0"
)