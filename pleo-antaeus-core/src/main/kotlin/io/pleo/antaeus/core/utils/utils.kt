package io.pleo.antaeus.core.utils

import mu.KotlinLogging

val logger = KotlinLogging.logger { }

inline fun retryer(MAX_RETRIES: Int, retryCondition: List<Exception>, func: () -> Boolean): Boolean {
    var retries = 0
    var response = false
    while (retries < MAX_RETRIES) {
        try {
            response = func()
            break
        } catch (ex: Exception) {
            when (ex) {
                in retryCondition -> {
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