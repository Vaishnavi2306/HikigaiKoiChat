package com.hikigai.koichat

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.concurrent.TimeUnit

private const val TAG = "OtpActivity"

class OtpActivity : AppCompatActivity() {
    private lateinit var otpDigits: Array<EditText>
    private lateinit var verifyOtpButton: MaterialButton
    private lateinit var resendOtpButton: TextView
    private lateinit var phoneNumberText: TextView
    
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    private var phoneNumber: String = ""
    private var isNewUser: Boolean = false
    private var isProcessing: Boolean = false
    private var navigationStarted: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)
        
        // Get data from intent
        phoneNumber = intent.getStringExtra("phoneNumber") ?: ""
        isNewUser = intent.getBooleanExtra("isNewUser", false)
        verificationId = intent.getStringExtra("verificationId")
        
        Log.d(TAG, "OTP Activity started - Phone: $phoneNumber, isNewUser: $isNewUser")
        
        if (phoneNumber.isEmpty() || verificationId == null) {
            Log.e(TAG, "Missing required data - Phone: $phoneNumber, verificationId: $verificationId")
            Toast.makeText(this, "Invalid verification data", Toast.LENGTH_SHORT).show()
            finishAndGoBack()
            return
        }
        
        initializeViews()
        setupOtpInputs()
        setupClickListeners()
        
        // Show phone number
        phoneNumberText.text = phoneNumber
    }

    private fun initializeViews() {
        otpDigits = arrayOf(
            findViewById(R.id.otpDigit1),
            findViewById(R.id.otpDigit2),
            findViewById(R.id.otpDigit3),
            findViewById(R.id.otpDigit4),
            findViewById(R.id.otpDigit5),
            findViewById(R.id.otpDigit6)
        )
        verifyOtpButton = findViewById(R.id.verifyOtpButton)
        resendOtpButton = findViewById(R.id.resendOtpText)
        phoneNumberText = findViewById(R.id.phoneNumberText)
    }

    private fun setupOtpInputs() {
        for (i in otpDigits.indices) {
            otpDigits[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpDigits.size - 1) {
                        otpDigits[i + 1].requestFocus()
                    }
                }
            })

            // Handle backspace
            otpDigits[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (otpDigits[i].text.isEmpty() && i > 0) {
                        otpDigits[i - 1].requestFocus()
                        otpDigits[i - 1].text = null
                        return@setOnKeyListener true
                    }
                }
                false
            }
        }
    }

    private fun setupClickListeners() {
        verifyOtpButton.setOnClickListener {
            if (isProcessing || navigationStarted) {
                return@setOnClickListener
            }

            verifyOtpCode()
        }

        resendOtpButton.setOnClickListener {
            if (!isProcessing && !navigationStarted) {
                startPhoneNumberVerification(phoneNumber)
            }
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        Log.d(TAG, "Starting phone verification for: $phoneNumber")
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        
        // Important: Make sure the phone number is in E.164 format (starts with +)
        val formattedPhoneNumber = if (phoneNumber.startsWith("+")) {
            phoneNumber
        } else {
            "+$phoneNumber"
        }
        
        Log.d(TAG, "Formatted phone number for verification: $formattedPhoneNumber")
        
        try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedPhoneNumber) // Must be in E.164 format
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()
                
            PhoneAuthProvider.verifyPhoneNumber(options)
            Toast.makeText(this, "Sending verification code...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting verification: ${e.message}", e)
            progressBar.visibility = View.GONE
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            resetProcessingState()
        }
    }
    
    // Create callbacks as a property
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d(TAG, "Verification completed automatically")
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            
            // Auto-fill the OTP if possible
            val code = credential.smsCode
            if (code != null) {
                Log.d(TAG, "Auto-filling OTP: $code")
                fillOtpFields(code)
            }
            
            // Auto-verify
            signInWithPhoneAuthCredential(credential)
        }
        
        override fun onVerificationFailed(e: FirebaseException) {
            Log.e(TAG, "Verification failed: ${e.message}", e)
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> {
                    val message = "Invalid phone number format. Please check the country code and number."
                    Log.e(TAG, message)
                    Toast.makeText(this@OtpActivity, message, Toast.LENGTH_LONG).show()
                }
                is FirebaseTooManyRequestsException -> {
                    val message = "Too many verification attempts. Please try again later."
                    Log.e(TAG, message)
                    Toast.makeText(this@OtpActivity, message, Toast.LENGTH_LONG).show()
                }
                else -> {
                    val message = "Verification failed: ${e.message}"
                    Log.e(TAG, message)
                    Toast.makeText(this@OtpActivity, message, Toast.LENGTH_LONG).show()
                }
            }
            resetProcessingState()
            verifyOtpButton.isEnabled = true
        }
        
        override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
            Log.d(TAG, "Verification code sent to $phoneNumber")
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            verificationId = vId
            resendToken = token
            
            Toast.makeText(this@OtpActivity, 
                "Verification code sent to $phoneNumber", 
                Toast.LENGTH_SHORT).show()
            
            // Enable verify button
            verifyOtpButton.isEnabled = true
            
            // Start countdown timer for resend
            startResendTimer()
        }
    }

    private fun fillOtpFields(code: String) {
        if (code.length == 6) {
            try {
                otpDigits[0].setText(code[0].toString())
                otpDigits[1].setText(code[1].toString())
                otpDigits[2].setText(code[2].toString())
                otpDigits[3].setText(code[3].toString())
                otpDigits[4].setText(code[4].toString())
                otpDigits[5].setText(code[5].toString())
            } catch (e: Exception) {
                Log.e(TAG, "Error filling OTP fields: ${e.message}")
            }
        }
    }
    
    private fun startResendTimer() {
        resendOtpButton.isEnabled = false
        val countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                resendOtpButton.text = "Resend in ${millisUntilFinished / 1000}s"
            }
            
            override fun onFinish() {
                resendOtpButton.isEnabled = true
                resendOtpButton.text = "Resend OTP"
            }
        }
        countDownTimer.start()
    }
    
    private fun verifyOtpCode() {
        if (isProcessing) {
            Log.d(TAG, "Verification already in progress")
            return
        }
        
        val otpCode = getOtpFromInputs()
        if (otpCode.length != 6) {
            Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "Verifying OTP code for phone: $phoneNumber")
        isProcessing = true
        verifyOtpButton.isEnabled = false
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        
        try {
            val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)
            signInWithPhoneAuthCredential(credential)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating credential: ${e.message}")
            resetProcessingState()
            Toast.makeText(this, "Verification failed: ${e.message}", Toast.LENGTH_SHORT).show()
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        }
    }

    private fun getOtpFromInputs(): String {
        return otpDigits.joinToString("") { it.text.toString() }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        try {
            auth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Sign in successful")
                        val user = task.result?.user
                        if (user != null) {
                            if (isNewUser) {
                                createUserInFirestore(user.uid)
                            } else {
                                navigateToHome(user.uid)
                            }
                        } else {
                            Log.e(TAG, "User is null after successful sign in")
                            resetProcessingState()
                            Toast.makeText(this, "Authentication failed: User is null", Toast.LENGTH_SHORT).show()
                            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                        }
                    } else {
                        Log.e(TAG, "Sign in failed: ${task.exception?.message}")
                        resetProcessingState()
                        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                        
                        val errorMessage = when {
                            task.exception?.message?.contains("invalid verification code", ignoreCase = true) == true -> 
                                "Invalid verification code. Please try again."
                            else -> "Authentication failed: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error: ${e.message}")
            resetProcessingState()
            Toast.makeText(this, "Error during sign in: ${e.message}", Toast.LENGTH_SHORT).show()
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
        }
    }
    
    private fun createUserInFirestore(userId: String) {
        Log.d(TAG, "Creating new user in Firestore")
        val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
        
        val userData = hashMapOf(
            "name" to sharedPrefs.getString("name", ""),
            "email" to sharedPrefs.getString("email", ""),
            "phoneNumber" to phoneNumber,
            "specialty" to sharedPrefs.getString("specialty", ""),
            "createdAt" to FieldValue.serverTimestamp()
        )
        
        db.collection("users")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                Log.d(TAG, "User created successfully in Firestore")
                navigateToHome(userId)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error creating user: ${e.message}")
                resetProcessingState()
                Toast.makeText(this, "Failed to create user profile", Toast.LENGTH_SHORT).show()
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            }
    }

    private fun navigateToHome(userId: String) {
        if (navigationStarted) {
            return
        }
        
        navigationStarted = true
        Log.d(TAG, "Navigating to Home screen")
        
        try {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("isNewUser", isNewUser)
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}")
            navigationStarted = false
        }
    }

    private fun resetProcessingState() {
        isProcessing = false
        verifyOtpButton.isEnabled = true
    }
    
    private fun finishAndGoBack() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
} 