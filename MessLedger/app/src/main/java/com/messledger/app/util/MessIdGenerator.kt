package com.messledger.app.util

import kotlin.random.Random

object MessIdGenerator {
    fun generateMessId(): String {
        return (1..Constants.MESS_ID_LENGTH)
            .map { Constants.MESS_ID_CHARS[Random.nextInt(Constants.MESS_ID_CHARS.length)] }
            .joinToString("")
    }
}
