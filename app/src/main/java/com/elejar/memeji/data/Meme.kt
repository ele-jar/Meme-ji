package com.elejar.memeji.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Meme(
    val name: String,
    val url: String,
    val tags: List<String>
) : Parcelable
