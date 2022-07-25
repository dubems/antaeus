package io.pleo.antaeus.models

data class FailedInvoicePayment(
    val invoiceId: Int,
    val failureCount:Int,
    val nextExecution: Int
)