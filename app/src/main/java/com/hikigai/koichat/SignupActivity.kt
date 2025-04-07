package com.hikigai.koichat

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.regex.Pattern
import java.util.concurrent.TimeUnit

private const val TAG = "SignupActivity"

class SignupActivity : AppCompatActivity() {
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var specialtyEditText: EditText
    private lateinit var phoneNumberEditText: EditText
    private lateinit var countrySpinner: Spinner
    private lateinit var signupButton: MaterialButton
    private lateinit var loginLink: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set window soft input mode to adjust resize
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        setContentView(R.layout.activity_signup)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        specialtyEditText = findViewById(R.id.specialtyEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        countrySpinner = findViewById(R.id.countrySpinner)
        signupButton = findViewById(R.id.signupButton)
        loginLink = findViewById(R.id.loginLink)

        // Set click listeners
        signupButton.setOnClickListener {
            validateAndSignup()
        }

        loginLink.setOnClickListener {
            finish()
        }
    }

    private fun validateAndSignup() {
        val name = nameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val specialty = specialtyEditText.text.toString().trim()
        val rawPhoneNumber = phoneNumberEditText.text.toString().trim()
        val countryCode = when (countrySpinner.selectedItemPosition) {
            0 -> "+91" // India
            1 -> "+1"  // US
            else -> "+91"
        }
        
        // Check if we have a phone number from input
        if (rawPhoneNumber.isEmpty()) {
            phoneNumberEditText.error = "Please enter your phone number"
            return
        }
        
        // Format the phone number with country code
        val phoneNumber = if (rawPhoneNumber.startsWith("+")) {
            rawPhoneNumber // Already has country code, keep as is
        } else {
            "$countryCode$rawPhoneNumber" // Add selected country code
        }
        
        Log.d(TAG, "Phone number with country code: $phoneNumber")
        
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

        if (name.isEmpty()) {
            nameEditText.error = getString(R.string.enter_name)
            return
        }

        if (email.isEmpty()) {
            emailEditText.error = getString(R.string.enter_email)
            return
        }

        if (!isValidEmail(email)) {
            emailEditText.error = getString(R.string.enter_valid_email)
            return
        }

        // Check for network connectivity
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable button and show progress
        signupButton.isEnabled = false
        signupButton.text = "Creating Account..."
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // First check if user already exists in Firestore
        db.collection("users")
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // User already exists
                    Toast.makeText(this, "An account with this phone number already exists. Please log in.", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    signupButton.isEnabled = true
                    signupButton.text = "SIGN UP"
                    
                    // Go back to login
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.putExtra("phoneNumber", phoneNumber)
                    startActivity(intent)
                    finish()
                } else {
                    // User doesn't exist - continue with signup
                    Log.d(TAG, "Phone number not found in Firestore, proceeding with new signup")
                    
                    // Save user data in SharedPreferences for later use
                    val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
                    val editor = sharedPrefs.edit()
                    editor.putString("name", name)
                    editor.putString("email", email)
                    editor.putString("phoneNumber", phoneNumber)
                    editor.putString("specialty", specialty)
                    editor.apply()
                    
                    // Proceed directly to OTP verification with the number entered on THIS screen
                    startPhoneNumberVerification(phoneNumber)
                }
            }
            .addOnFailureListener { e ->
                // Error checking user existence
                Log.e(TAG, "Error checking user existence: ${e.message}")
                Toast.makeText(this, "Network error. Please try again.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                signupButton.isEnabled = true
                signupButton.text = "SIGN UP"
            }
    }

    private fun isValidEmail(email: String): Boolean {
        val pattern = Pattern.compile(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                    ")+"
        )
        return pattern.matcher(email).matches()
    }

    private fun isValidPhoneNumber(phone: String, countryCode: String): Boolean {
        return when (countryCode) {
            "+91" -> phone.matches("^[6-9]\\d{9}$".toRegex()) // Indian numbers
            "+1" -> phone.matches("^[2-9]\\d{9}$".toRegex())  // US numbers
            else -> false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return actNw.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        Log.d(TAG, "Starting phone verification for signup: $phoneNumber")
        
        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification completed (rare with new numbers)
                        Log.d(TAG, "Auto-verification completed during signup")
                        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                        signInWithPhoneAuthCredential(credential)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e(TAG, "Verification failed during signup: ${e.message}")
                        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                        signupButton.isEnabled = true
                        signupButton.text = "SIGN UP"
                        
                        // Show appropriate error messages
                        when (e) {
                            is FirebaseAuthInvalidCredentialsException -> {
                                Toast.makeText(this@SignupActivity, 
                                    "Invalid phone number format. Please check the country code and number.", 
                                    Toast.LENGTH_LONG).show()
                            }
                            is FirebaseTooManyRequestsException -> {
                                Toast.makeText(this@SignupActivity, 
                                    "Too many verification attempts for this number. Please try again later.", 
                                    Toast.LENGTH_LONG).show()
                            }
                            else -> {
                                Toast.makeText(this@SignupActivity, 
                                    "Verification failed: ${e.message}", 
                                    Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        Log.d(TAG, "Verification code sent during signup to $phoneNumber")
                        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                        
                        // Navigate to OTP screen with verification ID
                        val intent = Intent(this@SignupActivity, OtpActivity::class.java)
                        intent.putExtra("phoneNumber", phoneNumber)
                        intent.putExtra("isNewUser", true)
                        intent.putExtra("verificationId", verificationId)
                        startActivity(intent)
                        finish()
                    }
                })
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
            Toast.makeText(this, "Sending verification code to $phoneNumber...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting verification during signup: ${e.message}")
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            signupButton.isEnabled = true
            signupButton.text = "SIGN UP"
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    // Save user data to Firestore after successful authentication
                    saveUserToFirestore(user.uid)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Authentication failed: ${e.message}", Toast.LENGTH_SHORT).show()
                signupButton.isEnabled = true
                signupButton.text = "SIGN UP"
            }
    }
    
    private fun saveUserToFirestore(userId: String) {
        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        val userData = hashMapOf(
            "name" to (sharedPrefs.getString("name", "") ?: ""),
            "email" to (sharedPrefs.getString("email", "") ?: ""),
            "phoneNumber" to (sharedPrefs.getString("phoneNumber", "") ?: ""),
            "specialty" to (sharedPrefs.getString("specialty", "") ?: ""),
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        
        db.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("isNewUser", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Account created but couldn't save data: ${e.message}", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("isNewUser", true)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                finish()
            }
    }
} 