package com.messledger.app.data.local.entity

import androidx.room.Entity

@Entity(tableName = "members", primaryKeys = ["id", "messId"])
data class CachedMember(
    val id: String,
    val messId: String,
    val name: String,
    val role: String,
    val joinedAt: String,
    val deletedAt: Long?
)
