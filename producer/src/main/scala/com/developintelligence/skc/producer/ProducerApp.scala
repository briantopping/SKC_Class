package com.developintelligence.skc.producer

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration._


object ProducerApp extends App with StrictLogging {
  private implicit val timeout = Timeout(10.seconds)

  // these three lines set up an ActorSystem (kind of like a Spring Context for actors) and create the "supervisor",
  // a common (but not required) pattern for Akka applications. The import just brings the dispatcher into scope.
  // http://doc.akka.io/docs/akka/current/scala/actors.html#creating-actors
  val actorSystem = ActorSystem()
  import actorSystem.dispatcher
  val supervisor = actorSystem.actorOf(Supervisor.props(), "supervisor")

  // these two are what we are going to initialize our stream handler with
  val topic = "test3"
  val searchKeys = Seq("startbucks", "sbux", "startbuck",
    "coffestartbuck", "coffee", "StarbucksUK", "StarbucksCanada", "StarbucksMY",
    "StarbucksIndia", "StarbucksIE", "StarbucksAu", "StarbucksFrance", "StarbucksMex", "StarBucksTweet")

  // this construction is a "for comprehension". It can be thought of a kind of as kind of an "inline foreach",
  // but that's a *very* loose analogy. The first and third lines are called "generators", the second line is a "filter".
  // note that any failure exits the for comprehension
  // https://docs.scala-lang.org/tour/sequence-comprehensions.html
  val streamHandlerActor = for {
    kafkaProducerActor <- (supervisor ? ((KafkaProducerActor.props("127.0.0.1:9092"), "kafkaProducerActor"))).mapTo[ActorRef]
    streamHandlerArgs = (StreamHandlerActor.props(kafkaProducerActor, topic, searchKeys), "streamHandler")
    streamHandlerActor <- (supervisor ? streamHandlerArgs).mapTo[ActorRef]
  } yield streamHandlerActor

  // by this point, our stream actor is running, injected with the kafkaProducerActor. We have isolated the connection
  // and search data to this file. It would be simple to extract a method so multiple parallel searches could be running.

  // simple check to see if there are any failures
  streamHandlerActor.onFailure {
    case ex =>
      logger.error("Error in actor initialization ", ex)
      System.exit(0)
  }
}
