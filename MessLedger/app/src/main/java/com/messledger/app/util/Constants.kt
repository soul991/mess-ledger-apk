package com.messledger.app.util

object Constants {
    const val SYNTHETIC_EMAIL_DOMAIN = "messledger.internal"
    const val MESS_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789#*-_"
    const val MESS_ID_LENGTH = 8

    const val ROLE_MANAGER = "manager"
    const val ROLE_MEMBER = "member"

    object Actions {
        const val MEMBER_JOINED = "MEMBER_JOINED"
        const val MEMBER_LEFT = "MEMBER_LEFT"
        const val EXPENSE_ADDED = "EXPENSE_ADDED"
        const val CONTRIBUTION_ADDED = "CONTRIBUTION_ADDED"
        const val MANAGER_TRANSFERRED = "MANAGER_TRANSFERRED"
    }
}
