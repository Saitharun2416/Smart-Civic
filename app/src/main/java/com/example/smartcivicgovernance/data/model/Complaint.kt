package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp

data class Complaint(
    val complaintId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "", // "Pothole" | "Garbage" | "Water Leakage" | "Drainage" | "Streetlight" | "Traffic"
    val imageUrl: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val status: String = "Pending", // "Pending" | "In Progress" | "Resolved" | "Rejected"
    val citizenId: String = "",
    val citizenName: String = "",
    val workerId: String? = null,
    val workerName: String? = null,
    val proofImageUrl: String? = null,
    val createdAt: Timestamp? = null,
    val acceptedAt: Timestamp? = null,
    val resolvedAt: Timestamp? = null,
    val citizenRating: Int? = null,
    val citizenFeedback: String? = null,
    val priority: String = "Medium", // "High" | "Medium" | "Low"
    @field:JvmField val isDuplicate: Boolean = false,
    @field:JvmField val verified: Boolean = false
)
