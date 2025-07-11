package com.raghu.businesscardscanner2.Login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.raghu.businesscardscanner.R

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            // Configure Google Sign In
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val googleSignInClient = GoogleSignIn.getClient(context, gso)
            val firebaseAuth = FirebaseAuth.getInstance()

            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(
                auth = firebaseAuth,
                googleSignInClient = googleSignInClient
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}