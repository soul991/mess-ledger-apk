package com.messledger.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messes")
data class CachedMess(
    @PrimaryKey
    val id: String,
    val messName: String,
    val categories: List<String>,
    val createdAt: Long
)
