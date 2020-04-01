package com.cainwong.eskrimacounterstimer.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import com.cainwong.eskrimacounterstimer.R
import com.cainwong.eskrimacounterstimer.databinding.ActivityMainBinding
import com.cainwong.eskrimacounterstimer.services.AudioService
import org.koin.android.viewmodel.ext.android.viewModel


class MainActivity : AppCompatActivity() {

    private val vm: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.vm = vm
        binding.lifecycleOwner = this
        vm.playState.observe(this, Observer { isPlaying ->
            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(Intent(applicationContext, AudioService::class.java))
                } else {
                    startService(Intent(applicationContext, AudioService::class.java))
                }
            } else {
                stopService(Intent(applicationContext, AudioService::class.java))
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun goToSettings(mi: MenuItem) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}
