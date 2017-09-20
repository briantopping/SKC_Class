import sbt.ExclusionRule
import sbt.Keys.{javaOptions, mainClass, scalaVersion, version}
import sbtassembly.AssemblyKeys

val sparkV = "2.2.0"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-target:jvm-1.8",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Xlint",
  "-Ydelambdafy:method",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused-import",
  "-Ywarn-value-discard"
)

val consumerDependencies = {
  val cassandraV = "2.0.0-M3"
  Seq(
    "org.apache.spark" %% "spark-core" % sparkV,
    "org.apache.spark" %% "spark-streaming" % sparkV,
    "org.apache.spark" %% "spark-streaming-kafka-0-10" % sparkV,
    "org.apache.spark" %% "spark-sql-kafka-0-10" % sparkV,
    "org.apache.spark" %% "spark-hive" % sparkV,
    "com.datastax.spark" %% "spark-cassandra-connector" % cassandraV,
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "com.holdenkarau" %% "spark-testing-base" % "2.0.0_0.4.4" % "test",
    "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.2"
//    "com.google.guava" % "guava" % "19.0"
  ).map(d => d excludeAll ExclusionRule("org.slf4j", "slf4j-log4j12"))
}

val producerDependencies = {
  val akkaVersion = "2.5.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "org.twitter4j" % "twitter4j-stream" % "4.0.4",
    "org.apache.kafka" % "kafka-clients" % "0.10.0.1"
  )
}

val commonDependencies = {
  val avroV = "1.8.1"
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0",
    "org.apache.logging.log4j" % "log4j-api" % "2.9.0",
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.9.0",
    "org.slf4j" % "slf4j-ext" % "1.7.25",
    "org.apache.avro" % "avro" % avroV,
    "org.apache.spark" %% "spark-sql" % sparkV,
    "com.sksamuel.avro4s" %% "avro4s-core" % "1.8.0"
  ).map(d => d excludeAll ExclusionRule("org.slf4j", "slf4j-log4j12"))
}

val masterSettings = Seq(
  organization := "com.developintelligence",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.wartremover" %% "wartremover" % "2.1.1"),
  parallelExecution in Test := false,
  javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled"),
  publish := publish dependsOn AssemblyKeys.assembly,
  assemblyMergeStrategy in assembly := {
    case m if m.toLowerCase.endsWith("manifest.mf")          => MergeStrategy.discard
    case m if m.toLowerCase.matches("meta-inf.*\\.sf$")      => MergeStrategy.discard
    case m if m.toLowerCase.startsWith("meta-inf/maven")     => MergeStrategy.discard
    case "log4j.properties"                                  => MergeStrategy.discard
    case m if m.toLowerCase.startsWith("meta-inf/services/") => MergeStrategy.filterDistinctLines
    case "reference.conf"                                    => MergeStrategy.concat
    case _                                                   => MergeStrategy.first
  }
)

val common = (project in file("common")).settings(
  masterSettings,
  libraryDependencies ++= commonDependencies
)

val consumer = (project in file("consumer"))
  .enablePlugins(AssemblyPlugin)
  .dependsOn(common)
  .settings(
    name := "kafka-consumer",
    masterSettings,
    libraryDependencies ++= consumerDependencies,
    mainClass in assembly := Some("com.developintelligence.skc.consumer.ConsumerApp")
  )

val producer = (project in file("producer"))
  .enablePlugins(AssemblyPlugin)
  .dependsOn(common)
  .settings(
    name := "kafka-producer",
    masterSettings,
    libraryDependencies ++= producerDependencies,
    mainClass in assembly := Some("com.developintelligence.skc.producer.ProducerApp")
  )

val root = (project in file(".")).aggregate(consumer, producer, common)
  .settings(
    scalaVersion := "2.11.8",
    aggregate in publishArtifact := false
  )
