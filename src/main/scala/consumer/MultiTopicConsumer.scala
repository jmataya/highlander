package consumer

import java.util.Properties
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.language.postfixOps

/**
 * Consumer using Kafka's new 0.9.0.0 consumer API
 */
class MultiTopicConsumer(
  topics: Seq[String], 
  groupId: String, 
  broker: String, 
  processor: MessageProcessor,
  startFromBeginning: Boolean = false,
  timeout: Long = 100)(implicit ec: ExecutionContext) {

  val props = new Properties()
  props.put("bootstrap.servers", broker)
  props.put("group.id", groupId)
  props.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer")
  props.put("enable.auto.commit", "false") //don't commit offset automatically


  val consumer = new KafkaConsumer[Array[Byte], Array[Byte]](props)
  consumer.subscribe(topics.toList)
  println(s"Subscribed to topics: ${consumer.subscription}")

  if(startFromBeginning) {
    println(s"Consuming from beggining")
    consumer.assignment.foreach{ a ⇒ consumer.seekToBeginning(a)}
  }

  def readForever(): Unit = {
    while (true) {
      val records = consumer.poll(timeout)

      records.map { r ⇒ 
        Console.err.println(s"Processing ${r.topic} offset ${r.offset}")
        val f = processor.process(r.offset, r.topic, r.value)
        f onSuccess { 
          case result ⇒ Console.err.println(s"Processed ${r.topic} offset ${r.offset}")
        }
        Await.result(f, 60 seconds)
        consumer.commitSync()
      }
    }
  }
}
