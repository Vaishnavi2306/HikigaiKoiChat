package com.hikigai.koichat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Feature(
    val title: String,
    val description: String
) : Parcelable 