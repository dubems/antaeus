package io.pleo.antaeus.core.services

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.exceptions.InvoiceNotFoundException
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class InvoiceServiceTest {
    @MockK
    private lateinit var dal: AntaeusDal

    @InjectMockKs
    private lateinit var underTest: InvoiceService

    @Test
    fun `will throw if invoice is not found`() {
        every { dal.fetchInvoice(404) } returns null

        assertThrows<InvoiceNotFoundException> {
            underTest.fetch(404)
        }
    }

    @Test
    fun `should return false when Invoice hasn't been paid`() {
        val invoice = invoice(InvoiceStatus.PENDING)
        every { dal.fetchInvoice(invoice.id) } returns invoice

        assertEquals(false, underTest.hasBeenPaid(invoice))
    }

    @Test
    fun `should return true when Invoice has been paid`() {
        val invoice = invoice(InvoiceStatus.PAID)
        every { dal.fetchInvoice(invoice.id) } returns invoice

        assertEquals(true, underTest.hasBeenPaid(invoice))
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
