package com.augustos.augmusic

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.media3.common.Player

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MiniPlayer(
    track: AudioTrack,
    ps: PlayerState,
    controller: MediaController?,
    isFavorite: Boolean,
    onOpenFull: () -> Unit,
    onLongPress: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onTimer: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val duration = (if (ps.durationMs > 0L) ps.durationMs else track.duration).coerceAtLeast(1L)

    Card(
        Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 7.dp)
            .combinedClickable(onClick = onOpenFull, onLongClick = onLongPress)
            .pointerInput(track.id) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragEnd = { if (totalDrag < -35f) onOpenFull(); totalDrag = 0f },
                    onVerticalDrag = { _, amount -> totalDrag += amount }
                )
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AugPanel)
    ) {
        Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }) {
                    Icon(Icons.Default.Shuffle, "Aleatório", tint = if (ps.shuffle) AugLime else AugMuted)
                }
                IconButton(onClick = { controller?.cycleRepeatMode() }) {
                    Icon(
                        if (ps.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Modo de repetição",
                        tint = if (ps.repeatMode == Player.REPEAT_MODE_OFF) AugMuted else AugLime
                    )
                }
            }
            Slider(
                value = (if (isSeeking) seekValue else ps.positionMs.toFloat()).coerceIn(0f, duration.toFloat()),
                onValueChange = { isSeeking = true; seekValue = it },
                onValueChangeFinished = {
                    controller?.seekTo(seekValue.toLong())
                    ps.positionMs = seekValue.toLong()
                    isSeeking = false
                },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(thumbColor = AugLime, activeTrackColor = AugLime)
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatDuration(if (isSeeking) seekValue.toLong() else ps.positionMs), color = AugMuted, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(formatDuration(duration), color = AugMuted, fontSize = 11.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { controller?.seekToPrevious() }) {
                    Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(31.dp))
                }
                Spacer(Modifier.width(10.dp))
                FilledIconButton(
                    onClick = { controller?.togglePlayPause() },
                    modifier = Modifier.size(50.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = AugLime, contentColor = AugInk)
                ) {
                    Icon(
                        if (ps.showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (ps.showPause) "Pausar" else "Reproduzir",
                        modifier = Modifier.size(29.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                IconButton(onClick = { controller?.seekToNext() }) {
                    Icon(Icons.Default.SkipNext, "Próxima", tint = Color.White, modifier = Modifier.size(31.dp))
                }
                IconButton(onClick = onQueue) {
                    Icon(Icons.Default.QueueMusic, "Abrir fila", tint = AugMuted)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLyrics) { Icon(Icons.Default.ChatBubbleOutline, "Letra", tint = AugMuted) }
                IconButton(onClick = onFavorite) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Favorito", tint = if (isFavorite) AugLime else AugMuted
                    )
                }
                IconButton(onClick = onTimer) { Icon(Icons.Default.Timer, "Timer", tint = AugMuted) }
                IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Fila", tint = AugMuted) }
                IconButton(onClick = onEqualizer) { Icon(Icons.Default.Equalizer, "Equalizador", tint = AugLime) }
            }
        }
    }
}

@Composable
internal fun FullPlayerDialog(
    track: AudioTrack,
    ps: PlayerState,
    controller: MediaController?,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onTimer: () -> Unit,
    onSpeed: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit,
    onMore: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    var dominant by remember(track.id) { mutableStateOf(Color(0xFF202820)) }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val duration = (if (ps.durationMs > 0L) ps.durationMs else track.duration).coerceAtLeast(1L)
    val backdrop = rememberArtwork(track, ArtSize.SMALL).value

    // A cor dominante agora vem da capa já carregada e reduzida (antes decodificava a imagem
    // inteira de novo, em tamanho original).
    LaunchedEffect(track.id, track.coverOverride) {
        val bmp = ArtworkLoader.load(context, track, ArtSize.SMALL)
        dominant = if (bmp == null) Color(0xFF202820) else withContext(Dispatchers.Default) {
            try {
                val p = Palette.from(bmp).generate()
                val rgb = p.getVibrantColor(p.getDominantColor(0xFF26362D.toInt()))
                Color(rgb)
            } catch (_: Exception) {
                Color(0xFF202820)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF070907), dominant.copy(alpha = .62f), AugInk)))
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            if (backdrop != null) {
                Image(
                    bitmap = backdrop,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().blur(34.dp)
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.78f), AugInk.copy(.68f), AugInk))))
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    Modifier.fillMaxWidth().pointerInput(track.id) {
                        detectVerticalDragGestures(
                            onDragEnd = { if (dragTotal > 55f) onDismiss(); dragTotal = 0f },
                            onVerticalDrag = { _, amount -> dragTotal += amount }
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.KeyboardArrowDown, "Fechar player", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text("TOCANDO AGORA", color = AugMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "Mais opções", tint = Color.White) }
                }
                Spacer(Modifier.height(12.dp))
                AlbumArtwork(track, modifier = Modifier.fillMaxWidth(.72f).size(250.dp), sizePx = ArtSize.LARGE)
                Spacer(Modifier.height(25.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Color.White.copy(alpha = .72f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito",
                            tint = if (isFavorite) AugLime else Color.White, modifier = Modifier.size(25.dp)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Slider(
                    value = (if (isSeeking) seekValue else ps.positionMs.toFloat()).coerceIn(0f, duration.toFloat()),
                    onValueChange = { isSeeking = true; seekValue = it },
                    onValueChangeFinished = {
                        controller?.seekTo(seekValue.toLong())
                        ps.positionMs = seekValue.toLong()
                        isSeeking = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = AugLime, activeTrackColor = AugLime, inactiveTrackColor = Color.White.copy(alpha = .22f))
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(if (isSeeking) seekValue.toLong() else ps.positionMs), color = AugMuted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(formatDuration(duration), color = AugMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }) {
                        Icon(Icons.Default.Shuffle, "Aleatório", tint = if (ps.shuffle) AugLime else AugMuted)
                    }
                    IconButton(onClick = { controller?.seekToPrevious() }) {
                        Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    FilledIconButton(
                        onClick = { controller?.togglePlayPause() },
                        modifier = Modifier.size(66.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = AugLime, contentColor = AugInk)
                    ) {
                        Icon(
                            if (ps.showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (ps.showPause) "Pausar" else "Reproduzir", modifier = Modifier.size(38.dp)
                        )
                    }
                    IconButton(onClick = { controller?.seekToNext() }) {
                        Icon(Icons.Default.SkipNext, "Próxima", tint = Color.White, modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = { controller?.cycleRepeatMode() }) {
                        Icon(
                            if (ps.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repetir",
                            tint = if (ps.repeatMode == Player.REPEAT_MODE_OFF) AugMuted else AugLime
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLyrics) { Icon(Icons.Default.ChatBubbleOutline, "Letra", tint = Color.White) }
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito",
                            tint = if (isFavorite) AugLime else Color.White
                        )
                    }
                    IconButton(onClick = onTimer) { Icon(Icons.Default.Timer, "Timer", tint = Color.White) }
                    TextButton(onClick = onSpeed) { Text("${ps.speed}×", color = AugLime, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Fila", tint = Color.White) }
                    IconButton(onClick = onEqualizer) { Icon(Icons.Default.Equalizer, "Equalizador", tint = AugLime) }
                }
                val format = (track.path?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.isNotBlank() }
                    ?: track.mimeType?.substringAfterLast('/')?.uppercase()
                    ?: "ÁUDIO")
                // bits por ms == kb/s
                val kbps = if (track.duration > 0L && track.size > 0L) (track.size * 8L / track.duration).coerceAtLeast(1L) else 0L
                Text(
                    if (kbps > 0) "$format · ~$kbps kb/s · arquivo local" else "$format · arquivo local",
                    color = AugMuted, fontSize = 10.sp, letterSpacing = .5.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }
        }
    }
}
