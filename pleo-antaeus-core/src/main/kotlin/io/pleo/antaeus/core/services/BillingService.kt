package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InsufficientBalanceException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import mu.KotlinLogging

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService
) {

    private val logger = KotlinLogging.logger {}

// TODO - Add code e.g. here
    //have a cron job run every month, considering the cur and timezone and unpaid invoices and also failed attempt
    //bill the customer using payment provider, then create an invoice about it
    //capture the failures and retry using an exponential backoff adn with a max retry
    //do not retry the payment with insufficient balances or when CustomerNotFoundException or CurrencyMismatchException is thrown
    //only retry when there is a network exception. How do i retry ?
    //notify/raise an alert for manual intervention
    // add MDC logging

    //todo: enable concurrent processing with thread safe
//    fun billInvoices(invoices: List<Invoice>) {
//        //todo: use kotlin sequence to process the invoices in batches
//        //todo: list concurrent processing as an improvement ?
//        invoices.forEach { invoice ->
//            //todo: Add MDC logging here
//            logger.info { }
//            billInvoice(invoice)
//        }
//    }

//    private fun billInvoice(invoice: Invoice) {
//        try {
//
//            val isSuccessful = paymentProvider.charge(invoice)
//            if (!isSuccessful) throw InsufficientBalanceException(invoice.customerId, invoice.id)
//            invoiceService.markInvoiceAsPaid(invoice)
//
//        } catch (ex: NetworkException) {
//            //todo: does this need to be here?
//        } catch (ex: CustomerNotFoundException) {
//            //todo: handleNonRetryable and notify someone
//
//        } catch (ex: CurrencyMismatchException) {
//            //todo: notify someone and save for retry
//
//        } catch (ex: InsufficientBalanceException) {
//            //todo: notify someone and save for retry
//
//        }
//    }

}
