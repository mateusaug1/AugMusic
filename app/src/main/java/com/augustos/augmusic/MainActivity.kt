package com.augustos.augmusic

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

private val Ink = Color(0xFF10120F)
private val Panel = Color(0xFF1A1E19)
private val Lime = Color(0xFFB7F36B)
private val Muted = Color(0xFF9BA397)

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri,
    val duration: Long
)

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
        }, ContextCompat.getMainExecutor(this))

        setContent {
            var tracks by remember { mutableStateOf(emptyList<Track>()) }
            var permissionGranted by remember { mutableStateOf(hasAudioPermission()) }
            var search by remember { mutableStateOf("") }
            var selectedTab by remember { mutableStateOf("Início") }
            var current by remember { mutableStateOf<Track?>(null) }
            var playing by remember { mutableStateOf(false) }
            val favorites = remember { mutableStateListOf<Long>() }
            val requestPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                permissionGranted = granted
                if (granted) tracks = loadTracks()
            }

            LaunchedEffect(permissionGranted) {
                if (permissionGranted) tracks = loadTracks()
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Ink,
                    surface = Panel,
                    primary = Lime,
                    onPrimary = Ink,
                    onBackground = Color(0xFFF4F6EF),
                    onSurface = Color(0xFFF4F6EF),
                    secondary = Muted
                )
            ) {
                Surface(Modifier.fillMaxSize(), color = Ink) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("AUGMUSIC", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
                                Text(
                                    when (selectedTab) {
                                        "Biblioteca" -> "Sua biblioteca."
                                        "Favoritas" -> "Só as favoritas."
                                        else -> "Som do seu jeito."
                                    },
                                    fontSize = 27.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Lime, Color(0xFF6E9B48)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, null, tint = Ink, modifier = Modifier.size(25.dp))
                            }
                        }

                        OutlinedTextField(
                            value = search, onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            placeholder = { Text("Buscar músicas, artistas…", color = Muted) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Lime, unfocusedBorderColor = Color(0xFF30372E),
                                focusedContainerColor = Panel, unfocusedContainerColor = Panel
                            )
                        )

                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            listOf("Início", "Biblioteca", "Favoritas").forEach { tab ->
                                FilterChip(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    label = { Text(tab) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Lime,
                                        selectedLabelColor = Ink,
                                        containerColor = Panel,
                                        labelColor = Muted
                                    ),
                                    border = null
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { if (permissionGranted) tracks = loadTracks() else requestPermission.launch(audioPermission()) }) {
                                Icon(Icons.Default.Refresh, "Atualizar biblioteca", tint = Muted)
                            }
                        }

                        val visibleTracks = tracks.filter {
                            val matches = (it.title + " " + it.artist + " " + it.album).contains(search, true)
                            matches && when (selectedTab) {
                                "Favoritas" -> favorites.contains(it.id)
                                else -> true
                            }
                        }

                        if (!permissionGranted) {
                            Column(
                                Modifier.fillMaxSize().padding(28.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.LibraryMusic, null, tint = Lime, modifier = Modifier.size(56.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("Sua música, no seu ritmo", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Permita o acesso ao áudio para encontrar as músicas salvas no aparelho.", color = Muted)
                                Spacer(Modifier.height(18.dp))
                                Button(onClick = { requestPermission.launch(audioPermission()) }) { Text("Permitir acesso") }
                            }
                        } else if (visibleTracks.isEmpty()) {
                            Column(
                                Modifier.fillMaxSize().padding(28.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.MusicOff, null, tint = Lime, modifier = Modifier.size(52.dp))
                                Spacer(Modifier.height(14.dp))
                                Text(if (search.isNotBlank()) "Nada encontrado" else "Ainda está silencioso por aqui", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Text(if (search.isNotBlank()) "Tente outro título ou artista." else "Adicione arquivos de áudio ao aparelho e atualize a biblioteca.", color = Muted)
                            }
                        } else {
                            Text(
                                "${visibleTracks.size} ${if (visibleTracks.size == 1) "faixa" else "faixas"}",
                                color = Muted, fontSize = 12.sp,
                                modifier = Modifier.padding(start = 23.dp, top = 14.dp, bottom = 6.dp)
                            )
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(visibleTracks, key = { it.id }) { track ->
                                    TrackRow(
                                        track = track,
                                        isFavorite = favorites.contains(track.id),
                                        isCurrent = current?.id == track.id,
                                        onPlay = {
                                            current = track
                                            val c = controller
                                            if (c != null) {
                                                val mediaItems = visibleTracks.map { t ->
                                                    MediaItem.Builder().setMediaId(t.id.toString())
                                                        .setUri(t.uri)
                                                        .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).setAlbumTitle(t.album).build())
                                                        .build()
                                                }
                                                val index = visibleTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                                                c.setMediaItems(mediaItems, index, 0L)
                                                c.prepare()
                                                c.play()
                                                playing = true
                                            }
                                        },
                                        onFavorite = {
                                            if (favorites.contains(track.id)) favorites.remove(track.id) else favorites.add(track.id)
                                        }
                                    )
                                }
                            }
                        }

                        current?.let { track ->
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)
                                    .clip(RoundedCornerShape(18.dp)).background(Panel).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(43.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF303B2A)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, null, tint = Lime)
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = {
                                    controller?.let { c ->
                                        if (c.isPlaying) { c.pause(); playing = false } else { c.play(); playing = true }
                                    }
                                }) {
                                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Reproduzir ou pausar", tint = Lime)
                                }
                                IconButton(onClick = { controller?.seekToNextMediaItem() }) {
                                    Icon(Icons.Default.SkipNext, "Próxima", tint = Color.White)
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().background(Color(0xFF0C0E0B)).padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(Icons.Default.Home to "Início", Icons.Default.LibraryMusic to "Biblioteca", Icons.Default.Favorite to "Favoritas").forEach { (icon, label) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedTab = label }.padding(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Icon(icon, null, tint = if (selectedTab == label) Lime else Muted)
                                    Text(label, fontSize = 10.sp, color = if (selectedTab == label) Lime else Muted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun audioPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun loadTracks(): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val result = mutableListOf<Track>()
        try {
            contentResolver.query(
                collection, projection, selection, null,
                "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    result += Track(
                        id = id,
                        title = cursor.getString(titleCol) ?: "Faixa sem título",
                        artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: "Artista desconhecido",
                        album = cursor.getString(albumCol)?.takeIf { it != "<unknown>" } ?: "Álbum desconhecido",
                        uri = Uri.withAppendedPath(collection, id.toString()),
                        duration = cursor.getLong(durationCol)
                    )
                }
            }
        } catch (_: SecurityException) {
        }
        return result
    }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onDestroy()
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isFavorite: Boolean,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
            .background(if (isCurrent) Color(0xFF222B1D) else Color.Transparent)
            .clickable(onClick = onPlay).padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(49.dp).clip(RoundedCornerShape(13.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF35452D), Color(0xFF202820)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Album, null, tint = if (isCurrent) Lime else Color(0xFFB8C7AE), modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (isCurrent) Lime else Color.White)
            Text(track.artist, fontSize = 12.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavorite) {
            Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favoritar", tint = if (isFavorite) Lime else Muted, modifier = Modifier.size(20.dp))
        }
        Icon(Icons.Default.PlayArrow, null, tint = if (isCurrent) Lime else Muted, modifier = Modifier.padding(end = 4.dp))
    }
}
