package com.developintelligence.skc.consumer

import com.datastax.spark.connector.cql.CassandraConnector
import com.developintelligence.skc.common.schema.Tweet
import org.apache.spark.sql.SparkSession

class CassandraWriter(sparkSession: SparkSession) extends Serializable {
  val connector = CassandraConnector.apply(sparkSession.sparkContext.getConf)

  // Create keyspace and tables here, NOT in prod
  def createKeySpace(): Unit = {
    connector.withSessionDo { session =>
      Statements.createKeySpaceAndTable(session, true)
    }
    ()
  }

  private def processRow(id: Long, value: Array[Byte]): Unit = {
    connector.withSessionDo { session =>
      session.execute(Statements.cql(id, value))
    }
    ()
  }

  //     This Foreach sink writer writes the output to cassandra.
  import org.apache.spark.sql.ForeachWriter

//  val writer = new ForeachWriter[Tweet] {
//    override def open(partitionId: Long, version: Long) = true
//    override def process(value: Tweet): Unit = {
//      processRow(value)
//    }
//    override def close(errorOrNull: Throwable) = {}
//  }
}
