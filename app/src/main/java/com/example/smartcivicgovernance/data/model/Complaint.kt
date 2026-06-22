package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

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

fun DocumentSnapshot.toComplaintSafe(): Complaint? {
    return try {
        val complaintId = this.id
        val title = this.getString("title") ?: ""
        val description = this.getString("description") ?: ""
        val category = this.getString("category") ?: ""
        val imageUrl = this.getString("imageUrl") ?: ""
        val latitude = when (val lat = this.get("latitude")) {
            is Number -> lat.toDouble()
            is String -> lat.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val longitude = when (val lng = this.get("longitude")) {
            is Number -> lng.toDouble()
            is String -> lng.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val address = this.getString("address") ?: ""
        val status = this.getString("status") ?: "Pending"
        val citizenId = this.getString("citizenId") ?: ""
        val citizenName = this.getString("citizenName") ?: ""
        val workerId = this.getString("workerId")
        val workerName = this.getString("workerName")
        val proofImageUrl = this.getString("proofImageUrl")
        val createdAt = this.getTimestamp("createdAt")
        val acceptedAt = this.getTimestamp("acceptedAt")
        val resolvedAt = this.getTimestamp("resolvedAt")
        val citizenRating = when (val rating = this.get("citizenRating")) {
            is Number -> rating.toInt()
            is String -> rating.toIntOrNull()
            else -> null
        }
        val citizenFeedback = this.getString("citizenFeedback")
        val priority = this.getString("priority") ?: "Medium"
        val isDuplicate = this.getBoolean("isDuplicate") ?: false
        val verified = this.getBoolean("verified") ?: false

        Complaint(
            complaintId = complaintId,
            title = title,
            description = description,
            category = category,
            imageUrl = imageUrl,
            latitude = latitude,
            longitude = longitude,
            address = address,
            status = status,
            citizenId = citizenId,
            citizenName = citizenName,
            workerId = workerId,
            workerName = workerName,
            proofImageUrl = proofImageUrl,
            createdAt = createdAt,
            acceptedAt = acceptedAt,
            resolvedAt = resolvedAt,
            citizenRating = citizenRating,
            citizenFeedback = citizenFeedback,
            priority = priority,
            isDuplicate = isDuplicate,
            verified = verified
        )
    } catch (e: Exception) {
        null
    }
}

fun QuerySnapshot.toComplaintsSafe(): List<Complaint> {
    val list = ArrayList<Complaint>()
    for (doc in this.documents) {
        doc.toComplaintSafe()?.let { list.add(it) }
    }
    return list
}

