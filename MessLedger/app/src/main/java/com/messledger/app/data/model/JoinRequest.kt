package com.messledger.app.data.model

data class JoinRequest(
    val uid: String = "",
    val name: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val status: String = "pending"
)
