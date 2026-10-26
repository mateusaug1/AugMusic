package com.augustos.augmusic

import android.Manifest
import android.content.ComponentName
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.File
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import com.google.common.util.concurrent.ListenableFuture

private val AugInk = Color(0xFF10120F)
private val AugPanel = Color(0xFF1A1E19)
private val AugLime = Color(0xFFB7F36B)
private val AugMuted = Color(0xFF9BA397)

private data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val path: String?,
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
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
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
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
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
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val pathCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                result += AudioTrack(
                    id,
                    cursor.getString(titleCol)?.takeIf(String::isNotBlank) ?: "Música sem título",
                    cursor.getString(artistCol)?.trim()?.takeIf {
                        it.isNotBlank() && !it.equals("<unknown>", true) && !it.equals("unknown", true)
                    } ?: "Artista desconhecido",
                    cursor.getString(albumCol)?.takeIf(String::isNotBlank) ?: "Álbum desconhecido",
                    cursor.getLong(albumIdCol),
                    if (pathCol >= 0) cursor.getString(pathCol) else null,
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
    var showFullPlayer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var currentTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var shuffleEnabled by remember { mutableStateOf(false) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }
    val favorites = remember {
        mutableStateListOf<Long>().apply {
            val saved = context.getSharedPreferences("wmusic_preferences", android.content.Context.MODE_PRIVATE)
                .getStringSet("favorites", emptySet()) ?: emptySet()
            addAll(saved.mapNotNull { it.toLongOrNull() })
        }
    }
    LaunchedEffect(favorites.size) {
        context.getSharedPreferences("wmusic_preferences", android.content.Context.MODE_PRIVATE)
            .edit().putStringSet("favorites", favorites.map { it.toString() }.toSet()).apply()
    }

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

    val searchedTracks = tracks.filter { track ->
        "${track.title} ${track.artist} ${track.album}".contains(search, true)
    }
    val visibleTracks = when (selectedTab) {
        "Favoritos" -> searchedTracks.filter { favorites.contains(it.id) }
        "Álbuns" -> searchedTracks.distinctBy { it.albumId }
        "Artistas" -> searchedTracks.distinctBy { it.artist.lowercase() }
        else -> searchedTracks
    }

    fun toMediaItem(track: AudioTrack): MediaItem = MediaItem.Builder()
        .setMediaId(track.id.toString())
        .setUri(track.uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .setAlbumTitle(track.album)
                .setArtworkUri(albumArtworkUri(track))
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
                listOf("Músicas", "Álbuns", "Artistas", "Favoritos", "Fila").forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab || (tab == "Fila" && showQueue),
                        onClick = { if (tab == "Fila") showQueue = true else selectedTab = tab },
                        icon = {
                            Icon(
                                when (tab) {
                                    "Álbuns" -> Icons.Default.Album
                                    "Artistas" -> Icons.Default.Person
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
                    Text("W", color = AugLime, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 0.sp)
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
                Text(when (selectedTab) {
                    "Favoritos" -> "FAVORITOS"
                    "Álbuns" -> "ÁLBUNS"
                    "Artistas" -> "ARTISTAS"
                    else -> "SUA BIBLIOTECA"
                },
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
                                    onClick = {
                                        val queue = when (selectedTab) {
                                            "Álbuns" -> tracks.filter { it.albumId == track.albumId }
                                            "Artistas" -> tracks.filter { it.artist.equals(track.artist, true) }
                                            else -> visibleTracks
                                        }
                                        playTrack(track, queue)
                                    },
                                    onLongClick = { menuTrack = track }
                                )
                                .padding(vertical = 8.dp, horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AlbumArtwork(track, 48.dp)
                                if (currentTrack?.id == track.id && isPlaying) {
                                    Icon(
                                        Icons.Default.GraphicEq, "Tocando",
                                        tint = AugLime,
                                        modifier = Modifier.align(Alignment.BottomEnd).size(18.dp)
                                            .background(AugInk.copy(alpha = .85f), RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                Text(
                                    when (selectedTab) {
                                        "Álbuns" -> track.album
                                        "Artistas" -> track.artist
                                        else -> track.title
                                    },
                                    color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    when (selectedTab) {
                                        "Álbuns" -> track.artist
                                        "Artistas" -> "${tracks.count { it.artist.equals(track.artist, true) }} músicas"
                                        else -> track.artist
                                    },
                                    color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
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
                    Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 7.dp)
                        .combinedClickable(onClick = { showFullPlayer = true }, onLongClick = { menuTrack = track })
                        .pointerInput(track.id) {
                            var totalDrag = 0f
                            detectVerticalDragGestures(
                                onDragEnd = { if (totalDrag < -35f) showFullPlayer = true; totalDrag = 0f },
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showLyrics = true }) { Icon(Icons.Default.ChatBubbleOutline, "Letra", tint = AugMuted) }
                            IconButton(onClick = { if (favorites.contains(track.id)) favorites.remove(track.id) else favorites.add(track.id) }) {
                                Icon(if (favorites.contains(track.id)) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    "Favorito", tint = if (favorites.contains(track.id)) AugLime else AugMuted)
                            }
                            IconButton(onClick = { showTimer = true }) { Icon(Icons.Default.Timer, "Timer", tint = AugMuted) }
                            IconButton(onClick = { showQueue = true }) { Icon(Icons.Default.QueueMusic, "Fila", tint = AugMuted) }
                        }
                    }
                }
            }
        }
    }

    if (showFullPlayer && currentTrack != null) {
        FullPlayerDialog(
            track = currentTrack!!,
            controller = controller,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            isFavorite = favorites.contains(currentTrack!!.id),
            playbackSpeed = playbackSpeed,
            onDismiss = { showFullPlayer = false },
            onPlayPause = { if (controller?.isPlaying == true) controller.pause() else controller?.play() },
            onPrevious = { controller?.seekToPreviousMediaItem() },
            onNext = { controller?.seekToNextMediaItem() },
            onSeek = { controller?.seekTo(it); positionMs = it },
            onShuffle = { shuffleEnabled = !shuffleEnabled; controller?.shuffleModeEnabled = shuffleEnabled },
            onRepeat = {
                repeatMode = when (repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                controller?.repeatMode = repeatMode
            },
            onFavorite = { if (favorites.contains(currentTrack!!.id)) favorites.remove(currentTrack!!.id) else favorites.add(currentTrack!!.id) },
            onLyrics = { showLyrics = true },
            onTimer = { showTimer = true },
            onSpeed = { showSpeed = true },
            onQueue = { showFullPlayer = false; showQueue = true },
            onMore = { menuTrack = currentTrack }
        )
    }

    LaunchedEffect(showLyrics, currentTrack?.id) {
        if (showLyrics) {
            val track = currentTrack
            lyricsText = withContext(Dispatchers.IO) {
                try {
                    val parent = track?.path?.let { File(it).parentFile }
                    val base = track?.path?.let { File(it).nameWithoutExtension }
                    val candidates = listOfNotNull(
                        if (parent != null && base != null) File(parent, "$base.lrc") else null,
                        if (parent != null && base != null) File(parent, "$base.txt") else null,
                        if (parent != null) File(parent, "lyrics.lrc") else null,
                        if (parent != null) File(parent, "lyrics.txt") else null
                    )
                    candidates.firstOrNull { it.isFile }?.readText()?.take(30000)
                } catch (_: Exception) { null }
            }
        }
    }

    if (showLyrics) {
        AlertDialog(
            onDismissRequest = { showLyrics = false },
            title = { Text("Letra da música") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Text(currentTrack?.title ?: "Música", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        lyricsText ?: "Nenhum arquivo de letra encontrado. Coloque um arquivo .lrc ou .txt com o mesmo nome da música na mesma pasta do áudio.",
                        color = if (lyricsText == null) AugMuted else Color.White
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showLyrics = false }) { Text("Fechar") } }
        )
    }

    if (showSpeed) {
        AlertDialog(
            onDismissRequest = { showSpeed = false },
            title = { Text("Velocidade de reprodução") },
            text = {
                Column {
                    listOf(0.5f, 0.8f, 1f, 1.2f, 1.5f, 2f).forEach { speed ->
                        TextButton(
                            onClick = {
                                playbackSpeed = speed
                                controller?.playbackParameters = PlaybackParameters(speed, 1f)
                                showSpeed = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "${speed}x",
                                color = if (playbackSpeed == speed) AugLime else Color.White
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSpeed = false }) { Text("Fechar") } }
        )
    }

    if (showTimer) {
        AlertDialog(
            onDismissRequest = { showTimer = false },
            title = { Text("Timer de desligamento") },
            text = { Text("Pause a reprodução automaticamente após:") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(15, 30, 45, 60).forEach { minutes ->
                        TextButton(onClick = {
                            timerJob?.cancel()
                            timerJob = coroutineScope.launch {
                                delay(minutes * 60_000L)
                                controller?.pause()
                                android.widget.Toast.makeText(context, "Timer finalizado", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            showTimer = false
                            android.widget.Toast.makeText(context, "Timer de $minutes min ativado", android.widget.Toast.LENGTH_SHORT).show()
                        }) { Text(if (minutes == 60) "1h" else "${minutes}m") }
                    }
                    TextButton(onClick = {
                        timerJob?.cancel()
                        val remaining = (controller?.duration ?: 0L) - (controller?.currentPosition ?: 0L)
                        if (remaining > 0L) {
                            timerJob = coroutineScope.launch {
                                delay(remaining)
                                controller?.pause()
                                android.widget.Toast.makeText(context, "Fim da música", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            android.widget.Toast.makeText(context, "Vai parar no fim da música", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Não foi possível calcular o tempo restante", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        showTimer = false
                    }) { Text("Fim da música") }
                }
            },
            dismissButton = { TextButton(onClick = { timerJob?.cancel(); timerJob = null; showTimer = false }) { Text("Cancelar") } }
        )
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


private fun coverFile(track: AudioTrack): File? {
    val parent = track.path?.let { File(it).parentFile }
    return parent?.let { File(it, "cover.jpg") }?.takeIf { it.isFile }
}

private fun albumArtworkUri(track: AudioTrack): Uri =
    Uri.parse("content://media/external/audio/albumart/${track.albumId}")

private fun artworkModel(track: AudioTrack): Any =
    coverFile(track) ?: albumArtworkUri(track)

@Composable
private fun AlbumArtwork(track: AudioTrack, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    var useAlbumFallback by remember(track.id) { mutableStateOf(false) }
    val imageModel = if (!useAlbumFallback) artworkModel(track) else albumArtworkUri(track)
    Box(
        modifier.size(size).clip(RoundedCornerShape(16.dp)).background(AugPanel),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.MusicNote, null, tint = AugMuted.copy(alpha = .8f), modifier = Modifier.size(size * .48f))
        AsyncImage(
            model = imageModel,
            contentDescription = "Capa de ${track.title}",
            contentScale = ContentScale.Crop,
            onError = { if (!useAlbumFallback) useAlbumFallback = true },
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullPlayerDialog(
    track: AudioTrack,
    controller: MediaController?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    playbackSpeed: Float,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onLyrics: () -> Unit,
    onTimer: () -> Unit,
    onSpeed: () -> Unit,
    onQueue: () -> Unit,
    onMore: () -> Unit
) {
    val context = LocalContext.current
    var dominant by remember(track.id) { mutableStateOf(Color(0xFF202820)) }
    var dragTotal by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(positionMs.toFloat()) }

    LaunchedEffect(track.id) {
        dominant = withContext(Dispatchers.IO) {
            try {
                val bitmap = coverFile(track)?.let { BitmapFactory.decodeFile(it.absolutePath) }
                    ?: context.contentResolver.openInputStream(albumArtworkUri(track))?.use { BitmapFactory.decodeStream(it) }
                bitmap?.let { bmp ->
                    val color = Palette.from(bmp).generate().getVibrantColor(0xFF26362D.toInt())
                    bmp.recycle()
                    Color(color)
                } ?: Color(0xFF202820)
            } catch (_: Exception) { Color(0xFF202820) }
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
            AsyncImage(
                model = artworkModel(track),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(34.dp)
            )
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
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.KeyboardArrowDown, "Fechar player", tint = Color.White, modifier = Modifier.size(30.dp)) }
                    Spacer(Modifier.weight(1f))
                    Text("TOCANDO AGORA", color = AugMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onMore) { Icon(Icons.Default.MoreVert, "Mais opções", tint = Color.White) }
                }
                Spacer(Modifier.height(12.dp))
                AlbumArtwork(track, 250.dp, Modifier.fillMaxWidth(.72f))
                Spacer(Modifier.height(25.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = Color.White.copy(alpha = .72f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito",
                            tint = if (isFavorite) AugLime else Color.White, modifier = Modifier.size(25.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Slider(
                    value = (if (isSeeking) seekValue else positionMs.toFloat()).coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                    onValueChange = { isSeeking = true; seekValue = it },
                    onValueChangeFinished = { onSeek(seekValue.toLong()); isSeeking = false },
                    valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                    colors = SliderDefaults.colors(thumbColor = AugLime, activeTrackColor = AugLime, inactiveTrackColor = Color.White.copy(alpha = .22f))
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(formatDuration(if (isSeeking) seekValue.toLong() else positionMs), color = AugMuted, fontSize = 11.sp)
                    Spacer(Modifier.weight(1f))
                    Text(formatDuration(durationMs), color = AugMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Aleatório", tint = if (shuffleEnabled) AugLime else AugMuted) }
                    IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Anterior", tint = Color.White, modifier = Modifier.size(38.dp)) }
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(66.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = AugLime, contentColor = AugInk)
                    ) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pausar" else "Reproduzir", modifier = Modifier.size(38.dp)) }
                    IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Próxima", tint = Color.White, modifier = Modifier.size(38.dp)) }
                    IconButton(onClick = onRepeat) { Icon(if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repetir", tint = if (repeatMode == Player.REPEAT_MODE_OFF) AugMuted else AugLime) }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onLyrics) { Icon(Icons.Default.ChatBubbleOutline, "Letra", tint = Color.White) }
                    IconButton(onClick = onFavorite) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (isFavorite) AugLime else Color.White) }
                    IconButton(onClick = onTimer) { Icon(Icons.Default.Timer, "Timer", tint = Color.White) }
                    TextButton(onClick = onSpeed) { Text("${playbackSpeed}×", color = AugLime, fontWeight = FontWeight.Bold) }
                    IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Fila", tint = Color.White) }
                }
                val format = track.path?.substringAfterLast('.', "")?.uppercase()?.takeIf { it.isNotBlank() } ?: "ÁUDIO"
                val kbps = if (track.duration > 0L && track.size > 0L) (track.size * 8L / track.duration).coerceAtLeast(1L) else 0L
                Text(
                    if (kbps > 0) "$format · ~${kbps} kb/s · arquivo local" else "$format · arquivo local",
                    color = AugMuted, fontSize = 10.sp, letterSpacing = .5.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            }
        }
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
