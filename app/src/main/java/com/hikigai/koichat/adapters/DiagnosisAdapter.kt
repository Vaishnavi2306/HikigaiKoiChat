package com.hikigai.koichat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.hikigai.koichat.R
import com.hikigai.koichat.data.DiagnosisResponse

class DiagnosisAdapter : RecyclerView.Adapter<DiagnosisAdapter.ViewHolder>() {
    private var diagnoses: List<DiagnosisResponse> = emptyList()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val diagnosisName: TextView = itemView.findViewById(R.id.diagnosisName)
        private val diagnosisDescription: TextView = itemView.findViewById(R.id.diagnosisDescription)

        fun bind(diagnosis: DiagnosisResponse) {
            diagnosisName.text = diagnosis.name
            diagnosis.description?.let { description ->
                diagnosisDescription.text = description
                diagnosisDescription.visibility = View.VISIBLE
            } ?: run {
                diagnosisDescription.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_diagnosis, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(diagnoses[position])
    }

    override fun getItemCount() = diagnoses.size

    fun updateDiagnoses(newDiagnoses: List<DiagnosisResponse>) {
        diagnoses = newDiagnoses
        notifyDataSetChanged()
    }
} 