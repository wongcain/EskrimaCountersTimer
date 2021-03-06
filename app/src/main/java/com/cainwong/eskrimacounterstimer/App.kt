package com.cainwong.eskrimacounterstimer

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import androidx.preference.PreferenceManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }
}
