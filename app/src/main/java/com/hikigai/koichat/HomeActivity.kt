package com.hikigai.koichat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hikigai.koichat.adapters.DiagnosisAdapter
import com.hikigai.koichat.data.APIConfig
import com.hikigai.koichat.data.ChatExample
import com.hikigai.koichat.data.DiagnosisResponse
import com.hikigai.koichat.data.FeedbackType
import com.hikigai.koichat.network.NetworkError
import com.hikigai.koichat.network.NetworkService
import com.hikigai.koichat.FeatureCardsDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class HomeActivity : AppCompatActivity() {
    private lateinit var profileButton: ImageButton
    private lateinit var infoButton: ImageButton
    private lateinit var examplesContainer: LinearLayout
    private lateinit var diagnosisContainer: View
    private lateinit var diagnosisRecyclerView: RecyclerView
    private lateinit var inputField: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var likeButton: ImageButton
    private lateinit var dislikeButton: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var userQueryText: TextView

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private var currentNotes: String = ""
    private var currentDiagnosis: List<DiagnosisResponse> = emptyList()
    private var currentFeedback: FeedbackType? = null

    private val diagnosisAdapter = DiagnosisAdapter()
    private var featureDialog: FeatureCardsDialog? = null
    private var infoDialog: InfoDialog? = null

    private fun requestCallPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) 
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                CALL_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CALL_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted
                    Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()
            
            // Check authentication status
            val currentUser = auth.currentUser
            if (currentUser == null) {
                // Not authenticated, go back to login
                Log.d(TAG, "User not authenticated, redirecting to login")
                Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
                return
            }
            
            // Get user information
            val userId = currentUser.uid
            val phoneNumber = currentUser.phoneNumber
            val isNewUser = intent.getBooleanExtra("isNewUser", false)
            
            Log.d(TAG, "Home screen started with userId: $userId, Phone: $phoneNumber, New user: $isNewUser")
            
            // Verify user data exists in Firestore
            verifyUserDataExists(userId, phoneNumber)
            
            initializeViews()
            setupClickListeners()
            setupRecyclerView()
            populateExamples()
            
            // Show welcome message for new users
            if (isNewUser) {
                Toast.makeText(this, "Welcome to Hikigai Koi Chat!", Toast.LENGTH_LONG).show()
            }

            // Request call permission
            requestCallPermission()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verifyUserDataExists(userId: String, phoneNumber: String?) {
        try {
            // Check if user data exists in Firestore
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        Log.w(TAG, "User document does not exist for ID: $userId")
                        // Try to recover by using shared preferences data if available
                        createUserFromLocalData(userId, phoneNumber)
                    } else {
                        Log.d(TAG, "User document exists: ${document.data}")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking user data: ${e.message}", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying user data: ${e.message}", e)
        }
    }

    private fun createUserFromLocalData(userId: String, phoneNumber: String?) {
        try {
            // Get user data from shared preferences
            val sharedPrefs = getSharedPreferences("UserData", MODE_PRIVATE)
            val name = sharedPrefs.getString("name", "") ?: ""
            val email = sharedPrefs.getString("email", "") ?: ""
            val localPhone = sharedPrefs.getString("phoneNumber", "") ?: phoneNumber ?: ""
            val specialty = sharedPrefs.getString("specialty", "") ?: ""
            
            if (localPhone.isNotEmpty()) {
                // Create user document with the Firebase Auth user ID
                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "phoneNumber" to localPhone,
                    "specialty" to specialty,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                
                db.collection("users").document(userId).set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User created from local data")
                        Toast.makeText(this, "User profile created", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error creating user from local data: ${e.message}", e)
                        Toast.makeText(this, "Failed to create profile. Please update in settings.", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in createUserFromLocalData: ${e.message}", e)
        }
    }

    private fun initializeViews() {
        profileButton = findViewById(R.id.profileButton)
        infoButton = findViewById(R.id.infoButton)
        examplesContainer = findViewById(R.id.examplesContainer)
        diagnosisContainer = findViewById(R.id.diagnosisContainer)
        diagnosisRecyclerView = findViewById(R.id.diagnosisRecyclerView)
        inputField = findViewById(R.id.inputField)
        sendButton = findViewById(R.id.sendButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        likeButton = findViewById(R.id.likeButton)
        dislikeButton = findViewById(R.id.dislikeButton)
        backButton = findViewById(R.id.backButton)
        userQueryText = findViewById(R.id.userQueryText)

        // Initialize the Hikigai logo click listener
        findViewById<ImageView>(R.id.hikigaiLogo).setOnClickListener {
            showFeatureCards()
        }

        // Initialize the AI insights card click listener
        findViewById<View>(R.id.hikigaiIcon)?.setOnClickListener {
            showFeatureCards()
        }
    }

    private fun setupClickListeners() {
        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        infoButton.setOnClickListener {
            showInfoDialog()
        }

        sendButton.setOnClickListener {
            handleUserInput()
        }

        inputField.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND ||
                (event != null && event.keyCode == android.view.KeyEvent.KEYCODE_ENTER && event.action == android.view.KeyEvent.ACTION_DOWN)) {
                handleUserInput()
                true
            } else {
                false
            }
        }

        backButton.setOnClickListener {
            showExamples()
        }

        likeButton.setOnClickListener {
            provideFeedback(FeedbackType.LIKE)
        }

        dislikeButton.setOnClickListener {
            provideFeedback(FeedbackType.DISLIKE)
        }
    }

    private fun handleUserInput() {
        val query = inputField.text.toString().trim()
        if (query.isNotEmpty()) {
            inputField.isEnabled = false
            sendButton.isEnabled = false
            getDiagnosis(query)
            inputField.setText("") // Clear the input field after sending
        } else {
            Toast.makeText(this, "Please enter your query", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateExamples() {
        examplesContainer.removeAllViews()
        ChatExample.examples.forEach { example ->
            val exampleView = layoutInflater.inflate(R.layout.item_example, examplesContainer, false)
            exampleView.findViewById<TextView>(R.id.exampleText).text = example
            exampleView.setOnClickListener {
                inputField.setText(example)
                getDiagnosis(example)
                inputField.setText("") // Clear the input field after sending example
            }
            examplesContainer.addView(exampleView)
        }
    }

    private fun getDiagnosis(query: String) {
        currentNotes = query
        currentFeedback = null
        showProgress()
        showDiagnosis()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    NetworkService.getInstance().getDiagnosis(query)
                }
                if (result.isEmpty()) {
                    showError("No diagnoses found. Please try rephrasing your query.")
                    showExamples()
                } else {
                    currentDiagnosis = result
                    diagnosisAdapter.updateDiagnoses(result)
                    userQueryText.text = query
                }
            } catch (e: Exception) {
                handleChatError()
            } finally {
                hideProgress()
            }
        }
    }

    private fun showDiagnosis() {
        examplesContainer.visibility = View.GONE
        diagnosisContainer.visibility = View.VISIBLE
        userQueryText.text = currentNotes
    }

    private fun showExamples() {
        diagnosisContainer.visibility = View.GONE
        examplesContainer.visibility = View.VISIBLE
        currentDiagnosis = emptyList()
        currentFeedback = null
        diagnosisAdapter.updateDiagnoses(emptyList())
        userQueryText.text = ""
    }

    private fun provideFeedback(type: FeedbackType) {
        if (currentFeedback == type) return
        currentFeedback = type

        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    NetworkService.getInstance().sendFeedback(
                        notes = currentNotes,
                        response = currentDiagnosis,
                        feedback = type,
                        doctorId = "user_id", // TODO: Get from auth service
                        doctorName = "Doctor Name" // TODO: Get from auth service
                    )
                }
                updateFeedbackUI(type)
            } catch (e: Exception) {
                handleChatError()
            }
        }
    }

    private fun updateFeedbackUI(type: FeedbackType) {
        likeButton.isSelected = type == FeedbackType.LIKE
        dislikeButton.isSelected = type == FeedbackType.DISLIKE
    }

    private fun showProgress() {
        loadingIndicator.visibility = View.VISIBLE
        inputField.isEnabled = false
        sendButton.isEnabled = false
    }

    private fun hideProgress() {
        loadingIndicator.visibility = View.GONE
        inputField.isEnabled = true
        sendButton.isEnabled = true
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupRecyclerView() {
        diagnosisRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@HomeActivity)
            adapter = diagnosisAdapter
        }
    }

    private fun showFeatureCards() {
        try {
            if (featureDialog == null || featureDialog?.isShowing == false) {
                featureDialog = FeatureCardsDialog(this).apply {
                    setOnDismissListener {
                        featureDialog = null
                    }
                    show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing feature cards: ${e.message}", e)
            Toast.makeText(this@HomeActivity, "Error showing features", Toast.LENGTH_SHORT).show()
            featureDialog = null
        }
    }

    private fun showInfoDialog() {
        try {
            if (infoDialog == null || infoDialog?.isShowing == false) {
                infoDialog = InfoDialog(this).apply {
                    setOnDismissListener {
                        infoDialog = null
                    }
                    show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing info dialog: ${e.message}", e)
            Toast.makeText(this@HomeActivity, "Error showing info", Toast.LENGTH_SHORT).show()
            infoDialog = null
        }
    }

    private fun showErrorDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_error)
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set dialog width to match parent with margins
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Auto dismiss after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
        }, 2000)

        dialog.show()
    }

    private fun handleChatError() {
        showErrorDialog()
    }

    override fun onStart() {
        super.onStart()
        // Check authentication status
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        featureDialog?.dismiss()
        featureDialog = null
        infoDialog?.dismiss()
        infoDialog = null
    }

    override fun onDestroy() {
        super.onDestroy()
        featureDialog?.dismiss()
        featureDialog = null
        infoDialog?.dismiss()
        infoDialog = null
    }

    companion object {
        private const val TAG = "HomeActivity"
        private const val CALL_PERMISSION_REQUEST_CODE = 101
    }
} 