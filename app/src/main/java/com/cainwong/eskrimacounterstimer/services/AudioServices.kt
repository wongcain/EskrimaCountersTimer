package com.cainwong.eskrimacounterstimer.services

import android.app.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import com.cainwong.eskrimacounterstimer.R
import com.cainwong.eskrimacounterstimer.core.Metronome
import com.cainwong.eskrimacounterstimer.ui.MainActivity
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import java.util.*
import android.app.PendingIntent
import java.util.concurrent.ThreadLocalRandom


const val ONGOING_NOTIFICATION_ID = 123
const val MAX_RANDOM = 12
const val ACTION_STOP_SERVICE = "stop"

class AudioService : Service() {

    private val metronome: Metronome by inject()

    private val sharedPreferences: SharedPreferences by inject()

    private val notificationManager: NotificationManager by inject()

    private var beatDisposable: Disposable? = null

    private lateinit var mp: MediaPlayer

    private lateinit var clickFileDescriptor: AssetFileDescriptor

    private lateinit var channelId: String

    override fun onCreate() {
        super.onCreate()

        clickFileDescriptor = resources.openRawResourceFd(R.raw.click)
        channelId = getString(R.string.notification_channel_id)

        mp = MediaPlayer().apply {
            setOnPreparedListener { start() }
            setOnCompletionListener { reset() }
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
        if (ACTION_STOP_SERVICE.equals(intent.getAction())) {
            metronome.togglePlay()
            stopSelf()
        } else {
            val pendingIntent: PendingIntent =
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }

            val stopSelf = Intent(this, AudioService::class.java)
            stopSelf.action = ACTION_STOP_SERVICE
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

            startForeground(ONGOING_NOTIFICATION_ID, notification)

            if (beatDisposable?.isDisposed != false) {
                beatDisposable = metronome.beatCount.subscribe(
                    { count ->
                        val counters = sharedPreferences.getStringSet(
                            resources.getString(R.string.pref_counters_key),
                            null
                        ) ?: Collections.emptySet()
                        if (counters.isNotEmpty() && count == 1) {
                            val index = ThreadLocalRandom.current().nextInt(0, counters.size -1)
                            playCounterSound(counters.toList()[index])
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
        mp.run {
            reset()
            setDataSource(
                clickFileDescriptor.fileDescriptor,
                clickFileDescriptor.startOffset,
                clickFileDescriptor.length
            )
            prepareAsync()
        }
    }

    private fun playCounterSound(counter: String) {
        mp.run {
            reset()
            try {
                setDataSource(applicationContext, Uri.fromFile(File(filesDir, "${counter}.wav")))
                prepareAsync()
            } catch (e: Exception) {
                Timber.e(e, "Error playing: ${counter}.wav")
            }
        }
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


class TtsCacheService() : IntentService("TTS Cache Service") {
    lateinit var tts: TextToSpeech

    override fun onHandleIntent(p0: Intent?) {
        tts = TextToSpeech(applicationContext,
            TextToSpeech.OnInitListener { status ->
                if (status != TextToSpeech.ERROR) {
                    onTtsInit()
                }
            })
    }

    private fun onTtsInit() {
        Timber.d("Started TTS for caching...")
        var doneCount = 0
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(p0: String?) {
                incrementDone()
            }

            override fun onError(p0: String?) {
                incrementDone()
            }

            override fun onStart(p0: String?) {}

            private fun incrementDone() {
                doneCount++
                if (doneCount == MAX_RANDOM) {
                    Timber.d("Caching complete.  Shutting TTS down.")
                    tts.stop()
                    tts.shutdown()
                }
            }

        })
        tts.language = Locale.getDefault()
        (1..MAX_RANDOM).forEach {
            val file = File(filesDir, "${it}.wav")
            Timber.d("Creating ${file.name}")
            tts.synthesizeToFile(it.toString(), Bundle(), file, it.toString())
        }
    }
}
