package com.augustos.augmusic

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal enum class SleepMode { OFF, TIMED, END_OF_TRACK }

internal data class SleepState(val mode: SleepMode = SleepMode.OFF, val endsAtElapsedMs: Long = 0L)

/**
 * Timer de soneca. Antes era uma coroutine presa à tela: se o Android destruísse a Activity
 * (ou o usuário fechasse o app com a música tocando), o timer sumia e a música nunca parava.
 * Agora ele vive no MusicService, junto com o player.
 * "Fim da música" usa os eventos do player em vez de um delay calculado — continua correto
 * se o usuário pausar, arrastar a barra ou pular de faixa.
 */
internal object SleepTimer {
    private val handler = Handler(Looper.getMainLooper())
    private val _state = MutableStateFlow(SleepState())
    val state: StateFlow<SleepState> = _state.asStateFlow()
    private var player: Player? = null

    private val fire = Runnable {
        player?.pause()
        _state.value = SleepState()
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (_state.value.mode != SleepMode.END_OF_TRACK) return
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
                reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT
            ) {
                player?.pause()
                _state.value = SleepState()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED && _state.value.mode == SleepMode.END_OF_TRACK) {
                _state.value = SleepState()
            }
        }
    }

    fun bind(target: Player) {
        player?.removeListener(listener)
        player = target
        target.addListener(listener)
    }

    fun unbind() {
        cancel()
        player?.removeListener(listener)
        player = null
    }

    fun startTimed(minutes: Int) {
        handler.removeCallbacks(fire)
        val delayMs = minutes * 60_000L
        _state.value = SleepState(SleepMode.TIMED, SystemClock.elapsedRealtime() + delayMs)
        handler.postDelayed(fire, delayMs)
    }

    fun startEndOfTrack() {
        handler.removeCallbacks(fire)
        _state.value = SleepState(SleepMode.END_OF_TRACK)
    }

    fun cancel() {
        handler.removeCallbacks(fire)
        _state.value = SleepState()
    }
}
