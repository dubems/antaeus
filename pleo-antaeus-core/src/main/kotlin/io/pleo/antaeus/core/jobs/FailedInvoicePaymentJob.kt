package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.external.acquireLock
import io.pleo.antaeus.core.external.releaseLock
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoicePaymentService
import mu.KotlinLogging
import java.util.TimerTask

private val logger = KotlinLogging.logger { }

class FailedInvoicePaymentJob(
    private val invoicePaymentService: InvoicePaymentService,
    private val billingService: BillingService,
) : TimerTask() {

    private val processName = "FailedInvoicePayment"
    private fun execute() {
        try {
            acquireLock(processName)
            val retryableInvoices = invoicePaymentService.fetchRetryableInvoicePayments()
            logger.info("process=retryFailedInvoicePayment, count={}", retryableInvoices.size)
            retryableInvoices.forEach {
                billingService.billInvoice(it)
            }
        } finally {
            releaseLock(processName)
        }
    }

    override fun run() {
        execute()
    }
}