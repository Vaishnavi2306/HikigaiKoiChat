package com.hikigai.koichat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hikigai.koichat.R

class InfoAdapter(private val context: Context) : RecyclerView.Adapter<InfoAdapter.ViewHolder>() {
    private val infoItems = listOf(
        InfoItem(
            "Welcome to KOI",
            "KOI is your advanced AI assistant designed to enhance healthcare delivery. It provides intelligent support for medical professionals, streamlining workflows and improving patient care."
        ),
        InfoItem(
            "AI-Powered Support",
            "Our platform leverages cutting-edge AI technology to assist in medical documentation, decision support, and patient care optimization. Experience the future of healthcare technology."
        ),
        InfoItem(
            "Important Notice",
            "This platform is a testing ground for doctors to explore the capabilities of our AI Engine. Using it for any other purpose is prohibited."
        )
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_feature_card, parent, false)
            ViewHolder(view)
        } catch (e: Exception) {
            ViewHolder(View(parent.context))
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position in infoItems.indices) {
                holder.bind(infoItems[position])
            }
        } catch (e: Exception) {
            // Handle binding error
        }
    }

    override fun getItemCount() = infoItems.size

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
                // Handle view initialization error
            }
        }

        fun bind(info: InfoItem) {
            try {
                logo?.setImageResource(R.drawable.hikigai_final_logo_png)
                title?.text = info.title
                description?.text = info.description
            } catch (e: Exception) {
                // Handle binding error
            }
        }
    }
}

data class InfoItem(
    val title: String,
    val description: String
) 