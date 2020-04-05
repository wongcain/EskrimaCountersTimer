package com.cainwong.eskrimacounterstimer.service

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.cainwong.eskrimacounterstimer.R
import com.cainwong.eskrimacounterstimer.core.Metronome
import com.cainwong.eskrimacounterstimer.ui.MainActivity
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*
import android.app.PendingIntent
import android.media.AudioAttributes
import android.media.SoundPool
import java.util.concurrent.ThreadLocalRandom


class AudioService : Service() {

    private val notificationId = 123

    private val maxRandom = 12

    private val stopService = "stop"

    private val metronome: Metronome by inject()

    private val sharedPreferences: SharedPreferences by inject()

    private val notificationManager: NotificationManager by inject()

    private var beatDisposable: Disposable? = null

    private lateinit var channelId: String

    private lateinit var soundPool: SoundPool

    private lateinit var soundIdsMap: Map<Int, Int>

    override fun onCreate() {
        super.onCreate()

        channelId = getString(R.string.notification_channel_id)

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build()
            ).build()

        soundIdsMap = (0..maxRandom).associateWith {
            soundPool.load(
                this,
                resources.getIdentifier("s$it", "raw", packageName),
                1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT)
            notificationChannel.description = getString(R.string.notification_channel_description)
            notificationChannel.setSound(null, null)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (stopService == intent.action) {
            metronome.togglePlay()
            stopSelf()
        } else {
            val pendingIntent: PendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }

            val stopSelf = Intent(this, AudioService::class.java)
            stopSelf.action = stopService
            val pStopSelf =
                PendingIntent.getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT)
            val notification = NotificationCompat.Builder(this, channelId)
                .setOngoing(true)
                .setSmallIcon(R.drawable.sticks)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_text))
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_stop_white_24dp, "Stop", pStopSelf)
                .build()

            startForeground(notificationId, notification)

            if (beatDisposable?.isDisposed != false) {
                beatDisposable = metronome.beatCount.subscribe(
                    { count ->
                        val counters = sharedPreferences.getStringSet(
                            resources.getString(R.string.pref_counters_key),
                            null
                        ) ?: Collections.emptySet()
                        if (counters.isNotEmpty() && count == 1) {
                            val index = ThreadLocalRandom.current().nextInt(0, counters.size -1)
                            playCounterSound(counters.toList()[index].toInt())
                        } else {
                            playClick()
                        }
                    },
                    Timber::e
                )
            }
        }
        return START_STICKY_COMPATIBILITY
    }

    private fun playClick() {
        playCounterSound(0)
    }

    private fun playCounterSound(soundIndex: Int) {
        val soundId = soundIdsMap[soundIndex]
        if (soundId == null)  {
            Timber.e("Unable to find sound id for index $soundIndex")
            return
        }
        soundPool.play(soundId, 1F, 1F, 1, 0, 1F)
    }

    override fun onDestroy() {
        Timber.d("Shutting down service")
        stopForeground(true)
        beatDisposable?.dispose()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}