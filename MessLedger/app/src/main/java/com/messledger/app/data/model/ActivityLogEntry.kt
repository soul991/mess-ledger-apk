package com.messledger.app.data.model

data class ActivityLogEntry(
    val id: String = "",
    val actorUid: String = "",
    val actorName: String = "",
    val action: String = "",
    val summary: String = "",
    val targetId: String? = null,
    val amount: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)
