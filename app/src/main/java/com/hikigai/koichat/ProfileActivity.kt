package com.hikigai.koichat

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    private lateinit var nameInput: EditText
    private lateinit var mobileInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var specialtyInput: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var experienceInput: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var logoutButton: Button
    private lateinit var backButton: ImageButton

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private var originalData: Map<String, Any>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initializeViews()
        setupListeners()
        loadUserData()
        setupGenderSpinner()
    }

    private fun initializeViews() {
        nameInput = findViewById(R.id.nameInput)
        mobileInput = findViewById(R.id.phoneInput)
        emailInput = findViewById(R.id.emailInput)
        specialtyInput = findViewById(R.id.specialityInput)
        genderSpinner = findViewById(R.id.genderSpinner)
        experienceInput = findViewById(R.id.experienceInput)
        saveButton = findViewById(R.id.saveButton)
        cancelButton = findViewById(R.id.cancelButton)
        logoutButton = findViewById(R.id.logoutButton)
        backButton = findViewById(R.id.backButton)

        // Disable mobile number editing
        mobileInput.isEnabled = false
    }

    private fun setupListeners() {
        saveButton.setOnClickListener { validateAndSave() }
        cancelButton.setOnClickListener { 
            // Restore original data and finish
            originalData?.let { restoreData(it) }
            finish() 
        }
        backButton.setOnClickListener { finish() }
        logoutButton.setOnClickListener { logout() }
    }

    private fun setupGenderSpinner() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = adapter
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val data = document.data
                    originalData = data // Store original data
                    data?.let { restoreData(it) }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun restoreData(data: Map<String, Any>) {
        nameInput.setText(data["name"]?.toString() ?: "")
        mobileInput.setText(data["phoneNumber"]?.toString() ?: "")
        emailInput.setText(data["email"]?.toString() ?: "")
        specialtyInput.setText(data["speciality"]?.toString() ?: "")
        data["gender"]?.toString()?.let { gender ->
            val position = when (gender) {
                "Male" -> 0
                "Female" -> 1
                else -> 2
            }
            genderSpinner.setSelection(position)
        }
        experienceInput.setText(data["experience"]?.toString() ?: "")
    }

    private fun validateAndSave() {
        val name = nameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val specialty = specialtyInput.text.toString().trim()
        val experience = experienceInput.text.toString().trim()

        if (name.isEmpty()) {
            nameInput.error = "Name is required"
            return
        }

        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Invalid email format"
            return
        }

        saveUserData()
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid ?: return
        
        val userData = hashMapOf(
            "name" to nameInput.text.toString().trim(),
            "email" to emailInput.text.toString().trim(),
            "speciality" to specialtyInput.text.toString().trim(),
            "gender" to genderSpinner.selectedItem.toString(),
            "experience" to experienceInput.text.toString().trim()
        )

        saveButton.isEnabled = false // Disable button while saving

        db.collection("users")
            .document(userId)
            .update(userData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                saveButton.isEnabled = true // Re-enable button on failure
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
} 