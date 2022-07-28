package io.pleo.antaeus.core.config

import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*


class KafkaClientFactory(private val bootstrapServers: String) {

    companion object {
        private const val GROUP_ID = "antaeus-consumer-group"
        const val KAFKA_TOPIC = "pending_invoices"
        private const val MAX_POLL_RECORDS = 50
        private const val MAX_SEND_RETRIES = 10000000
        private const val AUTO_OFFSET_RESET = "earliest"
    }

    fun createProducer(): Producer<String, String> {
        val config = Properties()
        config["bootstrap.servers"] = bootstrapServers
        config["message.send.max.retries"] = MAX_SEND_RETRIES
        config["enable.idempotence"] = true //important to enable idempotent production of message into Kafka
        config["key.serializer"] = StringSerializer::class.java
        config["value.serializer"] = StringSerializer::class.java
        return KafkaProducer(config)
    }

    fun createConsumer(): Consumer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = bootstrapServers
        props["key.deserializer"] = StringDeserializer::class.java
        props["value.deserializer"] = StringDeserializer::class.java
        props["group.id"] = GROUP_ID
        props["auto.offset.reset"] = AUTO_OFFSET_RESET
        props["max.poll.records"] = MAX_POLL_RECORDS
        val consumer = KafkaConsumer<String, String>(props)
        consumer.subscribe(listOf(KAFKA_TOPIC))
        return consumer
    }

}