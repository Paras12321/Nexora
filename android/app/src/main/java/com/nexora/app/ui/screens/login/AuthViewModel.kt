package com.nexora.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.remote.dto.LoginRequest
import com.nexora.app.data.remote.dto.RegisterRequest
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
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = repository.login(LoginRequest(email, password))) {
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
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val request = RegisterRequest(email, password, firstName, lastName)
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
