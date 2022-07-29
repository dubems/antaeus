package io.pleo.antaeus.core.external

import mu.KotlinLogging

interface NotificationProvider {
    fun notifyAdmin()
}

fun getNotificationProvider(): NotificationProvider {
    val log = KotlinLogging.logger { }
    return object : NotificationProvider {
        override fun notifyAdmin() {
            log.info("process=notifyAdmin, message=Dummy notification called!")
        }
    }
}