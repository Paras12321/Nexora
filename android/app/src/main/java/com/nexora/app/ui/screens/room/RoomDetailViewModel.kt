package com.nexora.app.ui.screens.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.HomeRepository
import com.nexora.app.data.repository.RoomRepository
import com.nexora.app.domain.model.HomeMemberModel
import com.nexora.app.domain.model.RoomMemberModel
import com.nexora.app.domain.model.RoomModel
import com.nexora.app.domain.model.RoomPreferenceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomDetailUiState(
    val isLoading: Boolean = false,
    val room: RoomModel? = null,
    val assignedMembers: List<RoomMemberModel> = emptyList(),
    val availableHomeMembers: List<HomeMemberModel> = emptyList(),
    val preference: RoomPreferenceModel? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class RoomDetailViewModel(
    private val homeId: Int,
    private val roomId: Int,
    private val roomRepository: RoomRepository,
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomDetailUiState())
    val uiState: StateFlow<RoomDetailUiState> = _uiState.asStateFlow()

    init {
        loadRoomData()
    }

    fun loadRoomData() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val roomResult = roomRepository.getRoomDetail(homeId, roomId)
            val assignedMembersResult = roomRepository.getRoomMembers(homeId, roomId)
            val homeMembersResult = homeRepository.getHomeMembers(homeId)
            val preferencesResult = roomRepository.getRoomPreferences(homeId, roomId)

            val roomData = if (roomResult is NetworkResult.Success) roomResult.data else null
            val assignedList = if (assignedMembersResult is NetworkResult.Success) assignedMembersResult.data else emptyList()
            val homeMembersList = if (homeMembersResult is NetworkResult.Success) homeMembersResult.data else emptyList()
            val preferenceData = if (preferencesResult is NetworkResult.Success) preferencesResult.data else null

            var errorMsg: String? = null
            if (roomResult is NetworkResult.Error) {
                errorMsg = formatErrorMessage(roomResult.error)
            } else if (assignedMembersResult is NetworkResult.Error) {
                errorMsg = formatErrorMessage(assignedMembersResult.error)
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                room = roomData,
                assignedMembers = assignedList,
                availableHomeMembers = homeMembersList,
                preference = preferenceData,
                errorMessage = errorMsg
            )
        }
    }

    fun assignMember(homeMemberId: Int) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = roomRepository.assignRoomMember(homeId, roomId, homeMemberId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Member assigned to room successfully!"
                    )
                    loadRoomData()
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

    fun updatePreferences(newPreferences: Map<String, String>) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = roomRepository.setRoomPreferences(homeId, roomId, newPreferences)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        preference = result.data,
                        successMessage = "Room preferences updated successfully!"
                    )
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
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    private fun formatErrorMessage(error: NetworkError): String {
        return error.message ?: "An unexpected error occurred."
    }
}

class RoomDetailViewModelFactory(
    private val homeId: Int,
    private val roomId: Int,
    private val roomRepository: RoomRepository,
    private val homeRepository: HomeRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoomDetailViewModel::class.java)) {
            return RoomDetailViewModel(homeId, roomId, roomRepository, homeRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
