package com.example.memesji

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MemesJiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        
    }
}
