package com.developintelligence.skc.producer

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props, Terminated}

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class Supervisor extends Actor with ActorLogging {
  val maxNrOfRetries = 10

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries, withinTimeRange = 1 second) {
      case ex: Exception =>
        log.error("Actor Instance is dead and Recreating ", ex)
        Resume
    }

  def receive: PartialFunction[Any, Unit] = {
    case (props: Props, name: String) =>
      log.info("creating child an actor...")
      val child = context.actorOf(props, name)
      context.watch(child)
      sender ! child

    case Terminated(child) =>
      log.error(s"[$child] is dead")

    case invalidMessage =>
      log.warning("No handler for this message " + invalidMessage)
  }
}

object Supervisor {
  def props() = Props(new Supervisor())
}