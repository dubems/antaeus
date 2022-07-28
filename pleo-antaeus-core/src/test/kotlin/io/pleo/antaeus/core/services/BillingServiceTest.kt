package io.pleo.antaeus.core.services

import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.NotificationProvider
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class BillingServiceTest {

    @MockK
    private lateinit var paymentProvider: PaymentProvider

    @MockK
    private lateinit var invoicePaymentService: InvoicePaymentService

    @MockK
    private lateinit var invoiceService: InvoiceService

    @MockK
    private lateinit var notificationProvider: NotificationProvider

    @InjectMockKs
    private lateinit var underTest: BillingService

    @Test
    fun `should not charge payment when invoice has already been paid`() {
        val invoice = invoice(InvoiceStatus.PAID)
        every { invoiceService.hasBeenPaid(invoice = invoice) } returns true

        underTest.billInvoice(invoice)

        verify { invoiceService.hasBeenPaid(invoice) }
        //verifies that these didn't happen
        verifyAll(true) {
            paymentProvider.charge(invoice)
            invoicePaymentService.markInvoiceAsPaid(invoice)
            notificationProvider.notifyAdmin()
            invoicePaymentService.captureFailedInvoicePayment(invoice, any() as Boolean)
        }
        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
    }

    @Test
    fun `should mark invoice as paid when payment charge is successful`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        every { invoiceService.hasBeenPaid(invoice) } returns false
        every { paymentProvider.charge(invoice) } returns true
        every { invoicePaymentService.markInvoiceAsPaid(invoice) } just runs

        underTest.billInvoice(invoice)

        verify { invoicePaymentService.markInvoiceAsPaid(invoice) }
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify { invoiceService.hasBeenPaid(invoice) }

        verifyAll(true) {
            notificationProvider.notifyAdmin()
            invoicePaymentService.captureFailedInvoicePayment(invoice, any() as Boolean)
        }

        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
    }

    @Test
    fun `should retry payment charge when there is a NetworkException`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        every { invoiceService.hasBeenPaid(invoice) } returns false
        every { paymentProvider.charge(invoice) } throws NetworkException() andThen true
        every { invoicePaymentService.markInvoiceAsPaid(invoice) } just runs

        underTest.billInvoice(invoice)

        verify { invoicePaymentService.markInvoiceAsPaid(invoice) }
        verify(exactly = 2) { paymentProvider.charge(invoice) }
        verify { invoiceService.hasBeenPaid(invoice) }

        verifyAll(true) {
            notificationProvider.notifyAdmin()
            invoicePaymentService.captureFailedInvoicePayment(invoice, any() as Boolean)
        }

        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
    }

    @Test
    fun `should handle failed invoice payment when CurrencyMismatchException occurs`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        every { invoiceService.hasBeenPaid(invoice) } returns false
        every { paymentProvider.charge(invoice) } throws CurrencyMismatchException(invoice.id, invoice.customerId)
        every { notificationProvider.notifyAdmin() } just runs
        every { invoicePaymentService.captureFailedInvoicePayment(invoice, true) } just runs

        underTest.billInvoice(invoice)

        verify { notificationProvider.notifyAdmin() }
        verify { invoicePaymentService.captureFailedInvoicePayment(invoice, true) }
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify { invoiceService.hasBeenPaid(invoice) }

        verifyAll(true) {
            invoicePaymentService.markInvoiceAsPaid(invoice)
        }

        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
    }

    @Test
    fun `should handle failed invoice payment when CustomerNotFoundException occurs`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        every { invoiceService.hasBeenPaid(invoice) } returns false
        every { paymentProvider.charge(invoice) } throws CustomerNotFoundException(invoice.customerId)
        every { notificationProvider.notifyAdmin() } just runs
        every { invoicePaymentService.captureFailedInvoicePayment(invoice, false) } just runs

        underTest.billInvoice(invoice)

        verify { notificationProvider.notifyAdmin() }
        verify { invoicePaymentService.captureFailedInvoicePayment(invoice, false) }
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify { invoiceService.hasBeenPaid(invoice) }

        verifyAll(true) {
            invoicePaymentService.markInvoiceAsPaid(invoice)
        }

        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
    }


    @Test
    fun `should handle failed invoice payment when payment charge is not successful (returns false)`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        every { invoiceService.hasBeenPaid(invoice) } returns false
        every { paymentProvider.charge(invoice) } returns false
        every { notificationProvider.notifyAdmin() } just runs
        every { invoicePaymentService.captureFailedInvoicePayment(invoice, true) } just runs

        underTest.billInvoice(invoice)

        verify { notificationProvider.notifyAdmin() }
        verify { invoicePaymentService.captureFailedInvoicePayment(invoice, true) }
        verify(exactly = 1) { paymentProvider.charge(invoice) }
        verify { invoiceService.hasBeenPaid(invoice) }

        verifyAll(true) {
            invoicePaymentService.markInvoiceAsPaid(invoice)
        }

        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
    }

    @Test
    fun `should not charge payment when invoice is not found`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)
        every { invoiceService.hasBeenPaid(invoice) } throws InvoiceNotFoundException(invoice.id)

        underTest.billInvoice(invoice)

        verify { invoiceService.hasBeenPaid(invoice) }

        verifyAll(true) {
            notificationProvider.notifyAdmin()
            invoicePaymentService.captureFailedInvoicePayment(invoice, true)
            paymentProvider.charge(invoice)
            invoicePaymentService.markInvoiceAsPaid(invoice)
        }

        confirmVerified(paymentProvider, invoiceService, invoicePaymentService, notificationProvider)
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
}