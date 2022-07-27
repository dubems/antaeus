package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoicePaymentService
import mu.KotlinLogging
import java.util.TimerTask

private val logger = KotlinLogging.logger { }

class FailedInvoicePaymentJob(
    private val invoicePaymentService: InvoicePaymentService,
    private val billingService: BillingService
) : TimerTask() {

    private fun execute() {
        val retryableInvoices = invoicePaymentService.fetchRetryableInvoicePayments()
        logger.info("process=retryFailedInvoicePayment, count={}", retryableInvoices.size)
        retryableInvoices.forEach {
            billingService.billInvoice(it)
        }
    }

    override fun run() {
        execute()
    }
}