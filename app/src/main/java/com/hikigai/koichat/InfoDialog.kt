package com.hikigai.koichat

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class InfoDialog(context: Context) : Dialog(context) {
    private var viewPager: ViewPager2? = null
    private var dotsIndicator: WormDotsIndicator? = null
    private var currentPage = 0
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var adapter: InfoSlideAdapter? = null
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_info)

            // Set dialog window attributes
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
                )
            }

            // Initialize views safely
            if (!initializeViews()) {
                Log.e(TAG, "Failed to initialize views")
                dismiss()
                return
            }

            // Setup ViewPager safely
            if (!setupViewPager()) {
                Log.e(TAG, "Failed to setup ViewPager")
                dismiss()
                return
            }

            // Set dialog properties
            setCanceledOnTouchOutside(true)
            setCancelable(true)

            // Restore state if available
            savedInstanceState?.let {
                currentPage = it.getInt(KEY_CURRENT_PAGE, 0)
                viewPager?.setCurrentItem(currentPage, false)
            }

            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            Toast.makeText(context, "Error initializing dialog", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun initializeViews(): Boolean {
        return try {
            viewPager = findViewById(R.id.viewPager)
            dotsIndicator = findViewById(R.id.dotsIndicator)

            if (viewPager == null || dotsIndicator == null) {
                Log.e(TAG, "Failed to find views")
                return false
            }

            // Set ViewPager orientation
            viewPager?.orientation = ViewPager2.ORIENTATION_HORIZONTAL
            
            // Disable user input until fully initialized
            viewPager?.isUserInputEnabled = false
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            false
        }
    }

    private fun setupViewPager(): Boolean {
        return try {
            viewPager?.let { pager ->
                // Create adapter with info slides
                val infoSlides = listOf(
                    InfoSlide(
                        imageResId = R.drawable.info1,
                        title = "KOI: A Quantum Leap in Healthcare",
                        description = "KOI, our advanced AI platform, is set to revolutionize healthcare by seamlessly merging with medical science. It equips doctors with a powerful suite of tools to elevate patient care and drive better outcomes."
                    ),
                    InfoSlide(
                        imageResId = R.drawable.info2,
                        title = "A Comprehensive Overview",
                        description = "KOI enhances patient care across multiple dimensions:\n\n" +
                            "• Personalized Insights: Delivers tailored recommendations based on individual patient profiles, ensuring precise and effective treatment.\n\n" +
                            "• Enhanced Diagnosis: Assists doctors in making accurate, timely diagnoses by analyzing visit notes and medical history.\n\n" +
                            "• Proactive Care: Identifies missing lab tests or vaccinations, proactively alerting doctors to ensure comprehensive patient care.\n\n" +
                            "• OCR-Powered Data Extraction: Utilizes OCR technology to extract key details from prescriptions and lab reports, saving time and improving accuracy."
                    ),
                    InfoSlide(
                        imageResId = R.drawable.info3,
                        title = "Benefits for Doctors and Patients",
                        description = "KOI transforms healthcare with its powerful capabilities:\n\n" +
                            "• Improved Efficiency: Automates administrative tasks, freeing doctors to focus on patient care.\n\n" +
                            "• Enhanced Accuracy: Minimizes human errors, ensuring precise diagnoses and treatment plans.\n\n" +
                            "• Personalized Care: Delivers tailored recommendations to address each patient's unique needs.\n\n" +
                            "• Better Patient Outcomes: Optimizes medication selection and prevents adverse reactions for improved health.\n\n" +
                            "• Empowered Patients: Provides patients with improved insights into their health."
                    ),
                    InfoSlide(
                        imageResId = R.drawable.info4,
                        title = "A Quantum Leap Forward",
                        description = "KOI marks a revolutionary advancement in healthcare, showcasing the immense potential of AI. By seamlessly integrating technology, KOI is shaping a future where healthcare is smarter, more personalized, and highly efficient. Together, we can harness AI to transform patient care and elevate health outcomes on a global scale."
                    )
                )

                adapter = InfoSlideAdapter(infoSlides)
                pager.adapter = adapter

                // Setup page change callback
                pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        currentPage = position
                    }
                }.also { callback ->
                    pager.registerOnPageChangeCallback(callback)
                }

                // Reduce sensitivity to prevent accidental swipes
                pager.getChildAt(0)?.overScrollMode = ViewPager2.OVER_SCROLL_NEVER

                // Connect dots indicator safely
                dotsIndicator?.setViewPager2(pager)

                // Enable user input now that everything is set up
                pager.isUserInputEnabled = true

                return true
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up ViewPager: ${e.message}", e)
            false
        }
    }

    override fun onSaveInstanceState(): Bundle {
        return Bundle().apply {
            putInt(KEY_CURRENT_PAGE, currentPage)
        }
    }

    override fun dismiss() {
        if (!isInitialized) {
            super.dismiss()
            return
        }

        try {
            // Cleanup ViewPager
            pageChangeCallback?.let { callback ->
                viewPager?.unregisterOnPageChangeCallback(callback)
            }
            pageChangeCallback = null

            // Clear adapter
            viewPager?.adapter = null
            adapter = null

            // Clear views
            viewPager = null
            dotsIndicator = null

            isInitialized = false
            
            super.dismiss()
        } catch (e: Exception) {
            Log.e(TAG, "Error in dismiss: ${e.message}", e)
            super.dismiss()
        }
    }

    companion object {
        private const val TAG = "InfoDialog"
        private const val KEY_CURRENT_PAGE = "current_page"
    }
} 