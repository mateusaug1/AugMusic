package com.augustos.augmusic

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import java.util.concurrent.ListenableFuture

private val AugInk = Color(0xFF10120F)
private val AugPanel = Color(0xFF1A1E19)
private val AugLime = Color(0xFFB7F36B)
private val AugMuted = Color(0xFF9BA397)

private data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: Uri,
    val duration: Long,
    val size: Long,
    val dateAdded: Long
)

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controllerState = mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, MusicService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controllerState.value = future.get()
            } catch (_: Exception) {
                controllerState.value = null
            }
        }, mainExecutor)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AugLime,
                    onPrimary = AugInk,
                    background = AugInk,
                    surface = AugPanel,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                AugMusicScreen(controller = controllerState.value, loadTracks = { readAudioTracks() })
            }
        }
    }

    override fun onDestroy() {
        val future = controllerFuture
        controllerFuture = null
        if (future != null) {
            MediaController.releaseFuture(future)
        }
        controllerState.value = null
        super.onDestroy()
    }

    private fun readAudioTracks(): List<AudioTrack> {
        val result = mutableListOf<AudioTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        contentResolver.query(
            collection, projection, selection, null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result += AudioTrack(
                    id,
                    cursor.getString(titleCol)?.takeIf(String::isNotBlank) ?: "Música sem título",
                    cursor.getString(artistCol)?.takeIf(String::isNotBlank) ?: "Artista desconhecido",
                    cursor.getString(albumCol)?.takeIf(String::isNotBlank) ?: "Álbum desconhecido",
                    ContentUris.withAppendedId(collection, id),
                    cursor.getLong(durationCol),
                    cursor.getLong(sizeCol),
                    cursor.getLong(dateCol)
                )
            }
        }
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AugMusicScreen(controller: MediaController?, loadTracks: () -> List<AudioTrack>) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf(emptyList<AudioTrack>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf("Músicas") }
    var menuTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var detailsTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var currentTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val favorites = remember { mutableStateListOf<Long>() }

    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) tracks = loadTracks()
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) tracks = loadTracks() else permissionLauncher.launch(permission)
    }

    DisposableEffect(controller, tracks) {
        if (controller == null) return@DisposableEffect onDispose { }
        fun syncCurrent() {
            val mediaId = controller.currentMediaItem?.mediaId?.toLongOrNull()
            currentTrack = tracks.firstOrNull { it.id == mediaId } ?: currentTrack
            isPlaying = controller.isPlaying
            durationMs = controller.duration.coerceAtLeast(0L)
            positionMs = controller.currentPosition.coerceAtLeast(0L)
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { syncCurrent() }
            override fun onPlaybackStateChanged(playbackState: Int) { syncCurrent() }
        }
        controller.addListener(listener)
        syncCurrent()
        onDispose { controller.removeListener(listener) }
    }

    LaunchedEffect(controller) {
        while (true) {
            if (controller != null && !isSeeking) {
                positionMs = controller.currentPosition.coerceAtLeast(0L)
                durationMs = controller.duration.coerceAtLeast(0L)
                isPlaying = controller.isPlaying
                val id = controller.currentMediaItem?.mediaId?.toLongOrNull()
                currentTrack = tracks.firstOrNull { it.id == id } ?: currentTrack
            }
            delay(400)
        }
    }

    val visibleTracks = tracks.filter { track ->
        val matchesSearch = "${track.title} ${track.artist} ${track.album}".contains(search, true)
        matchesSearch && (selectedTab != "Favoritos" || favorites.contains(track.id))
    }

    fun toMediaItem(track: AudioTrack): MediaItem = MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .build()
        ).build()

    fun playTrack(track: AudioTrack, queue: List<AudioTrack> = visibleTracks) {
        val player = controller ?: return
        val safeQueue = (queue.ifEmpty { tracks }).ifEmpty { listOf(track) }
        val index = safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player.setMediaItems(safeQueue.map(::toMediaItem), index, 0L)
        player.shuffleModeEnabled = shuffleEnabled
        player.repeatMode = repeatMode
        player.prepare()
        player.play()
        currentTrack = track
        isPlaying = true
    }

    Scaffold(
        containerColor = AugInk,
        bottomBar = {
            NavigationBar(containerColor = AugPanel) {
                listOf("Músicas", "Favoritos", "Fila").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab || (tab == "Fila" && showQueue),
                        onClick = { if (tab == "Fila") showQueue = true else selectedTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    "Favoritos" -> Icons.Default.Favorite
                                    "Fila" -> Icons.Default.QueueMusic
                                    else -> Icons.Default.LibraryMusic
                                }, contentDescription = tab
                            )
                        },
                        label = { Text(tab) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AugLime,
                            selectedTextColor = AugLime,
                            indicatorColor = AugLime.copy(alpha = .12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("AUG", color = AugLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                    Text("MUSIC", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                }
                IconButton(onClick = {
                    if (hasPermission) tracks = loadTracks() else permissionLauncher.launch(permission)
                }) { Icon(Icons.Default.Refresh, "Atualizar biblioteca", tint = AugLime) }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar música, artista ou álbum", color = AugMuted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = AugMuted) },
                trailingIcon = {
                    if (search.isNotEmpty()) IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, "Limpar busca")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AugLime,
                    unfocusedBorderColor = AugPanel,
                    focusedContainerColor = AugPanel,
                    unfocusedContainerColor = AugPanel
                )
            )

            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selectedTab == "Favoritos") "FAVORITOS" else "SUA BIBLIOTECA",
                    color = AugMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Spacer(Modifier.weight(1f))
                Text("${visibleTracks.size} faixas", color = AugMuted, fontSize = 12.sp)
            }

            when {
                !hasPermission -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(onClick = { permissionLauncher.launch(permission) }) { Text("Permitir acesso às músicas") }
                }
                visibleTracks.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.MusicOff, null, tint = AugMuted, modifier = Modifier.size(42.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(if (tracks.isEmpty()) "Nenhuma música encontrada" else "Nada encontrado", color = Color.White)
                        Text("Adicione músicas ao aparelho e atualize a biblioteca.", color = AugMuted, fontSize = 12.sp)
                    }
                }
                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 10.dp)) {
                    items(visibleTracks, key = { it.id }) { track ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                                .combinedClickable(
                                    onClick = { playTrack(track, visibleTracks) },
                                    onLongClick = { menuTrack = track }
                                )
                                .padding(vertical = 8.dp, horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(13.dp)).background(AugPanel), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = if (currentTrack?.id == track.id) AugLime else AugMuted)
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                if (favorites.contains(track.id)) favorites.remove(track.id) else favorites.add(track.id)
                            }) {
                                Icon(if (favorites.contains(track.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    "Alternar favorito", tint = if (favorites.contains(track.id)) AugLime else AugMuted)
                            }
                        }
                    }
                }
            }

            currentTrack?.let { track ->
                Card(
                    Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 7.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AugPanel)
                ) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { shuffleEnabled = !shuffleEnabled; controller?.shuffleModeEnabled = shuffleEnabled }) {
                                Icon(Icons.Default.Shuffle, "Aleatório", tint = if (shuffleEnabled) AugLime else AugMuted)
                            }
                            IconButton(onClick = {
                                repeatMode = when (repeatMode) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                    else -> Player.REPEAT_MODE_OFF
                                }
                                controller?.repeatMode = repeatMode
                            }) {
                                Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                    "Modo de repetição", tint = if (repeatMode == Player.REPEAT_MODE_OFF) AugMuted else AugLime)
                            }
                        }
                        Slider(
                            value = (if (isSeeking) seekValue else positionMs.toFloat()).coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                            onValueChange = { isSeeking = true; seekValue = it },
                            onValueChangeFinished = {
                                controller?.seekTo(seekValue.toLong())
                                positionMs = seekValue.toLong()
                                isSeeking = false
                            },
                            valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                            colors = SliderDefaults.colors(thumbColor = AugLime, activeTrackColor = AugLime)
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(formatDuration(positionMs), color = AugMuted, fontSize = 11.sp)
                            Spacer(Modifier.weight(1f))
                            Text(formatDuration(durationMs), color = AugMuted, fontSize = 11.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { controller?.seekToPreviousMediaItem() }) {
                                Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(31.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            FilledIconButton(
                                onClick = { if (controller?.isPlaying == true) controller.pause() else controller?.play() },
                                modifier = Modifier.size(50.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = AugLime, contentColor = AugInk)
                            ) {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    if (isPlaying) "Pausar" else "Reproduzir", modifier = Modifier.size(29.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            IconButton(onClick = { controller?.seekToNextMediaItem() }) {
                                Icon(Icons.Default.SkipNext, "Próxima", tint = Color.White, modifier = Modifier.size(31.dp))
                            }
                            IconButton(onClick = { showQueue = true }) {
                                Icon(Icons.Default.QueueMusic, "Abrir fila", tint = AugMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    menuTrack?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { menuTrack = null },
            containerColor = AugPanel,
            contentColor = Color.White
        ) {
            Text(track.title, Modifier.padding(horizontal = 22.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, Modifier.padding(start = 22.dp, end = 22.dp, top = 3.dp, bottom = 8.dp), color = AugMuted, fontSize = 13.sp)
            MenuAction(Icons.Default.PlayArrow, "Reproduzir agora") {
                playTrack(track, tracks); menuTrack = null
            }
            MenuAction(Icons.Default.QueueMusic, "Adicionar à fila") {
                val player = controller
                if (player != null) {
                    val item = toMediaItem(track)
                    if (player.mediaItemCount == 0) {
                        player.setMediaItem(item); player.prepare(); player.play()
                    } else player.addMediaItem(player.currentMediaItemIndex + 1, item)
                }
                menuTrack = null
            }
            MenuAction(Icons.Default.Favorite, if (favorites.contains(track.id)) "Remover dos favoritos" else "Adicionar aos favoritos") {
                if (favorites.contains(track.id)) favorites.remove(track.id) else favorites.add(track.id)
                menuTrack = null
            }
            MenuAction(Icons.Default.Info, "Detalhes do arquivo") { detailsTrack = track; menuTrack = null }
            MenuAction(Icons.Default.Share, "Compartilhar") {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = context.contentResolver.getType(track.uri) ?: "audio/*"
                    putExtra(Intent.EXTRA_STREAM, track.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartilhar música"))
                menuTrack = null
            }
            MenuAction(Icons.Default.Edit, "Editar informações") {
                android.widget.Toast.makeText(context, "A edição de tags será adicionada na próxima etapa.", android.widget.Toast.LENGTH_SHORT).show()
                menuTrack = null
            }
            MenuAction(Icons.Default.PlayCircle, "Definir como toque") {
                android.widget.Toast.makeText(context, "Definir toque exige uma permissão especial do Android; será implementado em etapa própria.", android.widget.Toast.LENGTH_LONG).show()
                menuTrack = null
            }
            MenuAction(Icons.Default.Delete, "Excluir música") {
                android.widget.Toast.makeText(context, "A exclusão será implementada com confirmação e permissão do sistema.", android.widget.Toast.LENGTH_LONG).show()
                menuTrack = null
            }
            MenuAction(Icons.Default.PlaylistAdd, "Adicionar à playlist") {
                android.widget.Toast.makeText(context, "As playlists persistentes serão adicionadas na próxima etapa.", android.widget.Toast.LENGTH_SHORT).show()
                menuTrack = null
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }, containerColor = AugPanel, contentColor = Color.White) {
            Text("Fila de reprodução", Modifier.padding(horizontal = 22.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            val player = controller
            if (player == null || player.mediaItemCount == 0) {
                Text("A fila está vazia.", Modifier.padding(22.dp), color = AugMuted)
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                    items(player.mediaItemCount) { index ->
                        val item = player.getMediaItemAt(index)
                        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = if (index == player.currentMediaItemIndex) AugLime else AugMuted)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(item.mediaMetadata.title?.toString() ?: "Música", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.mediaMetadata.artist?.toString() ?: "Artista desconhecido", color = AugMuted, fontSize = 12.sp)
                            }
                            if (index != player.currentMediaItemIndex) IconButton(onClick = { player.seekToDefaultPosition(index); player.play() }) {
                                Icon(Icons.Default.PlayArrow, "Tocar esta faixa")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    detailsTrack?.let { track ->
        AlertDialog(
            onDismissRequest = { detailsTrack = null },
            title = { Text("Detalhes do arquivo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Título: ${track.title}")
                    Text("Artista: ${track.artist}")
                    Text("Álbum: ${track.album}")
                    Text("Formato: ${track.uri.lastPathSegment?.substringAfterLast('.', "desconhecido")?.uppercase() ?: "desconhecido"}")
                    Text("Tamanho: ${formatBytes(track.size)}")
                    Text("Duração: ${formatDuration(track.duration)}")
                    Text("Data adicionada: ${track.dateAdded}")
                    Text("URI: ${track.uri}", fontSize = 10.sp, color = AugMuted)
                }
            },
            confirmButton = { TextButton(onClick = { detailsTrack = null }) { Text("Fechar") } }
        )
    }
}

@Composable
private fun MenuAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, onClick: () -> Unit) {
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

private fun formatDuration(ms: Long): String {
    val seconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
