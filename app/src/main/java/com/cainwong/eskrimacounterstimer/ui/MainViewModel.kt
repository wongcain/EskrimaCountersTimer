package com.cainwong.eskrimacounterstimer.ui

import android.content.res.Resources
import androidx.lifecycle.*
import com.cainwong.eskrimacounterstimer.R
import com.cainwong.eskrimacounterstimer.core.Metronome
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainViewModel(
    private val resources: Resources,
    private val metronome: Metronome
) : ViewModel() {

    private val disposables = CompositeDisposable()

    val playState = MutableLiveData<Boolean>()

    val playStopIconResId:LiveData<Int> = Transformations.map(playState) {isPlaying ->
        if (isPlaying) {
            R.drawable.ic_stop_white_24dp
        } else {
            R.drawable.ic_play_arrow_white_24dp
        }
    }

    private val tapTempoSubject = PublishSubject.create<Long>()

    private val tempoBpm = MutableLiveData<Int>()

    val tempoText: LiveData<String> = Transformations.map(tempoBpm) {
        resources.getString(R.string.tempo_disp, it)
    }

    init {
        disposables.addAll(
            metronome.playState
                .subscribe(playState::postValue, Timber::e),
            metronome.tempoBpm
                .subscribe(tempoBpm::postValue, Timber::e),
            tapTempoSubject
                .timeInterval()
                .map { it.time(TimeUnit.MILLISECONDS).toInt() }
                .filter { millis ->
                    millis in MIN_DELAY..MAX_DELAY
                }
                .subscribe(metronome::setDelay, Timber::e)
        )
    }

    override fun onCleared() {
        super.onCleared()
        disposables.clear()
    }

    fun onPlayStopClicked() {
        metronome.togglePlay()
    }

    fun onTapTempo() {
        tapTempoSubject.onNext(System.currentTimeMillis())
    }

    fun onPlus() {
        metronome.incrementBpm()
    }

    fun onMinus() {
        metronome.decrementBpm()
    }

    companion object {
        const val MIN_DELAY = 215L
        const val MAX_DELAY = 3000L
        const val MILLIS_PER_BPM = 60000L

    }
}