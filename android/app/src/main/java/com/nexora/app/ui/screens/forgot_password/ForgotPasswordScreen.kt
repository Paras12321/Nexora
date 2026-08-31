package com.nexora.app.ui.screens.forgot_password

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexora.app.NexoraApp
import com.nexora.app.ui.components.NexoraButton
import com.nexora.app.ui.components.NexoraTextField
import com.nexora.app.ui.screens.login.AuthViewModel
import com.nexora.app.ui.screens.login.AuthViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory((androidx.compose.ui.platform.LocalContext.current.applicationContext as NexoraApp).authRepository)
    )
) {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (uiState.resetEmailSent) Arrangement.Center else Arrangement.Top
        ) {
            if (!uiState.resetEmailSent) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Forgot your password?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Enter your email address and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                
                NexoraTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = null
                        viewModel.clearError()
                    },
                    label = "Email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    isError = emailError != null || uiState.error != null,
                    errorMessage = emailError ?: uiState.error
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                NexoraButton(
                    text = "Send Reset Link",
                    isLoading = uiState.isLoading,
                    onClick = {
                        if (email.isBlank()) {
                            emailError = "Email is required"
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            emailError = "Invalid email format"
                        } else {
                            viewModel.resetPassword(email)
                        }
                    }
                )
            } else {
                Text(
                    text = "Check your email",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "If an account exists for $email, you will receive a password reset link shortly.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                NexoraButton(
                    text = "Back to Login",
                    onClick = onNavigateBack
                )
            }
        }
    }
}
