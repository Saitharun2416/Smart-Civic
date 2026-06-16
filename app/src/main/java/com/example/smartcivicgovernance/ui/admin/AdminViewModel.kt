package com.example.smartcivicgovernance.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.model.User
import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.data.repository.ComplaintRepository
import com.example.smartcivicgovernance.data.repository.WorkerRepository
import com.example.smartcivicgovernance.data.remote.FirebaseHelper

class AdminViewModel : ViewModel() {

    private val complaintRepo = ComplaintRepository()
    private val workerRepo = WorkerRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _complaints = MutableLiveData<List<Complaint>>()
    val complaints: LiveData<List<Complaint>> get() = _complaints

    private val _workers = MutableLiveData<List<Worker>>()
    val workers: LiveData<List<Worker>> get() = _workers

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> get() = _users

    private val _actionSuccess = MutableLiveData<Boolean>()
    val actionSuccess: LiveData<Boolean> get() = _actionSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private var usersListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var complaintsListener: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadAllComplaints() {
        _loading.value = true
        _error.value = null
        complaintsListener?.remove()
        complaintsListener = FirebaseHelper.db.collection("complaints")
            .addSnapshotListener { snapshot, e ->
                _loading.value = false
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Complaint::class.java)
                    _complaints.value = list
                }
            }
    }

    fun loadAllWorkers() {
        _loading.value = true
        _error.value = null
        workerRepo.fetchAllWorkers { result ->
            _loading.value = false
            result.fold(
                onSuccess = { list -> _workers.value = list },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun loadAllUsers() {
        _loading.value = true
        _error.value = null
        usersListener?.remove()
        usersListener = FirebaseHelper.db.collection("users")
            .addSnapshotListener { snapshot, e ->
                _loading.value = false
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(User::class.java)
                    _users.value = list
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
        complaintsListener?.remove()
    }

    fun verifyComplaintResolution(complaintId: String, approve: Boolean) {
        _loading.value = true
        _error.value = null
        complaintRepo.verifyComplaint(complaintId, approve) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { _actionSuccess.value = true },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun assignWorkerToComplaint(complaintId: String, workerId: String, workerName: String) {
        _loading.value = true
        _error.value = null
        complaintRepo.assignWorker(complaintId, workerId, workerName) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { _actionSuccess.value = true },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun setUserAccountStatus(userId: String, enable: Boolean) {
        _loading.value = true
        _error.value = null
        
        var completed = false
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val timeoutRunnable = Runnable {
            if (!completed) {
                completed = true
                _loading.value = false
                _actionSuccess.value = true
            }
        }
        handler.postDelayed(timeoutRunnable, 3000)

        FirebaseHelper.db.collection("users").document(userId).update("disabled", !enable)
            .addOnSuccessListener {
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    _loading.value = false
                    _actionSuccess.value = true
                }
            }
            .addOnFailureListener { e ->
                if (!completed) {
                    completed = true
                    handler.removeCallbacks(timeoutRunnable)
                    _loading.value = false
                    _error.value = e.message
                }
            }
    }

    fun resolveDuplicateComplaint(complaintId: String, dismiss: Boolean) {
        _loading.value = true
        _error.value = null
        // If dismiss is true, we remove the duplicate flag (isDuplicate=false)
        // If merge (dismiss=false), we can delete or mark as rejected
        val statusAction = if (dismiss) {
            complaintRepo.flagDuplicate(complaintId, false) { result ->
                _loading.value = false
                result.fold(
                    onSuccess = { _actionSuccess.value = true },
                    onFailure = { e -> _error.value = e.message }
                )
            }
        } else {
            FirebaseHelper.db.collection("complaints").document(complaintId).delete()
                .addOnSuccessListener {
                    _loading.value = false
                    _actionSuccess.value = true
                }
                .addOnFailureListener { e ->
                    _loading.value = false
                    _error.value = e.message
                }
        }
    }

    private val _errorState = MutableLiveData<String?>()
}
