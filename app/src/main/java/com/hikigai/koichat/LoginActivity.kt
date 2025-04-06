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
        
        Log.d(TAG, "Checking phone number: $phoneNumber")
        
        // Basic validation for phone number length
        if (!rawPhoneNumber.startsWith("+") && rawPhoneNumber.length < 10) {
            phoneNumberEditText.error = "Please enter a valid phone number"
            return
        }
        
        // Check for numeric value in the raw phone number (excluding the + if present)
        val digitsOnly = if (rawPhoneNumber.startsWith("+")) {
            rawPhoneNumber.substring(1)
        } else {
            rawPhoneNumber
        }
        
        if (!digitsOnly.all { it.isDigit() }) {
            phoneNumberEditText.error = "Phone number should contain only digits"
            return
        }

        // Check for network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state immediately
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        sendOtpButton.isEnabled = false

        // Query Firestore to check if user exists
        db.collection("users")
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    progressBar.visibility = View.GONE
                    sendOtpButton.isEnabled = true
                    Toast.makeText(this, "Account not found. Please sign up first.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, SignupActivity::class.java))
                } else {
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
        
        // Show loading state
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        sendOtpButton.isEnabled = false
        Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show()

        // Add timeout handler
        val timeoutHandler = Handler(mainLooper)
        val timeoutRunnable = Runnable {
            if (progressBar.visibility == View.VISIBLE) {
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true
                Toast.makeText(this, "Verification request timed out. Please try again.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Phone verification timed out after 30 seconds")
            }
        }
        timeoutHandler.postDelayed(timeoutRunnable, 30000) // 30 seconds timeout
        
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted: $credential")
                timeoutHandler.removeCallbacks(timeoutRunnable)
                progressBar.visibility = View.GONE
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "onVerificationFailed: ${e.message}", e)
                timeoutHandler.removeCallbacks(timeoutRunnable)
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true

                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> {
                        "Invalid phone number format. Please check the country code and number."
                    }
                    is FirebaseTooManyRequestsException -> {
                        "Too many verification attempts. Please try again later."
                    }
                    else -> {
                        "Verification failed: ${e.message}"
                    }
                }
                Toast.makeText(this@LoginActivity, errorMessage, Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(TAG, "onCodeSent: $verificationId")
                timeoutHandler.removeCallbacks(timeoutRunnable)
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true

                // Navigate to OTP screen
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
            // Validate phone number format before sending
            if (!isValidPhoneNumber(phoneNumber, countrySpinner.selectedItem.toString())) {
                progressBar.visibility = View.GONE
                sendOtpButton.isEnabled = true
                Toast.makeText(this, "Invalid phone number format for selected country", Toast.LENGTH_LONG).show()
                return
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(30L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
            Log.d(TAG, "Phone verification initiated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting phone verification: ${e.message}", e)
            timeoutHandler.removeCallbacks(timeoutRunnable)
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