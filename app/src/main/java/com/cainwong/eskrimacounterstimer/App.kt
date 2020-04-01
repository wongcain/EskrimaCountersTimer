package com.cainwong.eskrimacounterstimer

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import android.content.Intent
import androidx.preference.PreferenceManager
import com.cainwong.eskrimacounterstimer.services.TtsCacheService

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        val initServiceIntent = Intent()
        initServiceIntent.setClass(this, TtsCacheService::class.java)
        startService(initServiceIntent)
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }
}
