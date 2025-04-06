package com.hikigai.koichat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InfoSlideAdapter(private val slides: List<InfoSlide>) :
    RecyclerView.Adapter<InfoSlideAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val slideImage: ImageView = view.findViewById(R.id.slideImage)
        val titleText: TextView = view.findViewById(R.id.titleText)
        val descriptionText: TextView = view.findViewById(R.id.descriptionText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_info_slide, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val slide = slides[position]
        holder.slideImage.setImageResource(slide.imageResId)
        holder.titleText.text = slide.title
        holder.descriptionText.text = slide.description
    }

    override fun getItemCount() = slides.size
} 