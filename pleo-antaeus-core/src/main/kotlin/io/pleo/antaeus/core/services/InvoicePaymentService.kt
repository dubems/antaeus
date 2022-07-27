package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.utils.calculateNextExecution
import io.pleo.antaeus.data.InvoicePaymentDal
import io.pleo.antaeus.data.dbTransaction
import io.pleo.antaeus.models.FailedInvoiceBilling
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.time.OffsetDateTime

class InvoicePaymentService(
    private val invoiceService: InvoiceService,
    private val invoicePaymentDal: InvoicePaymentDal,
) {

    private val log = KotlinLogging.logger { }

    companion object {
        private const val MAX_PAYMENT_RETRIES = 3
    }

    fun captureFailedInvoicePayment(invoice: Invoice, isRetryable: Boolean) {
        val failedInvoice = fetchFailedInvoice(invoice) ?: FailedInvoiceBilling(
            invoiceId = invoice.id, failureCount = 0, nextExecution = OffsetDateTime.now(), isRetryable = isRetryable
        )
        with(failedInvoice) {
            failureCount = ++failureCount
            nextExecution = calculateNextExecution(failureCount)
        }
        invoicePaymentDal.saveFailedInvoiceBilling(failedInvoice)
        log.info("process=captureFailedInvoicePayment, status=success, invoiceId={}", invoice.id)
    }

    fun markInvoiceAsPaid(invoice: Invoice) {
        dbTransaction {
            invoiceService.updateInvoicesStatus(listOf(invoice), InvoiceStatus.PAID)
            invoicePaymentDal.removeFailedPaymentIfExist(invoice.id)
        }
        log.info("process=markInvoiceAsPaid, status=success, invoiceId={}", invoice.id)
    }

    fun fetchRetryableInvoicePayments(): List<Invoice> {
        return invoicePaymentDal.fetchRetryableFailedInvoice(MAX_PAYMENT_RETRIES)
    }

    private fun fetchFailedInvoice(invoice: Invoice): FailedInvoiceBilling? {
        return invoicePaymentDal.fetchFailedInvoiceBilling(invoice.id)
    }
}