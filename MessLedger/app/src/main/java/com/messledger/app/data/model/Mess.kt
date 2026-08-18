package com.messledger.app.data.model

data class Mess(
    val id: String = "",
    val messName: String = "",
    val categories: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = ""
)
