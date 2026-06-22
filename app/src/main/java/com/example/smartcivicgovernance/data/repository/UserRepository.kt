package com.example.smartcivicgovernance.data.repository

import com.example.smartcivicgovernance.data.model.User
import com.example.smartcivicgovernance.data.remote.FirebaseHelper
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthResult

class UserRepository {

    private val auth = FirebaseHelper.auth
    private val db = FirebaseHelper.db

    fun login(email: String, password: String, callback: (Result<AuthResult>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    FirebaseHelper.updateFCMToken()
                    callback(Result.success(task.result))
                } else {
                    callback(Result.failure(task.exception ?: Exception("Login failed")))
                }
            }
    }

    fun register(user: User, password: String, callback: (Result<AuthResult>) -> Unit) {
        auth.createUserWithEmailAndPassword(user.email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = task.result.user?.uid ?: ""
                    val updatedUser = user.copy(
                        uid = uid,
                        createdAt = Timestamp.now(),
                        disabled = false
                    )
                    
                    var completed = false
                    val handler = android.os.Handler(android.os.Looper.getMainLooper())
                    val timeoutRunnable = Runnable {
                        if (!completed) {
                            completed = true
                            if (user.role == "worker") {
                                initializeWorkerProfile(uid, user.name)
                            }
                            FirebaseHelper.updateFCMToken()
                            callback(Result.success(task.result))
                        }
                    }
                    handler.postDelayed(timeoutRunnable, 3000)
                    
                    // Save user details in Firestore
                    db.collection("users").document(uid).set(updatedUser)
                        .addOnSuccessListener {
                            if (!completed) {
                                completed = true
                                handler.removeCallbacks(timeoutRunnable)
                                if (user.role == "worker") {
                                    initializeWorkerProfile(uid, user.name)
                                }
                                FirebaseHelper.updateFCMToken()
                                callback(Result.success(task.result))
                            }
                        }
                        .addOnFailureListener { e ->
                            if (!completed) {
                                completed = true
                                handler.removeCallbacks(timeoutRunnable)
                                callback(Result.failure(e))
                            }
                        }
                } else {
                    callback(Result.failure(task.exception ?: Exception("Registration failed")))
                }
            }
    }

    fun loginWithGoogle(idToken: String, selectedRole: String, callback: (Result<AuthResult>) -> Unit) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val authResult = task.result
                    val firebaseUser = authResult.user
                    val uid = firebaseUser?.uid ?: ""
                    
                    FirebaseHelper.getDocWithTimeout(db.collection("users").document(uid), 3000) { checkResult ->
                        checkResult.fold(
                            onSuccess = { document ->
                                if (document.exists()) {
                                    FirebaseHelper.updateFCMToken()
                                    callback(Result.success(authResult))
                                } else {
                                    val newUser = User(
                                        uid = uid,
                                        name = firebaseUser?.displayName ?: "Google User",
                                        email = firebaseUser?.email ?: "",
                                        role = selectedRole,
                                        photoUrl = firebaseUser?.photoUrl?.toString() ?: "https://api.dicebear.com/7.x/adventurer/svg?seed=$uid",
                                        createdAt = com.google.firebase.Timestamp.now(),
                                        disabled = false
                                    )
                                    
                                    db.collection("users").document(uid).set(newUser)
                                        .addOnSuccessListener {
                                            if (selectedRole == "worker") {
                                                initializeWorkerProfile(uid, newUser.name)
                                            }
                                            FirebaseHelper.updateFCMToken()
                                            callback(Result.success(authResult))
                                        }
                                        .addOnFailureListener { e ->
                                            callback(Result.failure(e))
                                        }
                                }
                            },
                            onFailure = {
                                val newUser = User(
                                    uid = uid,
                                    name = firebaseUser?.displayName ?: "Google User",
                                    email = firebaseUser?.email ?: "",
                                    role = selectedRole,
                                    photoUrl = firebaseUser?.photoUrl?.toString() ?: "https://api.dicebear.com/7.x/adventurer/svg?seed=$uid",
                                    createdAt = com.google.firebase.Timestamp.now(),
                                    disabled = false
                                )
                                db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener {
                                        if (selectedRole == "worker") {
                                            initializeWorkerProfile(uid, newUser.name)
                                        }
                                        FirebaseHelper.updateFCMToken()
                                        callback(Result.success(authResult))
                                    }
                                    .addOnFailureListener { e ->
                                        callback(Result.failure(e))
                                    }
                            }
                        )
                    }
                } else {
                    callback(Result.failure(task.exception ?: Exception("Google Sign-In failed")))
                }
            }
    }

    fun loginWithPhoneCredential(credential: com.google.firebase.auth.PhoneAuthCredential, selectedRole: String, callback: (Result<AuthResult>) -> Unit) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val authResult = task.result
                    val firebaseUser = authResult.user
                    val uid = firebaseUser?.uid ?: ""
                    
                    FirebaseHelper.getDocWithTimeout(db.collection("users").document(uid), 3000) { checkResult ->
                        checkResult.fold(
                            onSuccess = { document ->
                                if (document.exists()) {
                                    FirebaseHelper.updateFCMToken()
                                    callback(Result.success(authResult))
                                } else {
                                    val phoneNum = firebaseUser?.phoneNumber ?: ""
                                    val displayPhoneName = "Phone User " + (if (phoneNum.length >= 4) phoneNum.takeLast(4) else "XXXX")
                                    val newUser = User(
                                        uid = uid,
                                        name = displayPhoneName,
                                        email = phoneNum,
                                        role = selectedRole,
                                        photoUrl = "https://api.dicebear.com/7.x/adventurer/svg?seed=$uid",
                                        createdAt = com.google.firebase.Timestamp.now(),
                                        disabled = false
                                    )
                                    
                                    db.collection("users").document(uid).set(newUser)
                                        .addOnSuccessListener {
                                            if (selectedRole == "worker") {
                                                initializeWorkerProfile(uid, newUser.name)
                                            }
                                            FirebaseHelper.updateFCMToken()
                                            callback(Result.success(authResult))
                                        }
                                        .addOnFailureListener { e ->
                                            callback(Result.failure(e))
                                        }
                                }
                            },
                            onFailure = {
                                val phoneNum = firebaseUser?.phoneNumber ?: ""
                                val displayPhoneName = "Phone User " + (if (phoneNum.length >= 4) phoneNum.takeLast(4) else "XXXX")
                                val newUser = User(
                                    uid = uid,
                                    name = displayPhoneName,
                                    email = phoneNum,
                                    role = selectedRole,
                                    photoUrl = "https://api.dicebear.com/7.x/adventurer/svg?seed=$uid",
                                    createdAt = com.google.firebase.Timestamp.now(),
                                    disabled = false
                                )
                                db.collection("users").document(uid).set(newUser)
                                    .addOnSuccessListener {
                                        if (selectedRole == "worker") {
                                            initializeWorkerProfile(uid, newUser.name)
                                        }
                                        FirebaseHelper.updateFCMToken()
                                        callback(Result.success(authResult))
                                    }
                                    .addOnFailureListener { e ->
                                        callback(Result.failure(e))
                                    }
                            }
                        )
                    }
                } else {
                    callback(Result.failure(task.exception ?: Exception("Phone verification failed")))
                }
            }
    }


    private fun initializeWorkerProfile(uid: String, name: String) {
        val workerMap = hashMapOf(
            "uid" to uid,
            "name" to name,
            "totalPoints" to 0,
            "issuesSolved" to 0,
            "activeTasks" to 0,
            "averageResolutionTimeMinutes" to 0.0,
            "averageRating" to 0.0,
            "rank" to 99,
            "badges" to emptyList<String>(),
            "joinedAt" to Timestamp.now()
        )
        db.collection("workers").document(uid).set(workerMap)
    }

    fun logout() {
        auth.signOut()
    }

    fun forgotPassword(email: String, callback: (Result<Unit>) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    callback(Result.success(Unit))
                } else {
                    callback(Result.failure(task.exception ?: Exception("Password reset failed")))
                }
            }
    }

    fun fetchUserProfile(uid: String, callback: (Result<User>) -> Unit) {
        FirebaseHelper.getDocWithTimeout(db.collection("users").document(uid), 3000) { result ->
            result.fold(
                onSuccess = { doc ->
                    val user = doc.toObject(User::class.java)
                    if (user != null) {
                        callback(Result.success(user))
                    } else {
                        callback(Result.failure(Exception("User not found")))
                    }
                },
                onFailure = { e ->
                    callback(Result.failure(e))
                }
            )
        }
    }
}
