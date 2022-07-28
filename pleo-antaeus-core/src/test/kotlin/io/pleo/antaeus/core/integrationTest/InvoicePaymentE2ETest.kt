package io.pleo.antaeus.core.integrationTest

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.pleo.antaeus.core.config.KafkaClientFactory
import io.pleo.antaeus.core.event.BillInvoiceConsumer
import io.pleo.antaeus.core.event.BillInvoicePublisher
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.core.external.getNotificationProvider
import io.pleo.antaeus.core.jobs.BillInvoiceJob
import io.pleo.antaeus.core.jobs.EventConsumptionJob
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.InvoicePaymentService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.*
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import org.awaitility.Awaitility.await
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoicePaymentE2ETest : BaseIntegrationTest() {

    private val kafkaClientFactory = KafkaClientFactory("localhost:$kafkaMappedPort")

    private val objectMapper = ObjectMapper().registerKotlinModule()

    private val invoicePaymentDal = InvoicePaymentDal(db = db)

    private val dal = AntaeusDal(db = db)

    private val paymentProvider = getPaymentProvider()

    private val invoiceService = InvoiceService(dal)

    private val invoicePaymentService = InvoicePaymentService(invoiceService, invoicePaymentDal)

    private val billingService = BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        notificationProvider = getNotificationProvider(),
        invoicePaymentService = invoicePaymentService
    )

    private val billInvoicePublisher = BillInvoicePublisher(objectMapper, kafkaClientFactory.createProducer())

    private val billInvoiceJob = BillInvoiceJob(invoiceService, billInvoicePublisher)

    private val billInvoiceConsumer =
        BillInvoiceConsumer(objectMapper, kafkaClientFactory.createConsumer(), billingService, invoiceService)

    @BeforeAll
    fun beforeAll() {
        val tables = arrayOf(InvoiceTable, CustomerTable, FailedInvoicePaymentTable)
        transaction {
            SchemaUtils.drop(*tables)
            SchemaUtils.create(*tables)
        }

        setupInitialData(dal = dal)

        //setup Kafka event consumption
        val timer = Timer()
        val eventConsumptionJob = EventConsumptionJob(billInvoiceConsumer)
        timer.schedule(eventConsumptionJob, 0, 5000L)
    }

    @Test
    fun `should consume from Kafka and process invoice payment when events are published`() {
        //publish pending invoices to kafka
        billInvoiceJob.execute(true)

        await().atMost(5, TimeUnit.SECONDS).until {
            val allInvoice = invoiceService.fetchAll()
            invoiceService.fetchAll().filter { it.status == InvoiceStatus.PAID }.size == allInvoice.size
        }
    }

    private fun setupInitialData(dal: AntaeusDal) {
        val customers = (1..5).mapNotNull {
            dal.createCustomer(
                currency = Currency.values()[Random.nextInt(0, Currency.values().size)]
            )
        }

        customers.forEach { customer ->
            (1..5).forEach {
                dal.createInvoice(
                    amount = Money(
                        value = BigDecimal(Random.nextDouble(10.0, 500.0)),
                        currency = customer.currency
                    ),
                    customer = customer,
                    status = if (it == 1) InvoiceStatus.PENDING else InvoiceStatus.PAID
                )
            }
        }
    }

    //always successfully charges payment
    private fun getPaymentProvider(): PaymentProvider {
        return object : PaymentProvider {
            override fun charge(invoice: Invoice): Boolean {
                return true
            }
        }
    }
}