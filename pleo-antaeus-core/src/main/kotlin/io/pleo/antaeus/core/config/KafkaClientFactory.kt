package io.pleo.antaeus.core.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*


class KafkaClientFactory {

    companion object {
        private const val BOOTSTRAP_SERVERS: String = "localhost:9092"
        private const val GROUP_ID = "antaeus_consumer"
        const val KAFKA_TOPIC = "pending_invoices"
    }

    fun createProducer(): Producer<String, String> {
        val config = Properties()
        config["bootstrap.servers"] = BOOTSTRAP_SERVERS
        config["key.serializer"] = StringSerializer::class.java
        config["value.serializer"] = StringSerializer::class.java
        return KafkaProducer<String, String>(config)
    }

    fun createConsumer(): Consumer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = BOOTSTRAP_SERVERS
        props["key.deserializer"] = StringDeserializer::class.java
        props["value.deserializer"] = StringDeserializer::class.java
        props["group.id"] = GROUP_ID
        props["auto.offset.reset"] = "earliest"
        props["max.poll.records"] = 50
        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(KAFKA_TOPIC))
        return consumer
    }

}