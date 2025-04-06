package com.hikigai.koichat

import android.app.Application
import android.util.Log
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class HikigaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)
            
            // Note: Firebase Auth persistence is enabled by default
            // The setPersistenceEnabled API is deprecated and has been removed
            
            // Initialize Firestore with settings
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
            
            // Initialize Analytics
            FirebaseAnalytics.getInstance(this)
            
            // Only use emulators if explicitly configured
            if (BuildConfig.DEBUG && System.getenv("USE_FIREBASE_EMULATORS") == "true") {
                FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9099)
                FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8080)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "HikigaiApplication"
    }
} 