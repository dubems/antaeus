package io.pleo.antaeus.core.jobs

import io.pleo.antaeus.core.event.EventConsumer
import java.util.TimerTask

class EventConsumptionJob(
    private val eventConsumer: EventConsumer
) : TimerTask() {

    override fun run() {
        eventConsumer.consume()
    }
}