package com.developintelligence.skc.consumer

import java.util

import com.developintelligence.skc.common.Codec
import com.developintelligence.skc.common.schema.Tweet
import com.typesafe.scalalogging.StrictLogging
import org.apache.kafka.common.serialization.{Deserializer, LongDeserializer}
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.{KafkaUtils, LocationStrategies}
import org.apache.spark.streaming.{Seconds, StreamingContext}

object ConsumerApp extends App with StrictLogging {
  val app = new ConsumerApp()
  try {
    app.runJob()
  } catch {
    case ex: Exception =>
      logger.error("*****UNCAUGHT EXCEPTION", ex)
  }
}

class ConsumerApp extends StrictLogging with Serializable {
  val sparkSession: SparkSession =
    SparkSession.builder
      .master("local[2]")
      .appName("kafka2Spark2Cassandra")
      .config("spark.cassandra.connection.host", "localhost")
      .config("spark.driver.bindAddress", "127.0.0.1")
      .getOrCreate()

  @transient val cassWriter = new CassandraWriter(sparkSession)

  def runJob(): Unit = {
    val kafkaParams = Map[String, Object](
      "key.deserializer" -> classOf[LongDeserializer],
      "value.deserializer" -> classOf[TweetDeserializer],
      "group.id" -> "use_a_separate_group_id_for_each_stream",
      "auto.offset.reset" -> "latest",
      "bootstrap.servers" -> "localhost:9092"
    )

    val ssc = new StreamingContext(sparkSession.sparkContext, Seconds(2))
    val dStream =
      KafkaUtils.createDirectStream[Long, Tweet](ssc, LocationStrategies.PreferConsistent, Subscribe[Long, Tweet](Set("test3"), kafkaParams))

    // An RDD is created at each interval specified in the instantiation of the StreamingContext
    // Can we do anything more interesting with it than just print it out?
    dStream.foreachRDD { rdd =>
      rdd.foreach { msg =>
        println(msg)
      }
    }
    ssc.start()
    ssc.awaitTermination()
  }
}

class TweetDeserializer extends Deserializer[Tweet] {
  override def configure(configs: util.Map[String, _], isKey: Boolean): Unit = {} // nothing to do
  override def deserialize(topic: String, data: Array[Byte]): Tweet = Codec.deserializeMessage(data)
  override def close(): Unit = {} // nothing to do
}