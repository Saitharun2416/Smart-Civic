package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp

data class Notification(
    val notifId: String = "",
    val recipientId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "", // "complaint_accepted" | "complaint_resolved" | "points_earned" | "task_assigned"
    val complaintId: String = "",
    @field:JvmField val isRead: Boolean = false,
    val createdAt: Timestamp? = null
)
