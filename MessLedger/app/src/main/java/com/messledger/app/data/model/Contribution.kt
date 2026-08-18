package com.messledger.app.data.model

data class Contribution(
    val id: String = "",
    val memberId: String = "",
    val amount: Double = 0.0,
    val date: String = "", // YYYY-MM-DD
    val note: String? = null
)
