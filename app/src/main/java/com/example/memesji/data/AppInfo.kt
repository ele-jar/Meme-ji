package com.example.memesji.data

import com.google.gson.annotations.SerializedName

data class AppInfo(
    @SerializedName("version")
    val version: String?,

    @SerializedName("build_date")
    val buildDate: String?,

    @SerializedName("show_download")
    val showDownload: Boolean = false,

    @SerializedName("download_url")
    val downloadUrl: String?,

    @SerializedName("changelog")
    val changelog: String?
)
