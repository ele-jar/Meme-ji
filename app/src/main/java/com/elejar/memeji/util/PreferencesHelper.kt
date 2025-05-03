package com.elejar.memeji.util

import android.content.Context
import android.content.SharedPreferences

object PreferencesHelper {

    private const val PREFS_NAME = "MemeJiPrefs"
    private const val KEY_CUTIE_MODE = "cutieModeEnabled"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isCutieModeEnabled(context: Context): Boolean {
        // Default to true (Cutie Mode ON) for safety
        return getPreferences(context).getBoolean(KEY_CUTIE_MODE, false)
    }

    fun setCutieModeEnabled(context: Context, enabled: Boolean) {
        getPreferences(context).edit().putBoolean(KEY_CUTIE_MODE, enabled).apply()
    }
}
