package com.nexora.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.HomeRepository
import com.nexora.app.data.repository.RoomRepository
import com.nexora.app.domain.model.HomeMemberModel
import com.nexora.app.domain.model.HomeModel
import com.nexora.app.domain.model.RoomModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val homes: List<HomeModel> = emptyList(),
    val selectedHome: HomeModel? = null,
    val members: List<HomeMemberModel> = emptyList(),
    val rooms: List<RoomModel> = emptyList(),
    val errorMessage: String? = null,
    val userFeedbackMessage: String? = null
)

class HomeViewModel(
    private val homeRepository: HomeRepository,
    private val roomRepository: RoomRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomes()
    }

    fun loadHomes() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = homeRepository.getHomes()) {
                is NetworkResult.Success -> {
                    val homes = result.data
                    val currentSelected = _uiState.value.selectedHome
                    val newSelected = if (currentSelected != null && homes.any { it.id == currentSelected.id }) {
                        homes.first { it.id == currentSelected.id }
                    } else {
                        homes.firstOrNull()
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        homes = homes,
                        selectedHome = newSelected,
                        errorMessage = null
                    )
                    if (newSelected != null) {
                        loadHomeDetails(newSelected.id)
                    } else {
                        _uiState.value = _uiState.value.copy(members = emptyList(), rooms = emptyList())
                    }
                }
                is NetworkResult.Error -> {
                    val msg = formatErrorMessage(result.error)
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = msg)
                }
                is NetworkResult.Loading -> {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun selectHome(home: HomeModel) {
        _uiState.value = _uiState.value.copy(selectedHome = home)
        loadHomeDetails(home.id)
    }

    fun loadHomeDetails(homeId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Load members and rooms in parallel
            val membersResult = homeRepository.getHomeMembers(homeId)
            val roomsResult = roomRepository.getRooms(homeId)

            val updatedMembers = if (membersResult is NetworkResult.Success) membersResult.data else emptyList()
            val updatedRooms = if (roomsResult is NetworkResult.Success) roomsResult.data else emptyList()

            var errorMsg: String? = null
            if (membersResult is NetworkResult.Error) {
                errorMsg = formatErrorMessage(membersResult.error)
            } else if (roomsResult is NetworkResult.Error) {
                errorMsg = formatErrorMessage(roomsResult.error)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                members = updatedMembers,
                rooms = updatedRooms,
                errorMessage = errorMsg
            )
        }
    }

    fun createHome(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Home name cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = homeRepository.createHome(trimmedName)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        userFeedbackMessage = "Home '${result.data.name}' created successfully!"
                    )
                    loadHomes()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(result.error)
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun inviteMember(email: String) {
        val selectedHome = _uiState.value.selectedHome ?: return
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = homeRepository.inviteMember(selectedHome.id, trimmedEmail)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        userFeedbackMessage = "Invited '$trimmedEmail' successfully!"
                    )
                    loadHomeDetails(selectedHome.id)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(result.error)
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun removeMember(memberId: Int) {
        val selectedHome = _uiState.value.selectedHome ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = homeRepository.removeMember(selectedHome.id, memberId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(userFeedbackMessage = "Member removed.")
                    loadHomeDetails(selectedHome.id)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(result.error)
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun leaveHome() {
        val selectedHome = _uiState.value.selectedHome ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = homeRepository.leaveHome(selectedHome.id)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(userFeedbackMessage = result.data)
                    loadHomes()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(result.error)
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun createRoom(name: String, description: String) {
        val selectedHome = _uiState.value.selectedHome ?: return
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Room name cannot be empty.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = roomRepository.createRoom(selectedHome.id, trimmedName, description.ifBlank { null })) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        userFeedbackMessage = "Room '${result.data.name}' created!"
                    )
                    loadHomeDetails(selectedHome.id)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = formatErrorMessage(result.error)
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(userFeedbackMessage = null, errorMessage = null)
    }

    private fun formatErrorMessage(error: NetworkError): String {
        return when (error) {
            is NetworkError.HttpError -> {
                when (error.statusCode) {
                    401 -> "Session expired. Please log in again."
                    403 -> error.serverMessage ?: "Permission denied. Only home owners can perform this action."
                    404 -> error.serverMessage ?: "Resource not found."
                    400, 409 -> error.serverMessage ?: "Invalid request or item already exists."
                    else -> error.serverMessage ?: "Server error (${error.statusCode})."
                }
            }
            is NetworkError.ConnectivityError -> error.userFriendlyMessage
            is NetworkError.SerializationError -> error.userFriendlyMessage
            is NetworkError.UnknownError -> error.userFriendlyMessage
        }
    }
}

class HomeViewModelFactory(
    private val homeRepository: HomeRepository,
    private val roomRepository: RoomRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(homeRepository, roomRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
