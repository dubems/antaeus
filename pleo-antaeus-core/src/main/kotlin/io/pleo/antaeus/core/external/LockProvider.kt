package io.pleo.antaeus.core.external

import mu.KotlinLogging

val log = KotlinLogging.logger { }
fun acquireLock(process: String) {
    log.info("acquire lock for process ={}", process)
}

fun releaseLock(process: String) {
    log.info("release lock for process ={}", process)
}


