package io.pleo.antaeus.models

import java.time.OffsetDateTime

data class InvoiceFailedBilling(
    val invoiceId: Int,
    var failureCount: Int,
    var nextExecution: OffsetDateTime,
    val isRetryable: Boolean
)