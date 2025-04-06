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
import com.hikigai.koichat.adapters.FeatureAdapter

class FeatureCardsDialog(context: Context) : Dialog(context) {
    private var viewPager: ViewPager2? = null
    private var dotsIndicator: WormDotsIndicator? = null
    private var currentPage = 0
    private var pageChangeCallback: ViewPager2.OnPageChangeCallback? = null
    private var adapter: FeatureAdapter? = null
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_feature_cards)

            // Set dialog window attributes
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                
                // Set dialog width to match parent with margins
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
                // Create adapter if needed
                if (adapter == null) {
                    adapter = FeatureAdapter(context)
                }

                // Set adapter
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
        private const val TAG = "FeatureCardsDialog"
        private const val KEY_CURRENT_PAGE = "current_page"
    }
} 