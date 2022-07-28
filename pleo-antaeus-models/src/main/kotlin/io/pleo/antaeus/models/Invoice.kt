package io.pleo.antaeus.models

import java.time.OffsetDateTime

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
    val status: InvoiceStatus,
    val paidAt: OffsetDateTime?
)
