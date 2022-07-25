package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.event.BillInvoicePublisher
import io.pleo.antaeus.core.services.InvoiceService
import mu.KotlinLogging

val logger = KotlinLogging.logger { }

class BillInvoiceJob(
    private val invoiceService: InvoiceService,
    private val billInvoicePublisher: BillInvoicePublisher
) {

    //todo: run this on the first of the month at 12pm because of timezone(s), to process :
    //todo unbilled invoice, failed invoice
    //todo: this should run on only one instance
    fun execute() {
        //execute the job every day, only publish at the first of the month
        invoiceService.fetchAll().forEach {
            billInvoicePublisher.publish(it)
            logger.info("Published message for invoice ${it.id} and status ${it.status}")
            // update that it has been published
        }
    }

}