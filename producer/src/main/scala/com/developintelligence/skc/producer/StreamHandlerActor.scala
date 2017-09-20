package com.developintelligence.skc.producer

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.developintelligence.skc.common.schema.Tweet
import com.typesafe.scalalogging.StrictLogging
import shapeless.Coproduct
import twitter4j._

class StreamHandlerActor(kafkaProducer: ActorRef, topic: String, searchKeys: Seq[String]) extends Actor with ActorLogging {
  def receive: Receive = {
    case tweet: Tweet =>
      kafkaProducer ! (topic, tweet)

    case invalidMessage =>
      log.warning("No handler for this message " + invalidMessage)
  }

  override def preStart(): Unit = {
    val twitterStream: TwitterStream = new TwitterStreamFactory().getInstance
    twitterStream.addListener(new TwitterStatusListener(self))
    twitterStream.sample("en")
    twitterStream.filter(searchKeys: _*)
  }
}

object StreamHandlerActor {
  def props(kafkaProducer: ActorRef, topic: String, searchKeys: Seq[String]): Props = {
    Props(new StreamHandlerActor(kafkaProducer, topic, searchKeys))
  }
}

// This class can be used to listen to any Status. Upon getting an onStatus message, send the status to
// the specified actor.
class TwitterStatusListener(streamHandler: ActorRef) extends StatusListener with StrictLogging {
  val EMPTY_STRING = ""

  def onStatus(status: Status) {
    logger.info("Got tweet from user " + Option(status.getUser).fold("Unknown")(_.getName))
    streamHandler ! getTweet(status)
  }

  def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
    logger.info("Got a status deletion notice id:" + statusDeletionNotice.getStatusId)
  }

  def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
    logger.info("Got track limitation notice:" + numberOfLimitedStatuses)
  }

  def onScrubGeo(userId: Long, upToStatusId: Long) {
    logger.info("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId)
  }

  def onStallWarning(warning: StallWarning) {
    logger.info("Got stall warning:" + warning)
  }

  def onException(ex: Exception) {
    logger.error("Error in getting tweet", ex)
  }

  // Stinky Code Alert: I made a few design choices here. Please see the README!! :D
  private def getTweet(status: Status): Tweet = {
    Tweet(
      Coproduct(status.getId),
      Coproduct(status.getCreatedAt.toString),
      Coproduct(Option(status.getUser).fold(EMPTY_STRING)(_.getScreenName)),
      Coproduct(status.getText.replaceAll("\n", " ")),
      Coproduct(status.getRetweetCount)
    )
  }
}


