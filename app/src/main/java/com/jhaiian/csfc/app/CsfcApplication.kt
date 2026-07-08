package com.jhaiian.csfc.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class CsfcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
