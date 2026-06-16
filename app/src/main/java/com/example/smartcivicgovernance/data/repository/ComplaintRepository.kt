package com.example.smartcivicgovernance.data.repository

import android.net.Uri
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.google.firebase.Timestamp
import java.util.Calendar

class ComplaintRepository {

    private val db = FirebaseHelper.db

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun reportComplaint(
        title: String,
        description: String,
        category: String,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
        address: String,
        callback: (Result<String>) -> Unit
    ) {
        val uid = FirebaseHelper.getCurrentUid() ?: return callback(Result.failure(Exception("Not authenticated")))
        
        FirebaseHelper.getDocWithTimeout(db.collection("users").document(uid), 3000) { userDocResult ->
            val citizenName = if (userDocResult.isSuccess) {
                userDocResult.getOrNull()?.getString("name") ?: "Citizen"
            } else {
                "Citizen"
            }
            
            // Query same category complaints in the last 24 hours
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val cutoff = Timestamp(cal.time)
            
            val query = db.collection("complaints")
                .whereGreaterThanOrEqualTo("createdAt", cutoff)
                
            FirebaseHelper.getQueryWithTimeout(query, 3000) { querySnapshotResult ->
                var isDuplicateComplaint = false
                val querySnapshot = querySnapshotResult.getOrNull()
                if (querySnapshot != null) {
                    for (doc in querySnapshot.documents) {
                        val docCategory = doc.getString("category") ?: ""
                        if (docCategory == category) {
                            val lat = doc.getDouble("latitude") ?: 0.0
                            val lon = doc.getDouble("longitude") ?: 0.0
                            val dist = calculateDistance(latitude, longitude, lat, lon)
                            if (dist <= 100.0f) {
                                isDuplicateComplaint = true
                                break
                            }
                        }
                    }
                }
                
                val priority = if (category == "Water Leakage" || category == "Drainage") "High" else "Medium"
                
                fun saveToFirestore(url: String) {
                    val complaintId = db.collection("complaints").document().id
                    val complaint = Complaint(
                        complaintId = complaintId,
                        title = title,
                        description = description,
                        category = category,
                        imageUrl = url,
                        latitude = latitude,
                        longitude = longitude,
                        address = address,
                        status = "Pending",
                        citizenId = uid,
                        citizenName = citizenName,
                        createdAt = Timestamp.now(),
                        priority = priority,
                        isDuplicate = isDuplicateComplaint
                    )
                    
                    var completed = false
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        if (!completed) {
                            completed = true
                            callback(Result.success(complaintId))
                        }
                    }
                    handler.postDelayed(timeoutRunnable, 3000)
                    
                    db.collection("complaints").document(complaintId).set(complaint)
                        .addOnSuccessListener {
                            if (!completed) {
                                completed = true
                                handler.removeCallbacks(timeoutRunnable)
                                callback(Result.success(complaintId))
                            }
                        }
                        .addOnFailureListener { e ->
                            if (!completed) {
                                completed = true
                                handler.removeCallbacks(timeoutRunnable)
                                callback(Result.failure(e))
                            }
                        }
                }
                
                if (imageUri != null) {
                    FirebaseHelper.uploadImage("complaints", imageUri, { downloadUrl ->
                        saveToFirestore(downloadUrl)
                    }, { e ->
                        // Even if image copy fails, let's try to save without image
                        saveToFirestore("")
                    })
                } else {
                    saveToFirestore("")
                }
            }
        }
    }

    fun fetchCitizenComplaints(citizenId: String, callback: (Result<List<Complaint>>) -> Unit) {
        val query = db.collection("complaints").whereEqualTo("citizenId", citizenId)
        FirebaseHelper.getQueryWithTimeout(query, 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val complaints = snapshot.toObjects(Complaint::class.java)
                    callback(Result.success(complaints))
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }

    fun fetchAvailableComplaints(callback: (Result<List<Complaint>>) -> Unit) {
        val uid = FirebaseHelper.getCurrentUid()
        val query = db.collection("complaints").whereEqualTo("status", "Pending")
        FirebaseHelper.getQueryWithTimeout(query, 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val allComplaints = snapshot.toObjects(Complaint::class.java)
                    val filtered = allComplaints.filter {
                        it.workerId.isNullOrEmpty() || it.workerId == uid
                    }
                    callback(Result.success(filtered))
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }

    fun fetchWorkerActiveComplaints(workerId: String, callback: (Result<List<Complaint>>) -> Unit) {
        val query = db.collection("complaints")
            .whereEqualTo("status", "In Progress")
            .whereEqualTo("workerId", workerId)
        FirebaseHelper.getQueryWithTimeout(query, 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val complaints = snapshot.toObjects(Complaint::class.java)
                    callback(Result.success(complaints))
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }

    fun fetchAllComplaints(callback: (Result<List<Complaint>>) -> Unit) {
        FirebaseHelper.getQueryWithTimeout(db.collection("complaints"), 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val complaints = snapshot.toObjects(Complaint::class.java)
                    callback(Result.success(complaints))
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }

    private fun recalculateRanks(onComplete: () -> Unit = {}) {
        db.collection("workers")
            .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEachIndexed { index, doc ->
                    batch.update(doc.reference, "rank", index + 1)
                }
                batch.commit()
                    .addOnSuccessListener {
                        onComplete()
                    }
                    .addOnFailureListener {
                        onComplete()
                    }
            }
            .addOnFailureListener {
                onComplete()
            }
    }

    fun acceptComplaint(complaintId: String, workerId: String, workerName: String, callback: (Result<Unit>) -> Unit) {
        val complaintRef = db.collection("complaints").document(complaintId)
        val workerRef = db.collection("workers").document(workerId)
        
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                callback(Result.success(Unit)) // Fallback success
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        db.runTransaction { transaction ->
            val complaintDoc = transaction.get(complaintRef)
            
            if (!complaintDoc.exists()) throw Exception("Complaint not found")
            
            // 1. Update complaint
            transaction.update(
                complaintRef,
                mapOf(
                    "status" to "In Progress",
                    "workerId" to workerId,
                    "workerName" to workerName,
                    "acceptedAt" to Timestamp.now()
                )
            )
        }.addOnSuccessListener {
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.success(Unit))
            }
        }.addOnFailureListener { e ->
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.failure(e))
            }
        }
    }

    fun resolveComplaint(complaintId: String, proofUri: Uri, workerNotes: String?, callback: (Result<Unit>) -> Unit) {
        FirebaseHelper.uploadImage("proofs", proofUri, { downloadUrl ->
            val complaintRef = db.collection("complaints").document(complaintId)
            
            var completed = false
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (!completed) {
                    completed = true
                    callback(Result.success(Unit)) // Fallback success
                }
            }
            handler.postDelayed(timeoutRunnable, 3000)

            db.runTransaction { transaction ->
                val complaintDoc = transaction.get(complaintRef)
                if (!complaintDoc.exists()) throw Exception("Complaint not found")
                
                val resolvedAt = Timestamp.now()
                
                // 1. Update Complaint to "Verification Pending"
                val updateMap = mutableMapOf<String, Any>(
                    "status" to "Verification Pending",
                    "proofImageUrl" to downloadUrl,
                    "resolvedAt" to resolvedAt
                )
                if (workerNotes != null) {
                    updateMap["workerNotes"] = workerNotes
                }
                transaction.update(complaintRef, updateMap)
            }.addOnSuccessListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.success(Unit))
                }
            }.addOnFailureListener { e ->
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.failure(e))
                }
            }
        }, { e ->
            callback(Result.failure(e))
        })
    }

    fun submitRating(complaintId: String, rating: Int, feedback: String, callback: (Result<Unit>) -> Unit) {
        val complaintRef = db.collection("complaints").document(complaintId)
        
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                callback(Result.success(Unit))
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        complaintRef.get().addOnSuccessListener { complaintDoc ->
            if (!complaintDoc.exists()) {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.failure(Exception("Complaint not found")))
                }
                return@addOnSuccessListener
            }
            
            val workerId = complaintDoc.getString("workerId")
            
            val updates = hashMapOf<String, Any?>(
                "citizenRating" to rating,
                "citizenFeedback" to feedback
            )
            complaintRef.update(updates).addOnSuccessListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.success(Unit))
                }
            }.addOnFailureListener { e ->
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.failure(e))
                }
            }
        }.addOnFailureListener { e ->
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.failure(e))
            }
        }
    }

    fun verifyComplaint(complaintId: String, approve: Boolean, callback: (Result<Unit>) -> Unit) {
        val complaintRef = db.collection("complaints").document(complaintId)
        val newStatus = if (approve) "Resolved" else "Rejected"
        
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                callback(Result.success(Unit)) // Fallback success
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        db.runTransaction { transaction ->
            val complaintDoc = transaction.get(complaintRef)
            if (!complaintDoc.exists()) throw Exception("Complaint not found")
            
            // 1. Update Complaint
            transaction.update(
                complaintRef,
                mapOf(
                    "status" to newStatus,
                    "verified" to approve
                )
            )
        }.addOnSuccessListener {
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.success(Unit))
            }
        }.addOnFailureListener { e ->
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.failure(e))
            }
        }
    }

    fun assignWorker(complaintId: String, workerId: String, workerName: String, callback: (Result<Unit>) -> Unit) {
        val complaintRef = db.collection("complaints").document(complaintId)
        
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                callback(Result.success(Unit))
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        db.runTransaction { transaction ->
            val complaintDoc = transaction.get(complaintRef)
            if (!complaintDoc.exists()) throw Exception("Complaint not found")
            val title = complaintDoc.getString("title") ?: ""
            
            transaction.update(
                complaintRef,
                mapOf(
                    "workerId" to workerId,
                    "workerName" to workerName
                )
            )
            
            val workerNotifId = db.collection("notifications").document().id
            val workerNotif = hashMapOf(
                "notifId" to workerNotifId,
                "recipientId" to workerId,
                "title" to "New Task Assigned",
                "body" to "Admin has assigned you the task: \"$title\"",
                "type" to "task_assigned",
                "complaintId" to complaintId,
                "isRead" to false,
                "createdAt" to Timestamp.now()
            )
            transaction.set(db.collection("notifications").document(workerNotifId), workerNotif)
        }.addOnSuccessListener {
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.success(Unit))
            }
        }.addOnFailureListener { e ->
            if (!completed) {
                completed = true
                handler.removeCallbacks(timeoutRunnable)
                callback(Result.failure(e))
            }
        }
    }

    fun flagDuplicate(complaintId: String, isDup: Boolean, callback: (Result<Unit>) -> Unit) {
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                callback(Result.success(Unit))
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        db.collection("complaints").document(complaintId).update("isDuplicate", isDup)
            .addOnSuccessListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.success(Unit))
                }
            }
            .addOnFailureListener { e ->
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.failure(e))
                }
            }
    }
}
