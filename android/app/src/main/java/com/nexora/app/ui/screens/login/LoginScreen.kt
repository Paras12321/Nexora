package com.nexora.app.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexora.app.NexoraApp
import com.nexora.app.ui.components.NexoraButton
import com.nexora.app.ui.components.NexoraTextField

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory((androidx.compose.ui.platform.LocalContext.current.applicationContext as NexoraApp).authRepository)
    )
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEXORA",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
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
            Spacer(modifier = Modifier.height(16.dp))
            
            NexoraTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = null
                    viewModel.clearError()
                },
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordError != null,
                errorMessage = passwordError
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clickable { onNavigateToForgotPassword() }
                        .padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            NexoraButton(
                text = "Sign In",
                isLoading = uiState.isLoading,
                onClick = {
                    var hasError = false
                    if (email.isBlank()) {
                        emailError = "Email is required"
                        hasError = true
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = "Invalid email format"
                        hasError = true
                    }
                    
                    if (password.isBlank()) {
                        passwordError = "Password is required"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        viewModel.login(email, password)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToRegister() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "Don't have an account? ",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Register",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
