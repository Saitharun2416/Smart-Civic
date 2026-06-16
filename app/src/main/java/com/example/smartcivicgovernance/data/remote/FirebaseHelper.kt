package com.example.smartcivicgovernance.data.remote

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import java.io.File
import com.google.firebase.FirebaseApp

object FirebaseHelper {

    val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    val messaging: FirebaseMessaging
        get() = FirebaseMessaging.getInstance()

    fun getCurrentUid(): String? {
        return auth.currentUser?.uid
    }

    fun getDocWithTimeout(
        docRef: DocumentReference,
        timeoutMs: Long = 3000,
        callback: (Result<DocumentSnapshot>) -> Unit
    ) {
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                docRef.get(Source.CACHE)
                    .addOnSuccessListener { callback(Result.success(it)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)
        
        docRef.get()
            .addOnSuccessListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.success(it))
                }
            }
            .addOnFailureListener { e ->
                if (!completed) {
                    docRef.get(Source.CACHE)
                        .addOnSuccessListener {
                            completed = true
                            handler.removeCallbacks(timeoutRunnable)
                            callback(Result.success(it))
                        }
                        .addOnFailureListener {
                            completed = true
                            handler.removeCallbacks(timeoutRunnable)
                            callback(Result.failure(e))
                        }
                }
            }
    }

    fun getQueryWithTimeout(
        query: Query,
        timeoutMs: Long = 3000,
        callback: (Result<QuerySnapshot>) -> Unit
    ) {
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                query.get(Source.CACHE)
                    .addOnSuccessListener { callback(Result.success(it)) }
                    .addOnFailureListener { callback(Result.failure(it)) }
            }
        }
        handler.postDelayed(timeoutRunnable, timeoutMs)
        
        query.get()
            .addOnSuccessListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    callback(Result.success(it))
                }
            }
            .addOnFailureListener { e ->
                if (!completed) {
                    query.get(Source.CACHE)
                        .addOnSuccessListener {
                            completed = true
                            handler.removeCallbacks(timeoutRunnable)
                            callback(Result.success(it))
                        }
                        .addOnFailureListener {
                            completed = true
                            handler.removeCallbacks(timeoutRunnable)
                            callback(Result.failure(e))
                        }
                }
            }
    }

    fun getCurrentUserRole(onResult: (String?) -> Unit) {
        val uid = getCurrentUid()
        if (uid == null) {
            onResult(null)
            return
        }
        getDocWithTimeout(db.collection("users").document(uid), 3000) { result ->
            result.fold(
                onSuccess = { document ->
                    if (document.exists()) {
                        onResult(document.getString("role"))
                    } else {
                        onResult(null)
                    }
                },
                onFailure = {
                    onResult(null)
                }
            )
        }
    }

    fun checkUserStatus(onResult: (role: String?, disabled: Boolean) -> Unit) {
        val uid = getCurrentUid()
        if (uid == null) {
            onResult(null, false)
            return
        }
        getDocWithTimeout(db.collection("users").document(uid), 3000) { result ->
            result.fold(
                onSuccess = { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        val disabled = document.getBoolean("disabled") ?: false
                        onResult(role, disabled)
                    } else {
                        onResult(null, false)
                    }
                },
                onFailure = {
                    onResult(null, false)
                }
            )
        }
    }

    fun uploadImage(folderName: String, fileUri: Uri, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        try {
            val context = FirebaseApp.getInstance().applicationContext
            val contentResolver = context.contentResolver
            
            // Create a local directory for images
            val localDir = File(context.filesDir, folderName)
            if (!localDir.exists()) {
                localDir.mkdirs()
            }
            
            val fileName = UUID.randomUUID().toString() + ".jpg"
            val localFile = File(localDir, fileName)
            
            contentResolver.openInputStream(fileUri).use { inputStream ->
                if (inputStream == null) {
                    onFailure(Exception("Failed to open input stream for Uri: $fileUri"))
                    return
                }
                localFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            val localUriString = Uri.fromFile(localFile).toString()
            onSuccess(localUriString)
        } catch (e: Exception) {
            onFailure(e)
        }
    }

    fun updateFCMToken() {
        val uid = getCurrentUid() ?: return
        messaging.token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection("users").document(uid).update("fcmToken", token)
                    .addOnSuccessListener {
                        consoleLog("FCM token updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        consoleLog("Failed to update FCM token: ${e.message}")
                    }
            }
        }
    }

    private fun consoleLog(message: String) {
        android.util.Log.d("FirebaseHelper", message)
    }
}
