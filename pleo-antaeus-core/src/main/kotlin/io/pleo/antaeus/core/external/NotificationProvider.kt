package io.pleo.antaeus.core.external

import mu.KotlinLogging

val log = KotlinLogging.logger {  }

interface NotificationProvider {
    fun notifyAdmin()
}

//todo: make this internal ??
fun getNotificationProvider(): NotificationProvider {
    return object : NotificationProvider {
        override fun notifyAdmin() {
           log.info("process=notifyAdmin, message=Dummy notification called!")
        }
    }
}