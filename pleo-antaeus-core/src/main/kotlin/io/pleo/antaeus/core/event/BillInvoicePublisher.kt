package io.pleo.antaeus.core.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.pleo.antaeus.core.config.KafkaClientFactory
import io.pleo.antaeus.models.Invoice
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class BillInvoicePublisher(
    private val objectMapper: ObjectMapper,
    private val producer: Producer<String, String>
) {

    fun publish(invoice: Invoice) {
        producer.send(ProducerRecord(KafkaClientFactory.KAFKA_TOPIC, invoice.id.toString(),objectMapper.writeValueAsString(invoice)))
    }
}