package com.example.smartcivicgovernance.data.repository

import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.data.model.toWorkerSafe
import com.example.smartcivicgovernance.data.model.toWorkersSafe
import com.example.smartcivicgovernance.data.remote.FirebaseHelper

class WorkerRepository {

    private val db = FirebaseHelper.db

    fun fetchWorkerDetails(workerId: String, callback: (Result<Worker>) -> Unit) {
        FirebaseHelper.getDocWithTimeout(db.collection("workers").document(workerId), 3000) { result ->
            result.fold(
                onSuccess = { doc ->
                    val worker = doc.toWorkerSafe()
                    if (worker != null) {
                        callback(Result.success(worker))
                    } else {
                        callback(Result.failure(Exception("Worker profile not found")))
                    }
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }

    fun fetchWorkerLeaderboard(callback: (Result<List<Worker>>) -> Unit) {
        val query = db.collection("workers")
            .orderBy("totalPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
        FirebaseHelper.getQueryWithTimeout(query, 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val workers = snapshot.toWorkersSafe()
                    callback(Result.success(workers))
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }

    fun fetchAllWorkers(callback: (Result<List<Worker>>) -> Unit) {
        FirebaseHelper.getQueryWithTimeout(db.collection("workers"), 3000) { result ->
            result.fold(
                onSuccess = { snapshot ->
                    val workers = snapshot.toWorkersSafe()
                    callback(Result.success(workers))
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }
}
