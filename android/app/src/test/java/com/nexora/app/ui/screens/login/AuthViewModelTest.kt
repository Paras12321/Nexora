package com.nexora.app.ui.screens.login

import com.nexora.app.data.model.AuthResponse
import com.nexora.app.data.model.DetailResponse
import com.nexora.app.data.model.LoginRequest
import com.nexora.app.data.model.RegisterRequest
import com.nexora.app.data.model.UserDto
import com.nexora.app.data.remote.NetworkError
import com.nexora.app.data.remote.NetworkResult
import com.nexora.app.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    private class FakeAuthRepository : AuthRepository {
        var loginResult: NetworkResult<AuthResponse> = NetworkResult.Error(NetworkError.HttpError(400, "Invalid credentials"))
        var registerResult: NetworkResult<AuthResponse> = NetworkResult.Error(NetworkError.HttpError(400, "Invalid request"))
        var resetPasswordResult: NetworkResult<DetailResponse> = NetworkResult.Success(DetailResponse("OK"))
        var loggedIn: Boolean = false
        var lastCapturedLoginRequest: LoginRequest? = null

        override suspend fun register(request: RegisterRequest): NetworkResult<AuthResponse> = registerResult

        override suspend fun login(request: LoginRequest): NetworkResult<AuthResponse> {
            lastCapturedLoginRequest = request
            return loginResult
        }

        override suspend fun logout(): NetworkResult<Unit> {
            loggedIn = false
            return NetworkResult.Success(Unit)
        }

        override suspend fun resetPassword(email: String): NetworkResult<DetailResponse> = resetPasswordResult

        override fun isLoggedIn(): Boolean = loggedIn
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login success sets isSuccess to true`() = runTest {
        fakeRepository.loginResult = NetworkResult.Success(
            AuthResponse(token = "valid_token", user = UserDto(1, "test@nexora.com"))
        )

        viewModel.login("test@nexora.com", "password123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSuccess)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `login http 400 error sets user friendly error message in uiState`() = runTest {
        fakeRepository.loginResult = NetworkResult.Error(
            NetworkError.HttpError(400, "Unable to log in with provided credentials.")
        )

        viewModel.login("user@nexora.com", "wrongpassword")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSuccess)
        assertFalse(state.isLoading)
        assertEquals("Unable to log in with provided credentials.", state.error)
    }

    @Test
    fun `login trims whitespace from input email and password`() = runTest {
        fakeRepository.loginResult = NetworkResult.Success(
            AuthResponse(token = "valid_token", user = UserDto(1, "test@nexora.com"))
        )

        viewModel.login("  test@nexora.com  ", "  password123  ")
        testDispatcher.scheduler.advanceUntilIdle()

        val captured = fakeRepository.lastCapturedLoginRequest
        assertNotNull(captured)
        assertEquals("test@nexora.com", captured?.email)
        assertEquals("test@nexora.com", captured?.username)
        assertEquals("password123", captured?.password)
    }

    @Test
    fun `clearError clears error in uiState`() = runTest {
        fakeRepository.loginResult = NetworkResult.Error(
            NetworkError.HttpError(400, "Error 400")
        )

        viewModel.login("user@nexora.com", "pass")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Error 400", viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
