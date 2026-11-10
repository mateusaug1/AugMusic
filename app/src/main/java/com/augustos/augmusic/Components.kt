@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.augustos.augmusic

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

// ------------------------------------------------------------------ capas

@Composable
internal fun rememberArtwork(track: AudioTrack, sizePx: Int): State<ImageBitmap?> {
    val appContext = LocalContext.current.applicationContext
    return produceState<ImageBitmap?>(
        initialValue = ArtworkLoader.peek(track, sizePx)?.asImageBitmap(),
        track.id, track.coverOverride, sizePx
    ) {
        value = ArtworkLoader.load(appContext, track, sizePx)?.asImageBitmap()
    }
}

/** Placeholder com a identidade do WMusic (gradiente da cor de destaque + "W"), usado quando não há capa. */
@Composable
internal fun CoverPlaceholder(modifier: Modifier = Modifier) {
    val c = WC
    BoxWithConstraints(
        modifier.background(
            Brush.linearGradient(listOf(c.accent.copy(alpha = 0.92f), lerp(c.accent, c.bg, 0.72f)))
        ),
        contentAlignment = Alignment.Center
    ) {
        val side = minOf(maxWidth, maxHeight)
        val fontSize = with(LocalDensity.current) { (side * 0.46f).toSp() }
        Text("W", color = c.onAccent.copy(alpha = 0.88f), fontSize = fontSize, fontWeight = FontWeight.Black)
        Icon(
            Icons.Default.MusicNote, null,
            tint = c.onAccent.copy(alpha = 0.55f),
            modifier = Modifier.align(Alignment.BottomEnd).padding(side * 0.09f).size(side * 0.22f)
        )
    }
}

@Composable
internal fun ArtworkImage(
    track: AudioTrack,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    sizePx: Int = ArtSize.SMALL
) {
    val bitmap = rememberArtwork(track, sizePx).value
    Box(modifier.clip(shape)) {
        if (bitmap == null) {
            CoverPlaceholder(Modifier.fillMaxSize())
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = "Capa de ${track.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
internal fun ArtistAvatar(track: AudioTrack, size: Dp) {
    val c = WC
    val bitmap = rememberArtwork(track, ArtSize.MEDIUM).value
    Box(Modifier.size(size).clip(CircleShape).background(c.surfaceHigh), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            Icon(Icons.Default.Person, null, tint = c.muted, modifier = Modifier.size(size * 0.5f))
        } else {
            Image(
                bitmap = bitmap,
                contentDescription = "Imagem de ${track.artist}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ------------------------------------------------------------------ blocos de interface

@Composable
internal fun SectionTitle(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val c = WC
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = c.accentFg, fontSize = 13.sp) }
        }
    }
}

@Composable
internal fun WChip(label: String, selected: Boolean, onClick: () -> Unit, icon: ImageVector? = null) {
    val c = WC
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) c.accent else c.surfaceHigh,
        border = if (selected) null else BorderStroke(1.dp, c.outline)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = if (selected) c.onAccent else c.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label, color = if (selected) c.onAccent else c.text,
                fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1
            )
        }
    }
}

@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val c = WC
    Box(modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(72.dp).clip(CircleShape).background(c.accentSoft),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = c.accentFg, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(14.dp))
            Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            if (action != null) {
                Spacer(Modifier.height(14.dp))
                action()
            }
        }
    }
}

/** Linha de menu (sheets e menus contextuais). */
@Composable
internal fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val c = WC
    val tint = if (destructive) c.danger else c.accentFg
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (destructive) c.danger else c.text, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

internal val ScreenPadding = PaddingValues(horizontal = 18.dp)

/**
 * Slider vertical (equalizador). Gira o slider em 270° e troca as restrições de medida.
 */
internal fun Modifier.verticalSlider(): Modifier = this
    .graphicsLayer {
        rotationZ = 270f
        transformOrigin = TransformOrigin(0f, 0f)
    }
    .layout { measurable, constraints ->
        val placeable = measurable.measure(
            Constraints(
                minWidth = constraints.minHeight,
                maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth,
                maxHeight = constraints.maxWidth
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(-placeable.width, 0)
        }
    }

internal fun toMediaItem(context: Context, track: AudioTrack): MediaItem = MediaItem.Builder()
    .setMediaId(track.id.toString())
    .setUri(track.uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(ArtworkProvider.uriFor(context, track))
            .build()
    ).build()
