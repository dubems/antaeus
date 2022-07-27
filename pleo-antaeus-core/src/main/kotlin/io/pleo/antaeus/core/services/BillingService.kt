package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.*
import io.pleo.antaeus.core.external.NotificationProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.utils.retryer
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val invoicePaymentService: InvoicePaymentService,
    private val notificationProvider: NotificationProvider
) {

    private val logger = KotlinLogging.logger {}

    companion object {
        private const val MAX_RETRIES = 3
    }

    fun billInvoice(invoice: Invoice) {
        try {
            if (invoiceService.hasBeenPaid(invoice)) {
                logger.warn(
                    "process=billInvoice, status=warning, message=Invoice has been paid, invoiceId={}, customerId ={}",
                    invoice.id, invoice.customerId
                )
                return
            }
            val isSuccessful = retryer(MAX_RETRIES, listOf(NetworkException())) { paymentProvider.charge(invoice) }
            if (!isSuccessful) throw InsufficientBalanceException(invoice.customerId, invoice.id)
            invoicePaymentService.markInvoiceAsPaid(invoice)
        } catch (ex: CustomerNotFoundException) {
            logger.error("process=billInvoice, status=error,  ex=${ex.message}")
            handleFailedInvoiceBilling(invoice, false)
        } catch (ex: CurrencyMismatchException) {
            logger.error("process=billInvoice, status=error, ex=${ex.message}")
            handleFailedInvoiceBilling(invoice, true)
        } catch (ex: InsufficientBalanceException) {
            logger.error("process=billInvoice, status=error, ex=${ex.message}")
            handleFailedInvoiceBilling(invoice, true)
        } catch (ex: InvoiceNotFoundException) {
            logger.warn("process=billInvoice, status=warn ex=${ex.message}")
            return
        }
    }

    private fun handleFailedInvoiceBilling(invoice: Invoice, isRetryable: Boolean) {
        notificationProvider.notifyAdmin()
        invoicePaymentService.captureFailedInvoicePayment(invoice, isRetryable)
    }
}
