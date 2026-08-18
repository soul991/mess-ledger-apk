package com.messledger.app.data.model

data class LeaveRequest(
    val uid: String = "",
    val name: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "pending",
    val reason: String? = null
)
