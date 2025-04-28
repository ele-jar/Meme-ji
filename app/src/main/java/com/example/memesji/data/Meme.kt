package com.example.memesji.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Meme(
    val name: String,
    val url: String,
    val tags: List<String>
) : Parcelable
