package io.pleo.antaeus.core.exceptions

class InsufficientBalanceException(customerId: Int, invoiceId: Int) : Exception(
    "Customer with Id $customerId without sufficient balance for invoice $invoiceId"
)