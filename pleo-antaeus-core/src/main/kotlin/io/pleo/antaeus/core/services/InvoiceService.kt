/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class InvoiceService(private val dal: AntaeusDal) {
    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

//    fun markInvoiceAsPaid(invoice: Invoice) = dal.updateInvoice(invoice.id, InvoiceStatus.PAID)
//    fun fetchInvoicesForBilling(): List<Invoice> {
//
//
//
//        //todo: find invoices.
//        //if today's date is the first of the month, return Invoice with status PENDING and
//        //orderBy createdAt where createdAt is within lastMonth
//       //PAYMENT_FAIlED (where maxRetries has not been exceeded) in Batches ?
//        //if not first of the month,
//    }

}
