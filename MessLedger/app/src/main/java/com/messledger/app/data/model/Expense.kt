package com.messledger.app.data.model

data class Expense(
    val id: String = "",
    val paidBy: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val date: String = "", // YYYY-MM-DD
    val splitType: String = "meals", // "meals" or "equal"
    val note: String? = null
)
