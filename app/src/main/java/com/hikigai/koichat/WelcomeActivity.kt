package com.hikigai.koichat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "WelcomeActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Hide system UI for immersive experience
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        // Set persistence for Firebase Auth
        auth.firebaseAuthSettings.setAppVerificationDisabledForTesting(false)

        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthAndNavigate()
        }, 2000) // 2 seconds delay
    }

    private fun checkAuthAndNavigate() {
        val currentUser = auth.currentUser
        Log.d(TAG, "Checking authentication: user=${currentUser?.uid}")
        
        if (currentUser != null) {
            // User is already logged in, check if profile exists in Firestore
            Log.d(TAG, "User is authenticated, checking Firestore profile")
            
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // User has a profile, go to HomeActivity
                        Log.d(TAG, "User profile found, navigating to Home")
                        navigateToHome()
                    } else {
                        Log.d(TAG, "User authenticated but no profile found, trying to create from local data")
                        createProfileFromLocalData(currentUser.uid)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking user profile: ${e.message}")
                    // Try to create profile from local data as fallback
                    createProfileFromLocalData(currentUser.uid)
                }
        } else {
            // No authenticated user, go to login
            Log.d(TAG, "No user authenticated, navigating to Login")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    private fun createProfileFromLocalData(userId: String) {
        // Try to get user data from shared preferences
        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val name = sharedPrefs.getString("name", "")
        val email = sharedPrefs.getString("email", "")
        val phoneNumber = sharedPrefs.getString("phoneNumber", "")
        val specialty = sharedPrefs.getString("specialty", "")
        
        if (!phoneNumber.isNullOrEmpty()) {
            // We have some data, create a profile
            Log.d(TAG, "Creating user profile from local data")
            
            val userData = hashMapOf(
                "name" to (name ?: ""),
                "email" to (email ?: ""),
                "phoneNumber" to phoneNumber,
                "specialty" to (specialty ?: ""),
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            db.collection("users")
                .document(userId)
                .set(userData)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully created user profile from local data")
                    navigateToHome()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create profile from local data: ${e.message}")
                    // Still navigate to home even if profile creation failed
                    navigateToHome()
                }
        } else {
            Log.d(TAG, "No local data available, still navigating to Home")
            navigateToHome()
        }
    }
    
    private fun navigateToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
} 