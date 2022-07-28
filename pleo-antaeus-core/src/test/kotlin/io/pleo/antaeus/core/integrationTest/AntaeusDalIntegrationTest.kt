package io.pleo.antaeus.core.integrationTest

import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.FailedInvoicePaymentTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AntaeusDalIntegrationTest : BaseIntegrationTest() {

    private val dal: AntaeusDal = AntaeusDal(db)

    @BeforeEach
    fun beforeEach() {
        val tables = arrayOf(InvoiceTable, CustomerTable, FailedInvoicePaymentTable)
        transaction {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }
    }

    @Test
    fun `should fetch invoice by status`() {
        val paidInvoices = listOf(InvoiceStatus.PAID, InvoiceStatus.PAID).map { invoice(it) }
        listOf(InvoiceStatus.PROCESSING, InvoiceStatus.PENDING).forEach { invoice(it) }

        assertEquals(paidInvoices.size, dal.fetchInvoicesByStatus(InvoiceStatus.PAID).size)
    }

    @Test
    fun `should update invoiceStatus and paidAt when new status is PAID`() {
        val invoice = invoice(InvoiceStatus.PROCESSING)

        dal.updateInvoicesStatus(listOf(invoice!!), InvoiceStatus.PAID)

        val updatedInvoice = dal.fetchInvoice(invoice.id)
        assertEquals(updatedInvoice?.status, InvoiceStatus.PAID)
        assertThat(updatedInvoice?.paidAt).isNotNull
    }

    @Test
    fun `should update only invoiceStatus when new status is not PAID`() {
        val invoice = invoice(InvoiceStatus.PENDING)

        dal.updateInvoicesStatus(listOf(invoice!!), InvoiceStatus.PROCESSING)

        val updatedInvoice = dal.fetchInvoice(invoice.id)
        assertEquals(updatedInvoice?.status, InvoiceStatus.PROCESSING)
        assertThat(updatedInvoice?.paidAt).isNull()
    }

    private fun invoice(status: InvoiceStatus): Invoice? {
        val customer = dal.createCustomer(Currency.EUR)
        return dal.createInvoice(
            Money(BigDecimal.valueOf(20), Currency.EUR), customer!!, status
        )
    }
}