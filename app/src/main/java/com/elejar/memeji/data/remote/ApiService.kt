package com.elejar.memeji.data.remote

import com.elejar.memeji.data.AppInfo
import com.elejar.memeji.data.Meme
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers 

interface ApiService {
    @GET("ele-jar/meme-database/main/memes.json")
    suspend fun getMemes(): Response<List<Meme>>

    
    @Headers("Cache-Control: no-cache")
    @GET("ele-jar/meme-database/main/app_info.json")
    suspend fun getAppUpdateInfo(): Response<AppInfo>
}
