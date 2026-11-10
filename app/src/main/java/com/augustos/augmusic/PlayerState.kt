package com.augustos.augmusic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay

/**
 * Estado observável do player. Cada campo é um State independente: só os composables que leem
 * "positionMs" (mini-player e player completo) recompõem a cada tick — na V10 o loop de 400 ms
 * escrevia estados lidos pela tela inteira, recompondo lista, busca e menus o tempo todo
 * (mesmo com a música pausada).
 */
/** Formato real do áudio em reprodução, lido do decodificador (ExoPlayer). -1 = desconhecido. */
internal data class AudioFormatInfo(val sampleRate: Int, val bitrate: Int, val channels: Int, val mime: String?)

@Stable
internal class PlayerState {
    var audioFormat by mutableStateOf<AudioFormatInfo?>(null)
    var currentMediaId by mutableStateOf<Long?>(null)
    var isPlaying by mutableStateOf(false)
    var showPause by mutableStateOf(false)
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var shuffle by mutableStateOf(false)
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    var speed by mutableFloatStateOf(1f)
    var currentIndex by mutableIntStateOf(-1)
    var queue by mutableStateOf<List<QueueEntry>>(emptyList())

    fun syncBasics(player: Player) {
        currentMediaId = player.currentMediaItem?.mediaId?.toLongOrNull()
        isPlaying = player.isPlaying
        val state = player.playbackState
        showPause = player.playWhenReady && state != Player.STATE_ENDED && state != Player.STATE_IDLE
        shuffle = player.shuffleModeEnabled
        repeatMode = player.repeatMode
        speed = player.playbackParameters.speed
        currentIndex = player.currentMediaItemIndex
        updatePosition(player)
    }

    fun updatePosition(player: Player) {
        val duration = player.duration
        durationMs = if (duration == C.TIME_UNSET) 0L else duration.coerceAtLeast(0L)
        positionMs = player.currentPosition.coerceAtLeast(0L)
    }

    fun syncTracks(player: Player) {
        var info: AudioFormatInfo? = null
        for (group in player.currentTracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue
            for (i in 0 until group.length) {
                if (group.isTrackSelected(i)) {
                    val f = group.getTrackFormat(i)
                    val bitrate = if (f.averageBitrate > 0) f.averageBitrate else f.bitrate
                    info = AudioFormatInfo(f.sampleRate, bitrate, f.channelCount, f.sampleMimeType)
                    break
                }
            }
            if (info != null) break
        }
        audioFormat = info
    }

    fun syncQueue(player: Player) {
        val n = player.mediaItemCount
        val list = ArrayList<QueueEntry>(n)
        for (i in 0 until n) {
            val item = player.getMediaItemAt(i)
            list.add(
                QueueEntry(
                    mediaId = item.mediaId.toLongOrNull(),
                    title = item.mediaMetadata.title?.toString() ?: "Música",
                    artist = item.mediaMetadata.artist?.toString() ?: "Artista desconhecido"
                )
            )
        }
        queue = list
    }
}

@Composable
internal fun rememberPlayerState(controller: MediaController?): PlayerState {
    val state = remember { PlayerState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                state.syncBasics(player)
                if (events.containsAny(Player.EVENT_TIMELINE_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    state.syncQueue(player)
                }
                if (events.containsAny(Player.EVENT_TRACKS_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                    state.syncTracks(player)
                }
            }
        }
        controller.addListener(listener)
        // Ao reconectar (Activity nova com o serviço já tocando), shuffle/repeat/velocidade
        // vêm do player em vez de voltarem ao padrão da tela.
        state.syncBasics(controller)
        state.syncQueue(controller)
        state.syncTracks(controller)
        onDispose { controller.removeListener(listener) }
    }

    // O relógio só roda com a tela visível (STARTED) e só escreve estado se algo mudou.
    LaunchedEffect(controller, lifecycleOwner) {
        if (controller == null) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            state.syncBasics(controller)
            while (true) {
                if (controller.isPlaying) state.updatePosition(controller)
                delay(500)
            }
        }
    }
    return state
}

// ---------- ações comuns de transporte ----------

internal fun Player.togglePlayPause() {
    if (playWhenReady && playbackState != Player.STATE_ENDED && playbackState != Player.STATE_IDLE) {
        pause()
    } else {
        if (playbackState == Player.STATE_IDLE) prepare()
        play()
    }
}

internal fun Player.cycleRepeatMode() {
    repeatMode = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }
}
