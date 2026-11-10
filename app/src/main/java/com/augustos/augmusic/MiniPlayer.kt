@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.augustos.augmusic

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.session.MediaController

/**
 * Mini Player: fica na barra inferior (acima da navegação) durante TODAS as telas enquanto houver
 * faixa carregada. Aparência, informações, botões e comportamento ao tocar vêm das configurações.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniPlayer(
    track: AudioTrack,
    ps: PlayerState,
    controller: MediaController?,
    settings: AppSettings,
    onOpenFull: () -> Unit,
    onMenu: () -> Unit
) {
    val c = WC
    val style = settings.miniStyle
    val shape = when (style) {
        MiniStyle.FLOATING -> RoundedCornerShape(22.dp)
        MiniStyle.COMPACT -> RoundedCornerShape(16.dp)
        MiniStyle.BAR -> RoundedCornerShape(0.dp)
    }
    val cover = if (style == MiniStyle.COMPACT) 40.dp else 50.dp
    val outer = if (style == MiniStyle.BAR) Modifier.fillMaxWidth()
    else Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)

    Surface(
        modifier = outer.pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(
                onDragEnd = { if (total < -40f) onOpenFull(); total = 0f },
                onVerticalDrag = { _, amount -> total += amount }
            )
        },
        shape = shape,
        color = c.surfaceHigh,
        shadowElevation = if (style == MiniStyle.FLOATING) 10.dp else 0.dp,
        border = BorderStroke(1.dp, c.outline)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            when (settings.miniTap) {
                                MiniTap.OPEN_PLAYER -> onOpenFull()
                                MiniTap.PLAY_PAUSE -> controller?.togglePlayPause()
                            }
                        },
                        onLongClick = onMenu
                    )
                    .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (settings.miniShowCover) {
                    ArtworkImage(track, Modifier.size(cover), RoundedCornerShape(if (style == MiniStyle.COMPACT) 10.dp else 12.dp))
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    if (settings.miniShowTitle) {
                        Text(track.title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (settings.miniShowArtist && style != MiniStyle.COMPACT) {
                        Text(track.artist, color = c.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Surface(
                    onClick = { controller?.togglePlayPause() },
                    shape = CircleShape,
                    color = c.accent,
                    contentColor = c.onAccent,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (ps.showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (ps.showPause) "Pausar" else "Reproduzir",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                if (settings.miniShowNext) {
                    IconButton(onClick = { controller?.seekToNext() }) {
                        Icon(Icons.Default.SkipNext, "Próxima música", tint = c.text)
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
            }
            if (settings.miniShowProgress) {
                val dur = (if (ps.durationMs > 0L) ps.durationMs else track.duration).coerceAtLeast(1L)
                LinearProgressIndicator(
                    progress = { (ps.positionMs.toFloat() / dur).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                    color = c.accent,
                    trackColor = c.outline,
                    drawStopIndicator = {}
                )
            }
        }
    }
}
