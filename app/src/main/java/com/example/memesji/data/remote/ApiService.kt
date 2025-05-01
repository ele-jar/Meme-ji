package com.example.memesji.data.remote

import com.example.memesji.data.AppInfo
import com.example.memesji.data.Meme
import retrofit2.Response
import retrofit2.http.GET

interface ApiService {
    @GET("ele-jar/meme-database/main/memes.json")
    suspend fun getMemes(): Response<List<Meme>>

    @GET("ele-jar/meme-database/main/app_info.json")
    suspend fun getAppUpdateInfo(): Response<AppInfo>
}
