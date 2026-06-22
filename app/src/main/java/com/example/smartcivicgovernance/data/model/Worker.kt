package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

data class Worker(
    val uid: String = "",
    val name: String = "",
    val totalPoints: Int = 0,
    val issuesSolved: Int = 0,
    val activeTasks: Int = 0,
    val averageResolutionTimeMinutes: Double = 0.0,
    val averageRating: Double = 0.0,
    val rank: Int = 0,
    val badges: List<String> = emptyList(),
    val joinedAt: Timestamp? = null
)

fun DocumentSnapshot.toWorkerSafe(): Worker? {
    return try {
        val uid = this.id
        val name = this.getString("name") ?: ""
        val totalPoints = when (val tp = this.get("totalPoints")) {
            is Number -> tp.toInt()
            is String -> tp.toIntOrNull() ?: 0
            else -> 0
        }
        val issuesSolved = when (val isSolved = this.get("issuesSolved")) {
            is Number -> isSolved.toInt()
            is String -> isSolved.toIntOrNull() ?: 0
            else -> 0
        }
        val activeTasks = when (val at = this.get("activeTasks")) {
            is Number -> at.toInt()
            is String -> at.toIntOrNull() ?: 0
            else -> 0
        }
        val averageResolutionTimeMinutes = when (val art = this.get("averageResolutionTimeMinutes")) {
            is Number -> art.toDouble()
            is String -> art.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val averageRating = when (val ar = this.get("averageRating")) {
            is Number -> ar.toDouble()
            is String -> ar.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
        val rank = when (val r = this.get("rank")) {
            is Number -> r.toInt()
            is String -> r.toIntOrNull() ?: 0
            else -> 0
        }
        val badges = when (val b = this.get("badges")) {
            is List<*> -> b.mapNotNull { it?.toString() }
            else -> emptyList()
        }
        val joinedAt = this.getTimestamp("joinedAt")
        Worker(uid, name, totalPoints, issuesSolved, activeTasks, averageResolutionTimeMinutes, averageRating, rank, badges, joinedAt)
    } catch (e: Exception) {
        null
    }
}

fun QuerySnapshot.toWorkersSafe(): List<Worker> {
    val list = ArrayList<Worker>()
    for (doc in this.documents) {
        doc.toWorkerSafe()?.let { list.add(it) }
    }
    return list
}

