package io.pleo.antaeus.data

import io.pleo.antaeus.models.FailedInvoiceBilling
import io.pleo.antaeus.models.Invoice
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.Instant
import java.util.*

class InvoicePaymentDal(
    private val db: Database,
) {

    fun fetchFailedInvoiceBilling(invoiceId: Int): FailedInvoiceBilling? {
        return transaction(db) {
            FailedInvoicePaymentTable
                .select { FailedInvoicePaymentTable.invoiceId eq invoiceId }
                .firstOrNull()
                ?.toFailedInvoiceBilling()
        }
    }

    fun fetchRetryableFailedInvoice(maxRetry: Int): List<Invoice> {
        return transaction(db) {
            FailedInvoicePaymentTable
                .leftJoin(InvoiceTable, { invoiceId }, { id })
                .slice(
                    InvoiceTable.id,
                    InvoiceTable.currency,
                    InvoiceTable.value,
                    InvoiceTable.customerId,
                    InvoiceTable.status
                )
                .select { FailedInvoicePaymentTable.isRetryable eq true }
                .andWhere { FailedInvoicePaymentTable.nextExecution less Instant.now().millis }
                .andWhere { FailedInvoicePaymentTable.failureCount lessEq maxRetry }
                .orderBy(FailedInvoicePaymentTable.nextExecution, SortOrder.ASC)
                .map { it.toInvoice() }
        }
    }

    fun saveFailedInvoiceBilling(failedInvoice: FailedInvoiceBilling) {
        transaction(db) {
            FailedInvoicePaymentTable.insert {
                it[this.id] = UUID.randomUUID()
                it[this.invoiceId] = failedInvoice.invoiceId
                it[this.failureCount] = failedInvoice.failureCount
                it[this.nextExecution] = failedInvoice.nextExecution.toInstant().toEpochMilli()
                it[this.isRetryable] = failedInvoice.isRetryable
            }
        }
    }

    fun removeFailedPaymentIfExist(invoiceId: Int) {
        dbTransaction {
            FailedInvoicePaymentTable.deleteWhere {
                FailedInvoicePaymentTable.invoiceId eq invoiceId
            }
        }
    }
}
