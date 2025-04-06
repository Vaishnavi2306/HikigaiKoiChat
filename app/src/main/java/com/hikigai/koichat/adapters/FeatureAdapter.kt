package com.hikigai.koichat.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hikigai.koichat.R

class FeatureAdapter(private val context: Context) : RecyclerView.Adapter<FeatureAdapter.ViewHolder>() {
    private val features = listOf(
        Feature(
            "Ambient Documentation",
            "AI-powered ambient documentation captures patient interactions in real time, reducing manual work and boosting efficiency. It minimizes admin tasks, allowing more focus on patient care, while seamless integration ensures accuracy."
        ),
        Feature(
            "Automated Billing and Claims Management",
            "Automate billing and claims effortlessly with KOI. Streamline submissions, reduce admin tasks, and accelerate payments with AI-driven insights."
        ),
        Feature(
            "KOI: A Quantum Leap in Healthcare",
            "KOI, our advanced AI platform, is set to revolutionize healthcare by seamlessly merging with medical science. It equips doctors with a powerful suite of tools to elevate patient care and drive better outcomes."
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feature_card, parent, false)
            ViewHolder(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating ViewHolder: ${e.message}", e)
            // Create an empty view as fallback
            ViewHolder(View(parent.context))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position in features.indices) {
                holder.bind(features[position])
            } else {
                Log.e(TAG, "Invalid position: $position")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding ViewHolder: ${e.message}", e)
        }
    }

    override fun getItemCount() = features.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var logo: ImageView? = null
        private var title: TextView? = null
        private var description: TextView? = null

        init {
            try {
                logo = itemView.findViewById(R.id.hikigaiLogo)
                title = itemView.findViewById(R.id.titleText)
                description = itemView.findViewById(R.id.descriptionText)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ViewHolder views: ${e.message}", e)
            }
        }

        fun bind(feature: Feature) {
            try {
                logo?.setImageResource(R.drawable.hikigai_final_logo_png)
                title?.text = feature.title
                description?.text = feature.description
            } catch (e: Exception) {
                Log.e(TAG, "Error binding feature data: ${e.message}", e)
            }
        }
    }

    companion object {
        private const val TAG = "FeatureAdapter"
    }
}

data class Feature(
    val title: String,
    val description: String
)