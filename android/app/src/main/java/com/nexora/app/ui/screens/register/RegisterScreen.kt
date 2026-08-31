package com.nexora.app.ui.screens.register

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import com.nexora.app.ui.screens.login.AuthViewModel
import com.nexora.app.ui.screens.login.AuthViewModelFactory

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory((androidx.compose.ui.platform.LocalContext.current.applicationContext as NexoraApp).authRepository)
    )
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    
    val uiState by viewModel.uiState.collectAsState()
    
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var consentError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onRegisterSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Join the NEXORA ecosystem",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                NexoraTextField(
                    value = firstName,
                    onValueChange = { 
                        firstName = it
                        firstNameError = null
                    },
                    label = "First Name",
                    modifier = Modifier.weight(1f),
                    isError = firstNameError != null,
                    errorMessage = firstNameError
                )
                Spacer(modifier = Modifier.width(8.dp))
                NexoraTextField(
                    value = lastName,
                    onValueChange = { 
                        lastName = it
                        lastNameError = null
                    },
                    label = "Last Name",
                    modifier = Modifier.weight(1f),
                    isError = lastNameError != null,
                    errorMessage = lastNameError
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
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
                },
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordError != null,
                errorMessage = passwordError
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            NexoraTextField(
                value = confirmPassword,
                onValueChange = { 
                    confirmPassword = it
                    confirmPasswordError = null
                },
                label = "Confirm Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = confirmPasswordError != null,
                errorMessage = confirmPasswordError
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = consent,
                    onCheckedChange = { 
                        consent = it
                        consentError = null
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "I agree to the processing of my home data for AI optimization.",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (consentError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.clickable { 
                        consent = !consent
                        consentError = null
                    }
                )
            }
            if (consentError != null) {
                Text(
                    text = consentError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.Start).padding(start = 12.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            NexoraButton(
                text = "Register",
                isLoading = uiState.isLoading,
                onClick = {
                    var hasError = false
                    if (firstName.isBlank()) {
                        firstNameError = "First name required"
                        hasError = true
                    }
                    if (lastName.isBlank()) {
                        lastNameError = "Last name required"
                        hasError = true
                    }
                    if (email.isBlank()) {
                        emailError = "Email is required"
                        hasError = true
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        emailError = "Invalid email format"
                        hasError = true
                    }
                    if (password.length < 8) {
                        passwordError = "Password must be at least 8 characters"
                        hasError = true
                    }
                    if (password != confirmPassword) {
                        confirmPasswordError = "Passwords do not match"
                        hasError = true
                    }
                    if (!consent) {
                        consentError = "You must consent to proceed"
                        hasError = true
                    }
                    
                    if (!hasError) {
                        viewModel.register(firstName, lastName, email, password)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLogin() }
                    .padding(16.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Sign In",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
