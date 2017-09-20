package com.developintelligence.skc.consumer

import com.developintelligence.skc.common.Codec
import com.developintelligence.skc.common.schema.Tweet
import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.sql.SparkSession

object ConsumerApp extends App with StrictLogging {
  val app = new ConsumerApp()
  try {
    app.runJob()
  } catch {
    case ex: Exception =>
      logger.error("*****UNCAUGHT EXCEPTION", ex)
  }
}

class ConsumerApp extends StrictLogging {
  val sparkSession: SparkSession =
    SparkSession.builder
      .master("local[2]")
      .appName("kafka2Spark2Cassandra")
      .config("spark.cassandra.connection.host", "localhost")
      .config("spark.driver.bindAddress", "127.0.0.1")

      .getOrCreate()

  // Check this class thoroughly, it does some initializations which shouldn't be in PRODUCTION
  // WARNING: go through this class properly.
  @transient val cassWriter = new CassandraWriter(sparkSession)

  def runJob(): Unit = {

    logger.info("Execution started with following configuration")

    import sparkSession.implicits._

    val lines = sparkSession.readStream
      .format("kafka")
      .option("subscribe", "tweet_queue")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("startingOffsets", "latest")
      .load()
    //      .selectExpr("value",
    //                  "CAST(topic as STRING)",
    //                  "CAST(partition as INTEGER)")

    lines.printSchema()

    //    val df = lines
    //      .select($"value")
    //      .withColumn("deserialized", Codec.deserializeMessage($"value"))
    //      .select($"deserialized")
    //
    //    df.printSchema()
    //
    //    val ds = df
    //      .select($"deserialized.user_id",
    //              $"deserialized.time",
    //              $"deserialized.event")
    //      .as[Tweet]
    //
    //    val query =
    //      ds.writeStream
    //        .queryName("kafka2Spark2Cassandra")
    //        .foreach(cassWriter.writer)
    //        .start
    //
    //    query.awaitTermination()
    sparkSession.stop()
  }
}
