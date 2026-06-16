package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp

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
