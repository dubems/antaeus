package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.event.EventPublisher
import io.pleo.antaeus.core.external.acquireLock
import io.pleo.antaeus.core.external.releaseLock
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.core.utils.isFirstOfMonth
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.util.*

val log = KotlinLogging.logger { }

class BillInvoiceJob(
    private val invoiceService: InvoiceService,
    private val eventPublisher: EventPublisher<Invoice>,
) : TimerTask() {

    private val processName = "BillInvoice"
    fun execute(localTesting: Boolean) {
        try {
            acquireLock(processName)

            if (!isFirstOfMonth() && !localTesting) {
                log.info("process=BillInvoice, status=notExecuted, message=Current day is not first of month")
                return
            }
            //this can also be done in batches
            val invoices = invoiceService.fetchInvoicesForBilling()
            invoices.forEach { eventPublisher.publish(it) }

            log.info(
                "process=BillInvoice, status=success, message= ${invoices.size} pending-invoices event have been published"
            )
            invoiceService.updateInvoicesStatus(invoices, InvoiceStatus.PROCESSING)
        } finally {
            releaseLock(processName)
        }
    }

    override fun run() {
        execute(false)
    }

}