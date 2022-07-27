package io.pleo.antaeus.core.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.pleo.antaeus.core.config.KafkaClientFactory
import io.pleo.antaeus.models.Invoice
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

interface EventPublisher<in T> {
    fun publish(payload: T)
}

class BillInvoicePublisher(
    private val objectMapper: ObjectMapper,
    private val producer: Producer<String, String>
) : EventPublisher<Invoice> {

    override fun publish(payload: Invoice) {
        producer.send(
            ProducerRecord(
                KafkaClientFactory.KAFKA_TOPIC, payload.id.toString(), objectMapper.writeValueAsString(payload)
            )
        )
    }

}