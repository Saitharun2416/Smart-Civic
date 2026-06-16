package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "", // "citizen" | "worker" | "admin"
    val photoUrl: String = "",
    val createdAt: Timestamp? = null,
    val fcmToken: String = "",
    val disabled: Boolean = false
)
