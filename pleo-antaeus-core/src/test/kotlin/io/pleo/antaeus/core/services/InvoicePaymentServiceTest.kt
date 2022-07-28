package io.pleo.antaeus.core.services

import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import io.pleo.antaeus.data.InvoicePaymentDal
import io.pleo.antaeus.models.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.OffsetDateTime

@ExtendWith(MockKExtension::class)
class InvoicePaymentServiceTest {

    @MockK
    private lateinit var invoiceService: InvoiceService

    @MockK
    private lateinit var invoicePaymentDal: InvoicePaymentDal

    @InjectMockKs
    private lateinit var underTest: InvoicePaymentService

    @Test
    fun `should create failedInvoicePayment when payment fails for the first time`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        val failedInvoiceSlot = slot<FailedInvoicePayment>()

        every { invoicePaymentDal.saveFailedInvoicePayment(capture(failedInvoiceSlot)) } returns Unit
        every { invoicePaymentDal.fetchFailedInvoicePayment(any()) } returns null

        underTest.captureFailedInvoicePayment(invoice = invoice, true)

        assertEquals(1, failedInvoiceSlot.captured.failureCount)
        assertEquals(invoice.id, failedInvoiceSlot.captured.invoiceId)
        verify { invoicePaymentDal.saveFailedInvoicePayment(any() as FailedInvoicePayment) }
        verify { invoicePaymentDal.fetchFailedInvoicePayment(any() as Int) }

        confirmVerified(invoicePaymentDal, invoiceService)
    }

    @Test
    fun `should update failed invoice payment when payment fails after the first time`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        val failedInvoicePayment = failedInvoicePayment(invoice.id)
        val failedInvoiceSlot = slot<FailedInvoicePayment>()

        every { invoicePaymentDal.saveFailedInvoicePayment(capture(failedInvoiceSlot)) } returns Unit
        every { invoicePaymentDal.fetchFailedInvoicePayment(any()) } returns failedInvoicePayment

        underTest.captureFailedInvoicePayment(invoice = invoice, true)

        assertEquals(2, failedInvoiceSlot.captured.failureCount)
        assertEquals(invoice.id, failedInvoiceSlot.captured.invoiceId)
        verify { invoicePaymentDal.saveFailedInvoicePayment(any() as FailedInvoicePayment) }
        verify { invoicePaymentDal.fetchFailedInvoicePayment(any() as Int) }

        confirmVerified(invoicePaymentDal, invoiceService)
    }

    private fun invoice(status: InvoiceStatus): Invoice {
        return Invoice(
            id = 20,
            customerId = 29,
            amount = Money(BigDecimal.valueOf(20.00), Currency.EUR),
            status = status,
            paidAt = null
        )
    }

    private fun failedInvoicePayment(invoiceId: Int): FailedInvoicePayment {
        return FailedInvoicePayment(
            invoiceId = invoiceId, failureCount = 1, nextExecution = OffsetDateTime.now(), isRetryable = true
        )
    }

}