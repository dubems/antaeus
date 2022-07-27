/*
    Implements endpoints related to invoices.
 */

package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.utils.calculateNextExecution
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.FailedInvoiceBilling
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import mu.KotlinLogging
import java.time.OffsetDateTime


class InvoiceService(
    private val dal: AntaeusDal,
) {

    private val log = KotlinLogging.logger { }

    fun fetchAll(): List<Invoice> {
        return dal.fetchInvoices()
    }

    fun fetch(id: Int): Invoice {
        return dal.fetchInvoice(id) ?: throw InvoiceNotFoundException(id)
    }

    fun fetchInvoicesForBilling(): List<Invoice> {
        return dal.fetchInvoicesByStatus(InvoiceStatus.PENDING)
    }

    fun hasBeenPaid(invoice: Invoice): Boolean {
        val inv = dal.fetchInvoice(invoice.id) ?: throw InvoiceNotFoundException(invoice.id)
        return inv.status == InvoiceStatus.PAID
    }

    fun updateInvoicesStatus(invoices: List<Invoice>, status: InvoiceStatus) {
        dal.updateInvoicesStatus(invoices, status)
        log.info("process=updateInvoicesStatus, status=success, old={}, new={}", invoices.first().status, status)
    }

}
