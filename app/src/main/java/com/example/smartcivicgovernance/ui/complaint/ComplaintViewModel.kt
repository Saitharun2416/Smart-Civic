package com.example.smartcivicgovernance.ui.complaint

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartcivicgovernance.data.model.Complaint
import com.example.smartcivicgovernance.data.repository.ComplaintRepository
import com.example.smartcivicgovernance.data.remote.FirebaseHelper

class ComplaintViewModel : ViewModel() {

    private val repository = ComplaintRepository()

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    private val _reportSuccess = MutableLiveData<String?>()
    val reportSuccess: LiveData<String?> get() = _reportSuccess

    private val _complaintDetails = MutableLiveData<Complaint?>()
    val complaintDetails: LiveData<Complaint?> get() = _complaintDetails

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun reportNewComplaint(
        title: String,
        description: String,
        category: String,
        imageUri: Uri?,
        latitude: Double,
        longitude: Double,
        address: String
    ) {
        _loading.value = true
        _error.value = null
        repository.reportComplaint(title, description, category, imageUri, latitude, longitude, address) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { complaintId -> _reportSuccess.value = complaintId },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun loadComplaintDetails(complaintId: String) {
        _loading.value = true
        _error.value = null
        FirebaseHelper.getDocWithTimeout(FirebaseHelper.db.collection("complaints").document(complaintId), 3000) { result ->
            _loading.value = false
            result.fold(
                onSuccess = { doc ->
                    val complaint = doc.toObject(Complaint::class.java)
                    _complaintDetails.value = complaint
                },
                onFailure = { e ->
                    _error.value = e.message
                }
            )
        }
    }
}
