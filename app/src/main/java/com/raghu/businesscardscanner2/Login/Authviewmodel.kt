package com.raghu.businesscardscanner2.Login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser: FirebaseUser? = auth.currentUser

    init {
        if (currentUser != null) {
            _authState.value = AuthState.Authenticated(currentUser)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun signInWithEmailAndPassword(email: String, password: String) = viewModelScope.launch {
        try {
            _authState.value = AuthState.Loading
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.let { user ->
                _authState.value = AuthState.Authenticated(user)
            } ?: throw Exception("Authentication failed")
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                when (e) {
                    is FirebaseAuthInvalidUserException -> "User not found"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                    else -> e.localizedMessage ?: "Authentication failed"
                }
            )
        }
    }

    fun createUserWithEmailAndPassword(email: String, password: String) = viewModelScope.launch {
        try {
            _authState.value = AuthState.Loading
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            authResult.user?.let { user ->
                _authState.value = AuthState.Authenticated(user)
            } ?: throw Exception("User creation failed")
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                when (e) {
                    is FirebaseAuthWeakPasswordException -> "Password is too weak"
                    is FirebaseAuthInvalidCredentialsException -> "Invalid email format"
                    is FirebaseAuthUserCollisionException -> "Email already in use"
                    else -> e.localizedMessage ?: "Registration failed"
                }
            )
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) = viewModelScope.launch {
        try {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            authResult.user?.let { user ->
                _authState.value = AuthState.Authenticated(user)
            } ?: throw Exception("Google authentication failed")
        } catch (e: Exception) {
            _authState.value = AuthState.Error(
                e.localizedMessage ?: "Google sign-in failed"
            )
        }
    }

    fun signOut() = viewModelScope.launch {
        try {
            auth.signOut()
            googleSignInClient.signOut().await()
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Sign out failed: ${e.localizedMessage}")
        }
    }

    sealed class AuthState {
        object Initial : AuthState()
        object Loading : AuthState()
        data class Authenticated(val user: FirebaseUser) : AuthState()
        object Unauthenticated : AuthState()
        data class Error(val error: String) : AuthState()
    }
}