package com.nexora.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.model.LoginRequest
import com.nexora.app.data.model.RegisterRequest
import com.nexora.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val resetEmailSent: Boolean = false
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = repository.login(LoginRequest(email = trimmedEmail, username = trimmedEmail, password = trimmedPassword))) {
                is NetworkResult.Success -> {
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState(error = result.error.message)
                }
                else -> {}
            }
        }
    }

    fun register(firstName: String, lastName: String, email: String, password: String) {
        val trimmedFirstName = firstName.trim()
        val trimmedLastName = lastName.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val request = RegisterRequest(
                email = trimmedEmail,
                password = trimmedPassword,
                firstName = trimmedFirstName,
                lastName = trimmedLastName,
                username = trimmedEmail
            )
            when (val result = repository.register(request)) {
                is NetworkResult.Success -> {
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState(error = result.error.message)
                }
                else -> {}
            }
        }
    }

    fun resetPassword(email: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = repository.resetPassword(email)) {
                is NetworkResult.Success -> {
                    _uiState.value = AuthUiState(resetEmailSent = true)
                }
                is NetworkResult.Error -> {
                    _uiState.value = AuthUiState(error = result.error.message)
                }
                else -> {}
            }
        }
    }

    fun logout() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            repository.logout()
            _uiState.value = AuthUiState(isSuccess = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun resetState() {
        _uiState.value = AuthUiState()
    }
}
