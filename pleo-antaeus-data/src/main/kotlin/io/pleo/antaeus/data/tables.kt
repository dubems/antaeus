/*
    Defines database tables and their schemas.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Table

object InvoiceTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
    val value = decimal("value", 1000, 2)
    val customerId = reference("customer_id", CustomerTable.id)
    val status = text("status").index()
    val paidAt = long("paid_at").nullable()
}

object FailedInvoicePaymentTable : Table() {
    val id = uuid("id").primaryKey()
    val invoiceId = reference("invoice_id", InvoiceTable.id)
    val failureCount = integer("failure_count").index()
    val nextExecution = long("next_execution").index()
    val isRetryable = bool("is_retryable").default(false)
}

object CustomerTable : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val currency = varchar("currency", 3)
}
