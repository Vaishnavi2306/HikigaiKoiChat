package com.hikigai.koichat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InfoSlide(
    val imageResId: Int,
    val title: String,
    val description: String
) : Parcelable 