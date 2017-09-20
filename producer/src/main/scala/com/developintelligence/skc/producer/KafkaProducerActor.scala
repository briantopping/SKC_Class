package com.developintelligence.skc.producer

import java.util.Properties

import akka.actor.{Actor, ActorLogging, Props}
import com.developintelligence.skc.common.Codec
import com.developintelligence.skc.common.schema.Tweet
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.{ByteArraySerializer, LongSerializer}

import scala.concurrent.{Future, Promise}
import scala.util.Try

case class TweetJson(tweet: String, topic: String)

class KafkaProducerActor(servers: String) extends Actor with ActorLogging {
  def receive: Receive = {
    case (topic: String, tweet: Tweet) =>
      send(topic, tweet)

    case invalidMessage =>
      log.warning("No handler for this message " + invalidMessage)
  }

  private val props: Properties = new Properties

  props.put("bootstrap.servers", servers)
  props.put("acks", "all")
  props.put("retries", "5")
  props.put("key.serializer", classOf[LongSerializer].getName)
  props.put("value.serializer", classOf[ByteArraySerializer].getName)

  private val kafkaProducer: KafkaProducer[Long, Array[Byte]] = new KafkaProducer[Long, Array[Byte]](props)

  def send(topic: String, tweet: Tweet): Future[RecordMetadata] = {
    val message: ProducerRecord[Long, Array[Byte]] = new ProducerRecord[Long, Array[Byte]](topic, tweet.id.select[Long].get, Codec.serializeMessage(tweet))
    log.info("Sending message to kafka cluster .....")
    val recordMetadataResponse = kafkaProducer.send(message)
    val promise = Promise[RecordMetadata]()

    import context.dispatcher
    Future {
      promise.complete(Try(recordMetadataResponse.get()))
    }
    promise.future
  }

  def close(): Unit = kafkaProducer.close()
}

object KafkaProducerActor {
  def props(servers: String) = Props(new KafkaProducerActor(servers))
}
