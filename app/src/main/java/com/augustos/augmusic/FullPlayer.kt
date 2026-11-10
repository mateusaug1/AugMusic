@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.augustos.augmusic

import android.os.Build
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

internal enum class PlayerMenuAction { DETAILS, EDIT, SHARE, PLAYLIST, RINGTONE, LOCATE, LYRICS, SETTINGS }

// ------------------------------------------------------------------ fundo dinâmico

private val paletteCache = LruCache<Long, Int>(200)

/** Cor predominante da capa; calculada UMA vez por faixa (cache) e fora da thread principal. */
@Composable
private fun rememberDominantColor(track: AudioTrack, enabled: Boolean): Color? {
    val context = LocalContext.current.applicationContext
    val state = produceState<Color?>(
        initialValue = if (enabled) paletteCache.get(track.id)?.let { Color(it) } else null,
        track.id, track.coverOverride, enabled
    ) {
        if (!enabled) { value = null; return@produceState }
        paletteCache.get(track.id)?.let { value = Color(it); return@produceState }
        val bmp = ArtworkLoader.load(context, track, ArtSize.SMALL)
        if (bmp == null) { value = null; return@produceState }
        val rgb = withContext(Dispatchers.Default) {
            try {
                val p = Palette.from(bmp).maximumColorCount(12).generate()
                p.getVibrantColor(p.getMutedColor(p.getDominantColor(0)))
            } catch (_: Exception) {
                0
            }
        }
        if (rgb != 0) { paletteCache.put(track.id, rgb); value = Color(rgb) } else value = null
    }
    return state.value
}

/** Mistura a cor da capa com o fundo do tema até o texto atingir contraste 4,5:1 (legibilidade garantida). */
private fun readableBackdrop(dominant: Color, c: WColors): Color {
    var color = lerp(c.bg, dominant, if (c.dark) 0.55f else 0.45f)
    repeat(8) {
        if (contrastRatio(c.text, color) >= 4.5f) return color
        color = lerp(color, c.bg, 0.2f)
    }
    return c.bg
}

@Composable
private fun PlayerBackdrop(track: AudioTrack, settings: AppSettings) {
    val c = WC
    val mode = settings.backgroundMode
    val dominant = rememberDominantColor(track, mode == BackgroundMode.DYNAMIC)
    val target = remember(dominant, c) {
        dominant?.let { readableBackdrop(it, c) } ?: (if (c.useGradients) c.gradTop else c.bg)
    }
    // Transição suave entre músicas (a cor só muda quando a faixa muda; nada anima continuamente).
    val top by animateColorAsState(target, tween(700), label = "backdrop")
    when (mode) {
        BackgroundMode.SOLID -> Box(Modifier.fillMaxSize().background(c.bg))
        BackgroundMode.GRADIENT ->
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.gradTop, c.bg))))
        BackgroundMode.DYNAMIC ->
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, lerp(top, c.bg, 0.55f), c.bg))))
        BackgroundMode.BLUR -> {
            val bmp = rememberArtwork(track, ArtSize.SMALL).value
            Box(Modifier.fillMaxSize().background(c.bg)) {
                if (bmp != null) {
                    Image(
                        bitmap = bmp, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(28.dp) else Modifier
                        )
                    )
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(c.bg.copy(alpha = 0.55f), c.bg.copy(alpha = 0.86f), c.bg))
                    )
                )
            }
        }
    }
}

// ------------------------------------------------------------------ gestos (Behavior)

/**
 * Gestos da capa configurados em Configurações > Behavior.
 * Só atuam sobre a capa: o slider, os botões e o cabeçalho têm seus próprios toques, e o
 * arrastar para fechar fica restrito ao cabeçalho — por isso não há conflito entre eles.
 */
@Composable
internal fun rememberCoverGestures(settings: AppSettings, onAction: (GestureAction) -> Unit): Modifier {
    val act by rememberUpdatedState(onAction)
    val sr = settings.swipeRight
    val sl = settings.swipeLeft
    val su = settings.swipeUp
    val sd = settings.swipeDown
    val dt = settings.doubleTap
    val lp = settings.longPress
    fun isVolume(a: GestureAction) = a == GestureAction.VOLUME_UP || a == GestureAction.VOLUME_DOWN
    return Modifier
        .pointerInput(dt, lp) {
            detectTapGestures(
                onDoubleTap = { if (dt.enabled) act(dt.action) },
                onLongPress = { if (lp.enabled) act(lp.action) }
            )
        }
        .pointerInput(sr, sl, su, sd) {
            val slop = viewConfiguration.touchSlop * 2f
            val step = 56.dp.toPx()
            val minSwipe = 64.dp.toPx()
            var total = Offset.Zero
            var vertical: Boolean? = null
            var acc = 0f
            detectDragGestures(
                onDragStart = { total = Offset.Zero; vertical = null; acc = 0f },
                onDragEnd = {
                    val isVertical = vertical
                    if (isVertical == false && abs(total.x) >= minSwipe) {
                        val g = if (total.x > 0f) sr else sl
                        if (g.enabled) act(g.action)
                    } else if (isVertical == true && abs(total.y) >= minSwipe) {
                        val g = if (total.y < 0f) su else sd
                        // volume é contínuo (dispara durante o arrasto); as demais ações disparam ao soltar
                        if (g.enabled && !isVolume(g.action)) act(g.action)
                    }
                },
                onDragCancel = { vertical = null },
                onDrag = { _, amount ->
                    total += amount
                    if (vertical == null && (abs(total.x) > slop || abs(total.y) > slop)) {
                        vertical = abs(total.y) > abs(total.x)
                    }
                    if (vertical == true) {
                        acc += amount.y
                        if (acc <= -step) {
                            acc += step
                            if (su.enabled && isVolume(su.action)) act(su.action)
                        } else if (acc >= step) {
                            acc -= step
                            if (sd.enabled && isVolume(sd.action)) act(sd.action)
                        }
                    }
                }
            )
        }
}

// ------------------------------------------------------------------ peças do player

@Composable
private fun ColumnScope.Flex(compact: Boolean, fixed: Dp, weight: Float) {
    if (compact) Spacer(Modifier.height(fixed)) else Spacer(Modifier.weight(weight))
}

@Composable
private fun PlayerCover(
    track: AudioTrack,
    side: Dp,
    settings: AppSettings,
    playing: Boolean,
    gestures: Modifier
) {
    val c = WC
    val shape = RoundedCornerShape(settings.coverCorner.dp)
    // "Zoom discreto": a capa respira levemente ao pausar/tocar (desligável nas configurações).
    val scale by animateFloatAsState(
        targetValue = if (!settings.coverAnimation || playing) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "coverScale"
    )
    Box(
        Modifier.size(side)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (settings.coverStyle == CoverStyle.ELEVATED) Modifier.shadow(22.dp, shape, clip = false) else Modifier
            )
            .then(
                if (settings.coverStyle == CoverStyle.FRAMED) Modifier.border(2.dp, c.accent.copy(alpha = 0.75f), shape)
                else Modifier
            )
            .then(gestures)
    ) {
        if (settings.coverAnimation) {
            AnimatedContent(
                targetState = track,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "coverSwap"
            ) { t -> ArtworkImage(t, Modifier.fillMaxSize(), shape, ArtSize.LARGE) }
        } else {
            ArtworkImage(track, Modifier.fillMaxSize(), shape, ArtSize.LARGE)
        }
    }
}

@Composable
private fun PlayButton(style: ButtonStyle, size: Dp, showPause: Boolean, onClick: () -> Unit) {
    val c = WC
    val shape = if (style == ButtonStyle.SQUARE) RoundedCornerShape(size * 0.32f) else CircleShape
    val filled = style != ButtonStyle.OUTLINED
    Surface(
        onClick = onClick,
        modifier = Modifier.size(size),
        shape = shape,
        color = if (filled) c.accent else Color.Transparent,
        contentColor = if (filled) c.onAccent else c.accentFg,
        border = if (filled) null else BorderStroke(2.dp, c.accentFg),
        shadowElevation = if (filled) 8.dp else 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (showPause) Icons.Default.Pause else Icons.Default.PlayArrow,
                if (showPause) "Pausar" else "Reproduzir",
                modifier = Modifier.size(size * 0.5f)
            )
        }
    }
}

@Composable
private fun Segment(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val c = WC
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(if (active) c.accentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, label, tint = if (active) c.accentFg else c.muted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (active) c.accentFg else c.muted, fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.Medium)
    }
}

private fun formatKhz(hz: Int): String =
    if (hz % 1000 == 0) "${hz / 1000} kHz" else String.format(java.util.Locale.ROOT, "%.1f kHz", hz / 1000.0)

/** Somente dados reais: formato do arquivo, bitrate/frequência do decodificador e origem. */
private fun techInfo(track: AudioTrack, fmt: AudioFormatInfo?): String {
    val parts = ArrayList<String>()
    val ext = track.path?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.isNotBlank() && it.length <= 5 }
        ?: track.mimeType?.substringAfterLast('/')?.uppercase()?.takeIf { it.isNotBlank() }
    if (ext != null) parts.add(ext)
    val realKbps = fmt?.bitrate?.takeIf { it > 0 }?.let { it / 1000 }
    if (realKbps != null && realKbps > 0) {
        parts.add("$realKbps kbps")
    } else if (track.duration > 0L && track.size > 0L) {
        val approx = (track.size * 8L / track.duration).toInt() // bits/ms == kb/s
        if (approx > 0) parts.add("~$approx kbps")
    }
    fmt?.sampleRate?.takeIf { it > 0 }?.let { parts.add(formatKhz(it)) }
    parts.add("Arquivo local")
    return parts.joinToString(" · ")
}

private fun repeatLabel(mode: Int) = when (mode) {
    Player.REPEAT_MODE_ALL -> "Playlist"
    Player.REPEAT_MODE_ONE -> "Música"
    else -> "Repetir"
}

// ------------------------------------------------------------------ player completo

@Composable
internal fun FullPlayer(
    track: AudioTrack,
    ps: PlayerState,
    controller: MediaController?,
    settings: AppSettings,
    isFavorite: Boolean,
    sleepActive: Boolean,
    onClose: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onTimer: () -> Unit,
    onSpeed: () -> Unit,
    onQueue: () -> Unit,
    onEqualizer: () -> Unit,
    onMenuAction: (PlayerMenuAction) -> Unit,
    onGesture: (GestureAction) -> Unit
) {
    val c = WC
    BackHandler(onBack = onClose)
    var menuOpen by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val duration = (if (ps.durationMs > 0L) ps.durationMs else track.duration).coerceAtLeast(1L)
    val gestures = rememberCoverGestures(settings, onGesture)
    val scale = settings.controlsScalePct / 100f

    Box(Modifier.fillMaxSize()) {
        PlayerBackdrop(track, settings)
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val compact = maxHeight < 640.dp || LocalDensity.current.fontScale > 1.15f
            val side = minOf(
                maxWidth - 48.dp,
                maxWidth * (settings.coverSizePct / 100f),
                maxHeight * (if (compact) 0.34f else 0.40f)
            ).coerceAtLeast(120.dp)

            Column(
                Modifier.fillMaxSize().padding(horizontal = 24.dp)
                    .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---- cabeçalho
                var dragTotal by remember { mutableFloatStateOf(0f) }
                Row(
                    Modifier.fillMaxWidth().pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = { if (dragTotal > 60f) onClose(); dragTotal = 0f },
                            onVerticalDrag = { _, amount -> dragTotal += amount }
                        )
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.KeyboardArrowDown, "Minimizar player", tint = c.text, modifier = Modifier.size(32.dp))
                    }
                    Text(
                        "TOCANDO AGORA", color = c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.4.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, "Mais opções", tint = c.text)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            @Composable
                            fun item(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, a: PlayerMenuAction) {
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    leadingIcon = { Icon(icon, null, tint = c.accentFg) },
                                    onClick = { menuOpen = false; onMenuAction(a) }
                                )
                            }
                            item("Detalhes da música", Icons.Default.Info, PlayerMenuAction.DETAILS)
                            item("Editar informações", Icons.Default.Edit, PlayerMenuAction.EDIT)
                            item("Compartilhar", Icons.Default.Share, PlayerMenuAction.SHARE)
                            item("Adicionar à playlist", Icons.Default.PlaylistAdd, PlayerMenuAction.PLAYLIST)
                            item("Definir como toque", Icons.Default.PlayCircle, PlayerMenuAction.RINGTONE)
                            item("Abrir localização do arquivo", Icons.Default.FolderOpen, PlayerMenuAction.LOCATE)
                            item("Gerenciar letras", Icons.Default.Subtitles, PlayerMenuAction.LYRICS)
                            item("Configurações do player", Icons.Default.Settings, PlayerMenuAction.SETTINGS)
                        }
                    }
                }

                Flex(compact, 8.dp, 0.5f)
                // ---- capa
                PlayerCover(track, side, settings, ps.showPause, gestures)
                Flex(compact, 20.dp, 1f)

                // ---- informações + favorito
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title, color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                            lineHeight = 29.sp, maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        if (settings.showArtist) {
                            Text(track.artist, color = c.text.copy(alpha = 0.78f), fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (settings.showAlbum && track.album.isNotBlank()) {
                            Text(track.album, color = c.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconButton(onClick = onFavorite, modifier = Modifier.size(52.dp)) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                            tint = if (isFavorite) c.accentFg else c.text, modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // ---- progresso
                Spacer(Modifier.height(10.dp))
                Slider(
                    value = (if (isSeeking) seekValue else ps.positionMs.toFloat()).coerceIn(0f, duration.toFloat()),
                    onValueChange = { isSeeking = true; seekValue = it },
                    onValueChangeFinished = {
                        controller?.seekTo(seekValue.toLong())
                        ps.positionMs = seekValue.toLong()
                        isSeeking = false
                    },
                    valueRange = 0f..duration.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = c.accent, activeTrackColor = c.accent,
                        inactiveTrackColor = c.text.copy(alpha = 0.18f)
                    )
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(if (isSeeking) seekValue.toLong() else ps.positionMs), color = c.muted, fontSize = 12.sp)
                    Spacer(Modifier.weight(1f))
                    if (settings.showDuration) Text(formatDuration(duration), color = c.muted, fontSize = 12.sp)
                }

                // ---- controles principais
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { controller?.seekToPrevious() }, modifier = Modifier.size(64.dp * scale)) {
                        Icon(Icons.Default.SkipPrevious, "Anterior", tint = c.text, modifier = Modifier.size(40.dp * scale))
                    }
                    PlayButton(settings.buttonStyle, 74.dp * scale, ps.showPause) { controller?.togglePlayPause() }
                    IconButton(onClick = { controller?.seekToNext() }, modifier = Modifier.size(64.dp * scale)) {
                        Icon(Icons.Default.SkipNext, "Próxima", tint = c.text, modifier = Modifier.size(40.dp * scale))
                    }
                }

                // ---- controles secundários (segmentado)
                if (settings.showSecondaryControls) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                            .background(c.surfaceHigh.copy(alpha = if (c.dark) 0.65f else 0.9f)).padding(4.dp)
                    ) {
                        Segment(Icons.Default.Shuffle, "Aleatório", ps.shuffle, Modifier.weight(1f)) {
                            controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
                        }
                        Segment(
                            if (ps.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            repeatLabel(ps.repeatMode), ps.repeatMode != Player.REPEAT_MODE_OFF, Modifier.weight(1f)
                        ) { controller?.cycleRepeatMode() }
                        Segment(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            "Favorito", isFavorite, Modifier.weight(1f), onFavorite
                        )
                    }
                }

                // ---- ferramentas
                Flex(compact, 10.dp, 0.4f)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (settings.lyricsEnabled) {
                        IconButton(onClick = onLyrics) { Icon(Icons.Default.ChatBubbleOutline, "Letras", tint = c.muted) }
                    }
                    IconButton(onClick = onTimer) {
                        Icon(Icons.Default.Timer, "Temporizador de desligamento", tint = if (sleepActive) c.accentFg else c.muted)
                    }
                    TextButton(onClick = onSpeed) {
                        Text("${ps.speed}×", color = if (ps.speed != 1f) c.accentFg else c.muted, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Fila de reprodução", tint = c.muted) }
                    IconButton(onClick = onEqualizer) { Icon(Icons.Default.Equalizer, "Equalizador", tint = c.muted) }
                }

                if (settings.showTechInfo) {
                    Text(
                        techInfo(track, ps.audioFormat),
                        color = c.muted, fontSize = 11.sp, letterSpacing = 0.4.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
