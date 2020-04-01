package com.cainwong.eskrimacounterstimer

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.preference.PreferenceManager
import com.cainwong.eskrimacounterstimer.core.Metronome
import com.cainwong.eskrimacounterstimer.ui.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

@SuppressLint("ServiceCast")
val appModule = module {
    single { PreferenceManager.getDefaultSharedPreferences(androidApplication()) }
    single { androidApplication().resources }
    single { Metronome(get(), get()) }
    single { androidApplication().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    viewModel { MainViewModel(get(), get()) }
}