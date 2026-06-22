package com.example.smartcivicgovernance.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

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

fun DocumentSnapshot.toUserSafe(): User? {
    return try {
        val uid = this.id
        val name = this.getString("name") ?: ""
        val email = this.getString("email") ?: ""
        val role = this.getString("role") ?: ""
        val photoUrl = this.getString("photoUrl") ?: ""
        val createdAt = this.getTimestamp("createdAt")
        val fcmToken = this.getString("fcmToken") ?: ""
        val disabled = this.getBoolean("disabled") ?: false
        User(uid, name, email, role, photoUrl, createdAt, fcmToken, disabled)
    } catch (e: Exception) {
        null
    }
}

fun QuerySnapshot.toUsersSafe(): List<User> {
    val list = ArrayList<User>()
    for (doc in this.documents) {
        doc.toUserSafe()?.let { list.add(it) }
    }
    return list
}

