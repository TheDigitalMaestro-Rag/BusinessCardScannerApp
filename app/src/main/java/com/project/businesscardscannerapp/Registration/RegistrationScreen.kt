package com.project.businesscardscannerapp.Registration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import java.net.URLEncoder

// In RegistrationScreen.kt - replace the existing function
fun String.encodeToEmailKey(): String {
    return this.replace(".", ",")  // Consistent with ProvideDB
}
@Composable
fun AuthScreen(
    auth: FirebaseAuth,
    database: FirebaseDatabase,
    onAuthComplete: () -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }

    if (isLoginMode) {
        LoginScreen(
            auth = auth,
            onLoginComplete = onAuthComplete,
            onSwitchToRegister = { isLoginMode = false }
        )
    } else {
        RegistrationScreen(
            auth = auth,
            database = database,
            onRegistrationComplete = onAuthComplete,
            onSwitchToLogin = { isLoginMode = true }
        )
    }
}

@Composable
fun LoginScreen(
    auth: FirebaseAuth,
    onLoginComplete: () -> Unit,
    onSwitchToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign In", style = MaterialTheme.typography.h4)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error)
        }

        Button(
            onClick = {
                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onLoginComplete()
                        } else {
                            isLoading = false
                            errorMessage = task.exception?.message ?: "Login failed"
                        }
                    }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Sign In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToRegister) {
            Text("Don't have an account? Register here")
        }
    }
}

@Composable
fun RegistrationScreen(
    auth: FirebaseAuth,
    database: FirebaseDatabase,
    onRegistrationComplete: () -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Create Account", style = MaterialTheme.typography.h4)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") }
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error)
        }

        Button(
            onClick = {
                isLoading = true
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Save user data to Realtime Database
                            val user = auth.currentUser
                            val userData = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "createdAt" to ServerValue.TIMESTAMP
                            )

                            // In RegistrationScreen.kt - add after user creation
                            database.reference.child("users").child(user!!.uid)
                                .setValue(userData)
                                .addOnCompleteListener {
                                    // Register user email for lookup
                                    database.reference.child("user_emails")
                                        .child(email.encodeToEmailKey())
                                        .setValue(user.uid)
                                        .addOnCompleteListener {
                                            isLoading = false
                                            onRegistrationComplete()
                                        }
                                }
                        } else {
                            isLoading = false
                            errorMessage = task.exception?.message ?: "Registration failed"
                        }
                    }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else {
                Text("Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToLogin) {
            Text("Already have an account? Sign in here")
        }
    }
}

