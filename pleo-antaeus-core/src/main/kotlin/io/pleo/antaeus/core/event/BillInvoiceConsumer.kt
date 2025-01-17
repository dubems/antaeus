package io.pleo.antaeus.core.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.time.Duration

interface EventConsumer {
    fun consume()
}

private const val POLL_TIMEOUT: Long = 3L

class BillInvoiceConsumer(
    private val objectMapper: ObjectMapper,
    private val consumer: Consumer<String, String>,
    private val billingService: BillingService,
    private val invoiceService: InvoiceService,
) : EventConsumer {

    private val log = KotlinLogging.logger { }

    override fun consume() {
        log.info("Starting message consumption...")
        val records: ConsumerRecords<String, String> = consumer.poll(Duration.ofSeconds(POLL_TIMEOUT))
        log.info("Polled ${records.count()} records")
        records.forEach {
            var invoice = objectMapper.readValue(it.value(), Invoice::class.java)
            //refresh the invoice from the DB
            invoice = invoiceService.fetch(invoice.id)
            billingService.billInvoice(invoice)
        }
    }
}