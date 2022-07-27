package io.pleo.antaeus.core.utils

import java.time.OffsetDateTime


fun isFirstOfMonth(): Boolean {
    return OffsetDateTime.now().dayOfMonth == 1
}

 fun calculateNextExecution(failedAttempt: Int): OffsetDateTime {
        val backOffConstant = 8L
        //add jitter to spread the request
        return OffsetDateTime.now().plusHours(failedAttempt * backOffConstant)
}