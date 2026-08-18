package com.messledger.app.data.model

data class Member(
    val id: String = "",
    val name: String = "",
    val role: String = "member",
    val joinedAt: String = "", // YYYY-MM-DD
    val deletedAt: Long? = null
) {
    val isManager: Boolean
        get() = role == "manager"
        
    val isActive: Boolean
        get() = deletedAt == null
}
