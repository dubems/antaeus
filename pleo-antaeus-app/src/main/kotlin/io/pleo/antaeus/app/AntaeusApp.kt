/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import getPaymentProvider
import io.pleo.antaeus.core.config.KafkaClientFactory
import io.pleo.antaeus.core.event.BillInvoiceConsumer
import io.pleo.antaeus.core.event.BillInvoicePublisher
import io.pleo.antaeus.core.external.getNotificationProvider
import io.pleo.antaeus.core.jobs.BillInvoiceJob
import io.pleo.antaeus.core.jobs.EventConsumptionJob
import io.pleo.antaeus.core.jobs.FailedInvoicePaymentJob
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoicePaymentService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.*
import io.pleo.antaeus.rest.AntaeusRest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import setupInitialData
import java.io.File
import java.sql.Connection
import java.util.*

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable, FailedInvoicePaymentTable)

    val dbFile: File = File.createTempFile("antaeus-db", ".sqlite")
    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            driver = "org.sqlite.JDBC",
            user = "root",
            password = ""
        )
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)
    val invoicePaymentDal = InvoicePaymentDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)
    val invoicePaymentService = InvoicePaymentService(invoiceService, invoicePaymentDal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(
        paymentProvider = paymentProvider,
        invoiceService = invoiceService,
        notificationProvider = getNotificationProvider(),
        invoicePaymentService = invoicePaymentService
    )
    val kafkaClientFactory = KafkaClientFactory("localhost:9092")
    val objectMapper = ObjectMapper().registerKotlinModule()

    val billInvoicePublisher = BillInvoicePublisher(objectMapper, kafkaClientFactory.createProducer())

    val billInvoiceConsumer =
        BillInvoiceConsumer(objectMapper, kafkaClientFactory.createConsumer(), billingService, invoiceService)


    //setup KafkaEvent Consumption
    val timer = Timer()
    val eventConsumptionJob = EventConsumptionJob(billInvoiceConsumer)
    timer.schedule(eventConsumptionJob, 0, 5000L) // 5 sec

    //setup event publisher scheduler
    val billInvoiceJob = BillInvoiceJob(invoiceService, billInvoicePublisher)
    timer.schedule(billInvoiceJob, 0, 36000000L) //10 hours

    //setup failed invoice scheduler
    val failedInvoiceBillingJob = FailedInvoicePaymentJob(invoicePaymentService, billingService)
    timer.schedule(failedInvoiceBillingJob, 0, 3600000) //1 hr

    //to populate Kafka on startup for local development/testing
    billInvoiceJob.execute(true)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()
}
