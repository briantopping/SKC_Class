package com.developintelligence.skc.common

import java.io.ByteArrayOutputStream

import com.developintelligence.skc.common.schema.Tweet
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream}

object Codec {
  def deserializeMessage(bytes: Array[Byte]): Tweet = {
    try {
      val input = AvroInputStream.data[Tweet](bytes)
      val result = input.iterator().next
      input.close()
      result
    } catch {
      case e: Exception => null
    }
  }
  def serializeMessage(tweet: Tweet): Array[Byte] = {
    try {
      val baos = new ByteArrayOutputStream()
      val os = AvroOutputStream.data[Tweet](baos)
      os.write(tweet)
      os.flush()
      val result = baos.toByteArray
      baos.close()
      result
    } catch {
      case e: Exception => null
    }
  }
}
