package com.example.smartcivicgovernance.ui.worker

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.data.repository.ComplaintRepository
import com.example.smartcivicgovernance.data.repository.WorkerRepository
import com.example.smartcivicgovernance.data.remote.FirebaseHelper

import com.google.firebase.firestore.ListenerRegistration

class WorkerViewModel : ViewModel() {

    private val complaintRepo = ComplaintRepository()
    private val workerRepo = WorkerRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _availableTasks = MutableLiveData<List<Complaint>>()
    val availableTasks: LiveData<List<Complaint>> get() = _availableTasks

    private val _activeTasks = MutableLiveData<List<Complaint>>()
    val activeTasks: LiveData<List<Complaint>> get() = _activeTasks

    private val _workerStats = MutableLiveData<Worker>()
    val workerStats: LiveData<Worker> get() = _workerStats

    private val _actionSuccess = MutableLiveData<Boolean>()
    val actionSuccess: LiveData<Boolean> get() = _actionSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private var availableTasksListener: ListenerRegistration? = null
    private var activeTasksListener: ListenerRegistration? = null

    fun loadTasks() {
        val uid = FirebaseHelper.getCurrentUid() ?: return
        _loading.value = true
        _error.value = null
        
        availableTasksListener?.remove()
        availableTasksListener = FirebaseHelper.db.collection("complaints")
            .whereEqualTo("status", "Pending")
            .addSnapshotListener { snapshot, e ->
                _loading.value = false
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val avList = snapshot.toObjects(Complaint::class.java).filter {
                        it.workerId.isNullOrEmpty() || it.workerId == uid
                    }
                    _availableTasks.value = avList
                }
            }
            
        activeTasksListener?.remove()
        activeTasksListener = FirebaseHelper.db.collection("complaints")
            .whereEqualTo("status", "In Progress")
            .whereEqualTo("workerId", uid)
            .addSnapshotListener { snapshot, e ->
                _loading.value = false
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val acList = snapshot.toObjects(Complaint::class.java)
                    _activeTasks.value = acList
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        availableTasksListener?.remove()
        activeTasksListener?.remove()
    }

    fun loadWorkerStats() {
        val uid = FirebaseHelper.getCurrentUid() ?: return
        _loading.value = true
        _error.value = null
        workerRepo.fetchWorkerDetails(uid) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { stats -> _workerStats.value = stats },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun acceptTask(complaintId: String, workerName: String) {
        val uid = FirebaseHelper.getCurrentUid() ?: return
        _loading.value = true
        _error.value = null
        complaintRepo.acceptComplaint(complaintId, uid, workerName) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { _actionSuccess.value = true },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun resolveTask(complaintId: String, proofUri: Uri, workerNotes: String? = null) {
        _loading.value = true
        _error.value = null
        complaintRepo.resolveComplaint(complaintId, proofUri, workerNotes) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { _actionSuccess.value = true },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }
}
