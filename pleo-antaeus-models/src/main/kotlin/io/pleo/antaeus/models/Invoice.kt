package io.pleo.antaeus.models

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.OffsetDateTime

data class Invoice(
    val id: Int,
    val customerId: Int,
    val amount: Money,
    val status: InvoiceStatus,
    @JsonIgnore
    val paidAt: OffsetDateTime?
)
