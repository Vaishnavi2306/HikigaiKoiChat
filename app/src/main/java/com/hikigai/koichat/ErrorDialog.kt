package com.hikigai.koichat

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.view.WindowManager

class ErrorDialog(context: Context) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_error)

        // Make dialog background transparent
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Set dialog width to match parent with margins
        window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Auto dismiss after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
        }, 3000)
    }

    companion object {
        fun show(context: Context) {
            ErrorDialog(context).show()
        }
    }
} 