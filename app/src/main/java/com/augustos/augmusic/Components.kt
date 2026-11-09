package com.augustos.augmusic

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import android.content.Context

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

@Composable
internal fun AlbumArtwork(
    track: AudioTrack,
    size: Dp = Dp.Unspecified,
    modifier: Modifier = Modifier,
    sizePx: Int = ArtSize.SMALL
) {
    val bitmap = rememberArtwork(track, sizePx).value
    val sized = if (size != Dp.Unspecified) modifier.size(size) else modifier
    Box(
        sized.clip(RoundedCornerShape(16.dp)).background(AugPanel),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap == null) {
            Icon(
                Icons.Default.MusicNote, null,
                tint = AugMuted.copy(alpha = .8f),
                modifier = Modifier.size(if (size != Dp.Unspecified) size * .48f else 40.dp)
            )
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
internal fun ArtistArtwork(track: AudioTrack, size: Dp) {
    val bitmap = rememberArtwork(track, ArtSize.MEDIUM).value
    Box(Modifier.size(size).clip(CircleShape).background(AugPanel), contentAlignment = Alignment.Center) {
        if (bitmap == null) {
            Icon(Icons.Default.Person, null, tint = AugMuted, modifier = Modifier.size(size * .55f))
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

@Composable
internal fun MenuAction(icon: ImageVector, title: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Icon(icon, null, tint = AugLime)
        Spacer(Modifier.width(14.dp))
        Text(title, Modifier.weight(1f), color = Color.White)
    }
}

/**
 * Slider vertical. A V10 usava um Slider horizontal com width(30.dp) e fillMaxHeight(), o que
 * produzia um controle de 30 dp de largura em vez de uma barra vertical.
 * Gira o slider em 270° e troca as restrições de medida.
 */
internal fun Modifier.verticalSlider(): Modifier = this
    .graphicsLayer {
        rotationZ = 270f
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
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
