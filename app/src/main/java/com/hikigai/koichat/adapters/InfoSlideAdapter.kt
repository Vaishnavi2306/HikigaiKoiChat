package com.hikigai.koichat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hikigai.koichat.InfoSlide
import com.hikigai.koichat.R

class InfoSlideAdapter(private val slides: List<InfoSlide>) : 
    RecyclerView.Adapter<InfoSlideAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_info_slide, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount() = slides.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val slideImage: ImageView = itemView.findViewById(R.id.slideImage)
        private val title: TextView = itemView.findViewById(R.id.titleText)
        private val description: TextView = itemView.findViewById(R.id.descriptionText)

        fun bind(slide: InfoSlide) {
            slideImage.setImageResource(slide.imageResId)
            title.text = slide.title
            description.text = slide.description
        }
    }
} 