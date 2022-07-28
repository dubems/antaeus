package io.pleo.antaeus.core.integrationTest

import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.*
import org.assertj.core.api.Java6Assertions.assertThat
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoicePaymentDalIntegrationTest : BaseIntegrationTest() {

    private val invoicePaymentDal: InvoicePaymentDal = InvoicePaymentDal(db)

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
    fun `should fetch failedInvoicePayment when it exists`() {
        val invoiceId = invoice()!!.id
        val failedInvoicePayment = failedInvoicePayment(invoiceId)
        invoicePaymentDal.saveFailedInvoicePayment(failedInvoicePayment)

        val failedInvoice = invoicePaymentDal.fetchFailedInvoicePayment(invoiceId)

        assertThat(failedInvoice).usingRecursiveComparison()
            .ignoringFieldsOfTypes(OffsetDateTime::class.java)
            .isEqualTo(failedInvoicePayment)
    }

    @Test
    fun `should return null when failedInvoicePayment does not exist`() {
        val failedInvoices = invoicePaymentDal.fetchFailedInvoicePayment(1)

        assertEquals(null, failedInvoices)
    }

    @Test
    fun `should save failed invoice payment`() {
        val invoiceId = invoice()!!.id
        val failedInvoicePayment = failedInvoicePayment(invoiceId)
        invoicePaymentDal.saveFailedInvoicePayment(failedInvoicePayment)

        val failedInvoice = invoicePaymentDal.fetchFailedInvoicePayment(invoiceId)

        assertThat(failedInvoice).usingRecursiveComparison()
            .ignoringFieldsOfTypes(OffsetDateTime::class.java)
            .isEqualTo(failedInvoicePayment)
    }

    @Test
    fun `should delete failed invoice payment if it exists`() {
        val invoiceId = invoice()!!.id
        val failedInvoicePayment = failedInvoicePayment(invoiceId)
        invoicePaymentDal.saveFailedInvoicePayment(failedInvoicePayment)

        invoicePaymentDal.removeFailedPaymentIfExist(invoiceId)

        assertEquals(null, invoicePaymentDal.fetchFailedInvoicePayment(invoiceId))
    }

    @Test
    fun `should fetch retryable failed invoice payment when it exists`() {
        val retryableInvoices = listOf(invoice()!!, invoice()!!)
        val nonRetryableInvoices = listOf(invoice()!!, invoice()!!, invoice()!!)
        retryableInvoices.map {
            invoicePaymentDal.saveFailedInvoicePayment(
                failedInvoicePayment(
                    it.id,
                    2,
                    OffsetDateTime.now().minusMinutes(15),
                    true
                )
            )
        }

        val maxRetry = 3
        listOf(
            invoicePaymentDal.saveFailedInvoicePayment(
                failedInvoicePayment(
                    nonRetryableInvoices[0].id, 1, OffsetDateTime.now().plusMinutes(15), true
                )
            ), invoicePaymentDal.saveFailedInvoicePayment(
                failedInvoicePayment(
                    nonRetryableInvoices[1].id, 1, OffsetDateTime.now().minusMinutes(15), false
                )
            ), invoicePaymentDal.saveFailedInvoicePayment(
                failedInvoicePayment(
                    nonRetryableInvoices[2].id, maxRetry + 1, OffsetDateTime.now().minusMinutes(15), true
                )
            )
        )

        val returnedRetryables = invoicePaymentDal.fetchRetryableFailedInvoice(maxRetry)

        assertEquals(retryableInvoices.size, returnedRetryables.size)
        assertThat(returnedRetryables).containsExactlyInAnyOrderElementsOf(retryableInvoices)
    }


    private fun failedInvoicePayment(
        invoiceId: Int,
        failureCount: Int = 0,
        nextExecution: OffsetDateTime = OffsetDateTime.now(),
        retryable: Boolean = true,
    ): FailedInvoicePayment {
        return FailedInvoicePayment(
            invoiceId, failureCount, nextExecution, retryable
        )
    }

    private fun invoice(): Invoice? {
        val customer = dal.createCustomer(Currency.EUR)
        return dal.createInvoice(
            Money(BigDecimal.valueOf(20), Currency.EUR), customer!!, InvoiceStatus.PROCESSING
        )
    }
}