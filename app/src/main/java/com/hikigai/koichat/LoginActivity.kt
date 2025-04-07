package com.hikigai.koichat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit

private const val TAG = "LoginActivity"
private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000

class LoginActivity : AppCompatActivity() {
    private lateinit var phoneNumberEditText: EditText
    private lateinit var countrySpinner: Spinner
    private lateinit var sendOtpButton: MaterialButton
    private lateinit var signupLink: TextView
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var storedVerificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check for Google Play Services
        if (!checkPlayServices()) {
            return
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        countrySpinner = findViewById(R.id.countrySpinner)
        sendOtpButton = findViewById(R.id.sendOtpButton)
        signupLink = findViewById(R.id.signupLink)

        // Handle back press
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        // Check if we got a phone number from another activity
        val phoneFromIntent = intent.getStringExtra("phoneNumber")
        if (!phoneFromIntent.isNullOrEmpty()) {
            // If the phone number starts with a country code, extract it
            if (phoneFromIntent.startsWith("+")) {
                if (phoneFromIntent.startsWith("+91")) {
                    countrySpinner.setSelection(0) // India
                    // Remove country code to show just the number
                    phoneNumberEditText.setText(phoneFromIntent.substring(3))
                } else if (phoneFromIntent.startsWith("+1")) {
                    countrySpinner.setSelection(1) // US
                    // Remove country code to show just the number
                    phoneNumberEditText.setText(phoneFromIntent.substring(2))
                } else {
                    // Some other country code, just show the full number
                    phoneNumberEditText.setText(phoneFromIntent)
                }
            } else {
                // No country code, just set the number as is
                phoneNumberEditText.setText(phoneFromIntent)
            }
        }

        // Set click listeners
        sendOtpButton.setOnClickListener {
            validateAndProceed()
        }

        signupLink.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun validateAndProceed() {
        val rawPhoneNumber = phoneNumberEditText.text.toString().trim()
        val countryCode = when (countrySpinner.selectedItemPosition) {
            0 -> "+91" // India
            1 -> "+1"  // US
            else -> "+91"
        }

        // Basic validations
        if (rawPhoneNumber.isEmpty()) {
            phoneNumberEditText.error = "Please enter your phone number"
            return
        }
        
        // Check if the number is already formatted with a country code
        val phoneNumber = if (rawPhoneNumber.startsWith("+")) {
            rawPhoneNumber // Already formatted with country code
        } else {
            "$countryCode$rawPhoneNumber" // Add selected country code
        }
        
        Log.d(TAG, "Attempting login with phone number: $phoneNumber")
        
        // Basic validation for phone number length
        if (!rawPhoneNumber.startsWith("+") && rawPhoneNumber.length < 10) {
            phoneNumberEditText.error = "Please enter a valid phone number"
            return
        }
        
        // Show loading state immediately
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        sendOtpButton.isEnabled = false

        // Query Firestore to check if user exists
        Log.d(TAG, "Checking if user exists in Firestore")
        db.collection("users")
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d(TAG, "User not found in Firestore")
                    progressBar.visibility = View.GONE
                    sendOtpButton.isEnabled = true
                    Toast.makeText(this, "Account not found. Please sign up first.", Toast.LENGTH_SHORT).show()
                    // Pass the phone number to SignupActivity
                    val intent = Intent(this, SignupActivity::class.java)
                    intent.putExtra("phoneNumber", phoneNumber)
                    startActivity(intent)
                } else {
                    Log.d(TAG, "User found in Firestore, starting phone verification")
                    startPhoneNumberVerification(phoneNumber)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking user existence: ${e.message}")
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true
                Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkPlayServices(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST) {
                    // User cancelled the dialog
                    finish()
                }?.show()
            } else {
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_LONG).show()
                finish()
            }
            return false
        }
        return true
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        Log.d(TAG, "Starting phone verification for: $phoneNumber")
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: Auto-verification successful")
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}")
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true
                
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Invalid phone number format."
                    is FirebaseTooManyRequestsException -> "Too many requests. Please try again later."
                    else -> "Verification failed: ${e.message}"
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "OTP code sent successfully")
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true

                // Navigate to OTP screen with proper extras
                val intent = Intent(this@LoginActivity, OtpActivity::class.java).apply {
                    putExtra("verificationId", verificationId)
                    putExtra("phoneNumber", phoneNumber)
                    putExtra("isNewUser", false)
                }
                startActivity(intent)
                finish()
            }
        }

        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d(TAG, "Phone verification initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting phone verification: ${e.message}", e)
            progressBar.visibility = View.GONE
            sendOtpButton.isEnabled = true
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                // Navigate to HomeActivity after successful verification
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                sendOtpButton.isEnabled = true
            }
    }

    override fun onStart() {
        super.onStart()
        // If user is already logged in, go directly to HomeActivity
        auth.currentUser?.let {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun isValidPhoneNumber(phone: String, countryCode: String): Boolean {
        // For testing in debug mode
        val isDebug = try {
            packageManager.getApplicationInfo(packageName, 0).flags and 
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        } catch (e: Exception) {
            false
        }

        // Allow test number in debug mode
        if (isDebug && phone == "1234567890") {
            return true
        }

        return when (countryCode) {
            "+91" -> phone.matches("^[6-9]\\d{9}$".toRegex()) // Indian numbers
            "+1" -> phone.matches("^[2-9]\\d{9}$".toRegex())  // US numbers
            else -> true // Allow other numbers for testing
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
} 