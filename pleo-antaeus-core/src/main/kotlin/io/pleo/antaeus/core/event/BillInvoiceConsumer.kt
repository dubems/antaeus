package io.pleo.antaeus.core.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.pleo.antaeus.core.config.KafkaClientFactory
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import java.time.Duration

val logger = KotlinLogging.logger { }

class BillInvoiceConsumer(
    private val objectMapper: ObjectMapper,
    private val consumer: Consumer<String, String>,
    private val billingService: BillingService
) : Runnable {

    private fun consume() {
        while (true) {
            logger.info("Starting message consumption...")
            val records = consumer.poll(Duration.ofSeconds(3))
            logger.info("Polled ${records.count()} records")
            records.forEach {
                val message = objectMapper.readValue<Invoice>(it.value(),Invoice::class.java)
                logger.info("message amount is ${message.amount}")
            }
        }
    }

    override fun run() {
        consume()
    }
}