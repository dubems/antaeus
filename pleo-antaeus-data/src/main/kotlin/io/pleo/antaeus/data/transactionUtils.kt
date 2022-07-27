package io.pleo.antaeus.data

import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * This is a 'hack' for having something similar to Propagation.REQUIRED as in SpringBoot
 * I wasn't sure how to do it in Exposed of if the feature even exists
 */
fun <T> dbTransaction(block: Transaction.() -> T): T {
    val currentTransaction = TransactionManager.currentOrNull()
    //not in transaction -> get into one
    return if (currentTransaction == null) {
        transaction { block() }
    } else {
        //already in transaction so execute in there
        block(currentTransaction)
    }
}
