/*
    Defines mappings between database rows and Kotlin objects.
    To be used by `AntaeusDal`.
 */

package io.pleo.antaeus.data

import io.pleo.antaeus.models.*
import org.jetbrains.exposed.sql.ResultRow
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

fun ResultRow.toInvoice(): Invoice = Invoice(
    id = this[InvoiceTable.id],
    amount = Money(
        value = this[InvoiceTable.value],
        currency = Currency.valueOf(this[InvoiceTable.currency])
    ),
    status = InvoiceStatus.valueOf(this[InvoiceTable.status]),
    customerId = this[InvoiceTable.customerId],
    paidAt = getPaidAt(this[InvoiceTable.paidAt])
)

fun ResultRow.toFailedInvoicePayment(): FailedInvoicePayment = FailedInvoicePayment(
    invoiceId = this[FailedInvoicePaymentTable.invoiceId],
    failureCount = this[FailedInvoicePaymentTable.failureCount],
    nextExecution = OffsetDateTime.ofInstant(
        Instant.ofEpochMilli(
            this[FailedInvoicePaymentTable.nextExecution]
        ), ZoneId.systemDefault()
    ),
    isRetryable = this[FailedInvoicePaymentTable.isRetryable]
)

fun ResultRow.toCustomer(): Customer = Customer(
    id = this[CustomerTable.id],
    currency = Currency.valueOf(this[CustomerTable.currency])
)

internal fun getPaidAt(paidAt: Long?): OffsetDateTime? {
    return if (paidAt == null) paidAt
    else OffsetDateTime.ofInstant(
        Instant.ofEpochMilli(
            paidAt
        ), ZoneId.systemDefault()
    )
}