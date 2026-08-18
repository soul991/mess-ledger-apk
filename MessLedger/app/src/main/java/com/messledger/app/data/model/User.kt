package com.messledger.app.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val messMemberships: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
