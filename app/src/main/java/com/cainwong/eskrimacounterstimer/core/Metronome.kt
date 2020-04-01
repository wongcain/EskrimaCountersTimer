package com.cainwong.eskrimacounterstimer.core

import android.content.SharedPreferences
import android.content.res.Resources
import com.cainwong.eskrimacounterstimer.R
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class Metronome(
    private val sharedPreferences: SharedPreferences,
    private val resources: Resources
) {
    private var beat = 0
    private var playDisposable: Disposable? = null

    private val delaySubject = BehaviorSubject.create<Int>().apply {
        onNext(sharedPreferences.getInt(resources.getString(R.string.pref_delay_key), DEFAULT_DELAY))
    }

    val tempoBpm: Observable<Int>
        get() = delaySubject.map { millisToBpm(it)}

    private fun millisToBpm(millis: Int) =  (60000.toFloat() / millis).roundToInt()

    private val beatCountSubject = BehaviorSubject.create<Int>()

    val beatCount: Observable<Int>
        get() = beatCountSubject

    private val playStateSubject = BehaviorSubject.create<Boolean>().apply {
        onNext(false)
    }

    val playState: Observable<Boolean>
        get() = playStateSubject

    private val isPlaying: Boolean
        get() = java.lang.Boolean.TRUE == playStateSubject.value

    fun setDelay(delay: Int) {
        val bpm = (MILLIS_PER_MIN.toFloat() / delay).roundToInt()
        delaySubject.onNext((MILLIS_PER_MIN.toFloat() / bpm).roundToInt())
        sharedPreferences.edit().putInt(resources.getString(R.string.pref_delay_key), delay).apply()
        restartIfPlaying()
    }

    private fun setBpm(bpm: Int) {
        delaySubject.onNext((MILLIS_PER_MIN.toFloat() / bpm).roundToInt())
        restartIfPlaying()
    }

    fun togglePlay() {
        if (isPlaying) {
            stop()
        } else {
            play()
        }
    }

    fun incrementBpm() {
        setBpm(millisToBpm(delaySubject.value!!) + 1)
    }

    fun decrementBpm() {
        setBpm(millisToBpm(delaySubject.value!!) - 1)
    }

    private fun play() {
        val millis = delaySubject.value?.toLong() ?: return
        playDisposable = Flowable.interval(millis, TimeUnit.MILLISECONDS, Schedulers.computation())
            .subscribe {
                beat++
                beatCountSubject.onNext(beat)
                val numBeats =
                    (sharedPreferences.getString(resources.getString(R.string.pref_beats_key), null)
                        ?: "8").toInt()
                if (beat >= numBeats) {
                    beat = 0
                }
            }
        playStateSubject.onNext(true)
    }

    private fun stop() {
        playDisposable?.dispose()
        playDisposable = null
        beat = 0
        playStateSubject.onNext(false)
    }

    private fun restartIfPlaying() {
        if (isPlaying) {
            stop()
            play()
        }
    }

    companion object {
        const val MILLIS_PER_MIN = 60000
        const val DEFAULT_DELAY = 500
    }

}
