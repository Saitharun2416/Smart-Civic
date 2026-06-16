package com.example.smartcivicgovernance.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartcivicgovernance.data.model.User
import com.example.smartcivicgovernance.data.repository.UserRepository
import com.google.firebase.auth.AuthResult

class AuthViewModel : ViewModel() {

    private val repository = UserRepository()

    private val _loadingState = MutableLiveData<Boolean>()
    val loadingState: LiveData<Boolean> get() = _loadingState

    private val _errorState = MutableLiveData<String?>()
    val errorState: LiveData<String?> get() = _errorState

    private val _authSuccess = MutableLiveData<AuthResult>()
    val authSuccess: LiveData<AuthResult> get() = _authSuccess

    private val _resetSuccess = MutableLiveData<Boolean>()
    val resetSuccess: LiveData<Boolean> get() = _resetSuccess

    private val _userProfile = MutableLiveData<User?>()
    val userProfile: LiveData<User?> get() = _userProfile

    fun login(email: String, password: String) {
        _loadingState.value = true
        _errorState.value = null
        repository.login(email, password) { result ->
            _loadingState.value = false
            result.fold(
                onSuccess = { authResult -> _authSuccess.value = authResult },
                onFailure = { exception -> _errorState.value = exception.message }
            )
        }
    }

    fun register(user: User, password: String) {
        _loadingState.value = true
        _errorState.value = null
        repository.register(user, password) { result ->
            _loadingState.value = false
            result.fold(
                onSuccess = { authResult -> _authSuccess.value = authResult },
                onFailure = { exception -> _errorState.value = exception.message }
            )
        }
    }

    fun forgotPassword(email: String) {
        _loadingState.value = true
        _errorState.value = null
        repository.forgotPassword(email) { result ->
            _loadingState.value = false
            result.fold(
                onSuccess = { _resetSuccess.value = true },
                onFailure = { exception -> _errorState.value = exception.message }
            )
        }
    }

    fun fetchProfile(uid: String) {
        repository.fetchUserProfile(uid) { result ->
            result.fold(
                onSuccess = { user -> _userProfile.value = user },
                onFailure = { _userProfile.value = null }
            )
        }
    }
}
