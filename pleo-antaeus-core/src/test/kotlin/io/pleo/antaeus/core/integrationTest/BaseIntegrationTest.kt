package io.pleo.antaeus.core.integrationTest

import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.FailedInvoicePaymentTable
import io.pleo.antaeus.data.InvoiceTable
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import java.sql.Connection
import java.util.stream.Stream

open class BaseIntegrationTest {

    companion object {
        private const val kafkaImage = "confluentinc/cp-kafka"
        private const val postgreSQLImage = "postgres:13.7-alpine"
        private const val kafkaExposedPort = 9093
        private const val DB_USER = "anataeus"
        private const val DB_NAME = "anataeus-db"
        private const val DB_PASSWORD = "secret"


        val kafkaMappedPort: Int

        private val dBUrl: String

        val db: Database

        private val log = KotlinLogging.logger { }

        @JvmStatic
        @Container
        private val kafkaContainer = KafkaContainer(DockerImageName.parse(kafkaImage))
            .withExposedPorts(kafkaExposedPort)

        @JvmStatic
        @Container
        private val postgreSQLiteContainer = PostgreSQLContainer(
            DockerImageName.parse(postgreSQLImage).asCompatibleSubstituteFor("postgres")
        ).withDatabaseName(DB_NAME)
            .withUsername(DB_USER)
            .withPassword(DB_PASSWORD)

        init {

            Stream.of(
                kafkaContainer,
                postgreSQLiteContainer
            ).parallel().forEach(GenericContainer<*>::start)

            kafkaContainer.getMappedPort(kafkaExposedPort).also {
                kafkaMappedPort = it
                log.info("Started kafka container, mapped port = {}", it)
            }

            postgreSQLiteContainer.jdbcUrl.also {
                dBUrl = it
                log.info("Started postgresql container, jdbc url = {}", it)
            }

            //setup DB for testing
            val tables = arrayOf(InvoiceTable, CustomerTable, FailedInvoicePaymentTable)
            Database.connect(dBUrl, "org.postgresql.Driver", DB_USER, DB_PASSWORD)
                .also {
                    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                    transaction(it) {
                        addLogger(StdOutSqlLogger)
                        // Drop all existing tables to ensure a clean slate on each run
                        SchemaUtils.drop(*tables)
                        // Create all tables
                        SchemaUtils.create(*tables)
                    }
                }.also { db = it }
        }
    }
}
