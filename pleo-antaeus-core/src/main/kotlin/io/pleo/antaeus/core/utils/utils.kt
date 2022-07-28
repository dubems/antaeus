package io.pleo.antaeus.core.utils

import io.pleo.antaeus.core.exceptions.NetworkException
import mu.KotlinLogging

val logger = KotlinLogging.logger { }

inline fun retryer(MAX_RETRIES: Int, block: () -> Boolean): Boolean {
    var retries = 0
    var response = false
    while (retries < MAX_RETRIES) {
        try {
            response = block()
            break
        } catch (ex: Exception) {
            when (ex) {
                is NetworkException -> {
                    logger.info("Retrying... reason= $ex")
                    retries++
                }
                else -> {
                    throw ex
                }
            }
        }
    }
    return response
}