package io.pleo.antaeus.models

import java.time.OffsetDateTime

data class FailedInvoicePayment(
    val invoiceId: Int,
    var failureCount: Int,
    var nextExecution: OffsetDateTime,
    val isRetryable: Boolean
)