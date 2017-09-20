package com.developintelligence.skc.common

import java.io.{ByteArrayOutputStream, OutputStream}

import com.developintelligence.skc.common.schema.Tweet
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream}

object Codec {
  def deserializeMessage(bytes: Array[Byte]): Tweet = {
    try {
      val input = AvroInputStream.data[Tweet](bytes)
      input.iterator().next
    } catch {
      case e: Exception => null
    }
  }
  def serializeMessage(tweet: Tweet): Array[Byte] = {
    try {
      val baos = new ByteArrayOutputStream()
      val os = AvroOutputStream.data[Tweet](baos)
      os.write(tweet)
      baos.toByteArray
    } catch {
      case e: Exception => null
    }
  }
}
