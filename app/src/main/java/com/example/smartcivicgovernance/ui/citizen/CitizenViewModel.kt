package com.example.smartcivicgovernance.ui.citizen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.model.Worker
import com.example.smartcivicgovernance.data.repository.ComplaintRepository
import com.example.smartcivicgovernance.data.repository.WorkerRepository
import com.example.smartcivicgovernance.data.remote.FirebaseHelper

import com.google.firebase.firestore.ListenerRegistration

class CitizenViewModel : ViewModel() {

    private val complaintRepo = ComplaintRepository()
    private val workerRepo = WorkerRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _complaints = MutableLiveData<List<Complaint>>()
    val complaints: LiveData<List<Complaint>> get() = _complaints

    private val _leaderboard = MutableLiveData<List<Worker>>()
    val leaderboard: LiveData<List<Worker>> get() = _leaderboard

    private val _operationSuccess = MutableLiveData<Boolean>()
    val operationSuccess: LiveData<Boolean> get() = _operationSuccess

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private var complaintsListener: ListenerRegistration? = null

    fun loadCitizenComplaints() {
        val uid = FirebaseHelper.getCurrentUid() ?: return
        _loading.value = true
        _error.value = null
        complaintsListener?.remove()
        complaintsListener = FirebaseHelper.db.collection("complaints")
            .whereEqualTo("citizenId", uid)
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

    override fun onCleared() {
        super.onCleared()
        complaintsListener?.remove()
    }

    fun loadLeaderboard() {
        _loading.value = true
        _error.value = null
        workerRepo.fetchWorkerLeaderboard { result ->
            _loading.value = false
            result.fold(
                onSuccess = { list -> _leaderboard.value = list },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun submitComplaintRating(complaintId: String, rating: Int, feedback: String) {
        _loading.value = true
        _error.value = null
        complaintRepo.submitRating(complaintId, rating, feedback) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { _operationSuccess.value = true },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }
}
