package com.messledger.app.data.model

data class GuestMeal(
    val id: String = "",
    val hostId: String = "",
    val date: String = "", // YYYY-MM-DD
    val meal: String = "lunch", // "lunch" or "dinner"
    val count: Int = 0,
    val note: String? = null
)
