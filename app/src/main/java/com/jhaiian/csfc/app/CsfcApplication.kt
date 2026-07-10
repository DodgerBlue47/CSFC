package com.jhaiian.csfc.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.jhaiian.csfc.crash.CrashHandler

class CsfcApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
