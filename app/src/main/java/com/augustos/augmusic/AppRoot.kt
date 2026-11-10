package com.augustos.augmusic

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController

private val NAV_ITEMS = listOf(
    Triple("home", Icons.Default.Home, "Início"),
    Triple("library", Icons.Default.LibraryMusic, "Biblioteca"),
    Triple("search", Icons.Default.Search, "Pesquisa"),
    Triple("settings", Icons.Default.Settings, "Config.")
)

@Composable
internal fun AugMusicApp(controller: MediaController?, vm: MainViewModel) {
    val context = LocalContext.current
    val c = WC
    val settings = vm.settings
    val ps = rememberPlayerState(controller)
    val nav = remember { AppNav() }

    // ---------------- diálogos / painéis
    var menuTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var detailsTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var editingTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var deleteCandidate by remember { mutableStateOf<AudioTrack?>(null) }
    var pendingDelete by remember { mutableStateOf<AudioTrack?>(null) }
    var pendingPlaylistTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var playlistToDelete by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showFull by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var currentTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var lyricsText by remember { mutableStateOf<String?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }

    val sleepState by SleepTimer.state.collectAsStateWithLifecycle()

    // ---------------- permissões
    var hasPermission by remember { mutableStateOf(hasAudioPermission(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) vm.onPermissionGranted()
        else context.toast("Sem a permissão de áudio o WMusic não consegue listar suas músicas.", true)
    }
    val requestPermission: () -> Unit = {
        // Depois de negar em definitivo o Android não mostra mais o pedido: leva às configurações.
        val activity = context as? Activity
        val runtime = context.getSharedPreferences("wmusic_runtime", 0)
        val asked = runtime.getBoolean("asked_audio", false)
        val permanentlyDenied = activity != null && !hasPermission && asked &&
            !activity.shouldShowRequestPermissionRationale(audioPermissionName())
        runtime.edit().putBoolean("asked_audio", true).apply()
        if (permanentlyDenied) openAppSettings(context) else permissionLauncher.launch(audioPermissionName())
    }
    LaunchedEffect(Unit) {
        if (hasPermission) vm.onPermissionGranted() else requestPermission()
    }
    // Permissão pode ser concedida/revogada nas configurações do sistema.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = hasAudioPermission(context)
        if (granted && !hasPermission) {
            hasPermission = true
            vm.onPermissionGranted()
        } else if (!granted && hasPermission) {
            hasPermission = false
        }
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission && Build.VERSION.SDK_INT >= 33 && !vm.askedNotifications &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            vm.markAskedNotifications()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(controller) {
        if (controller == null) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                context.toast("Não foi possível reproduzir esta faixa.")
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    // ---------------- dados derivados (memorizados)
    val sortOptions = remember {
        buildList {
            add("Nome"); add("Álbum"); add("Artista")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { add("Artista do álbum"); add("Gênero") }
            add("Número da faixa"); add("Duração"); add("Ano"); add("Compositor")
            add("Data modificada"); add("Data adicionada")
        }
    }
    val data = remember(vm.tracks, vm.sortBy, vm.sortDescending) {
        buildLibraryData(vm.tracks, vm.sortBy, vm.sortDescending)
    }
    val playlistNames = remember(vm.playlists) { vm.playlists.keys.sortedWith(compareBy<String, String>(AppCollator) { it }) }

    LaunchedEffect(ps.currentMediaId, data) {
        ps.currentMediaId?.let { id ->
            (data.byId[id] ?: vm.rawTracks.firstOrNull { it.id == id })?.let { currentTrack = it }
        }
    }

    // ---------------- letras
    LaunchedEffect(showLyrics, currentTrack?.id, vm.lyricsVersion) {
        val track = currentTrack
        if (showLyrics && track != null) {
            lyricsLoading = true
            lyricsText = vm.loadLyrics(track)
            lyricsLoading = false
        }
    }
    val lyricsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val track = currentTrack
        if (uri != null && track != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            vm.setLyricsUri(track.id, uri.toString())
        }
    }

    // ---------------- exclusão
    fun finishDelete(track: AudioTrack) {
        controller?.let { player ->
            for (i in player.mediaItemCount - 1 downTo 0) {
                if (player.getMediaItemAt(i).mediaId == track.id.toString()) player.removeMediaItem(i)
            }
        }
        vm.onTrackDeleted(track.id)
        context.toast("Música excluída")
    }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val track = pendingDelete
        pendingDelete = null
        if (track != null && result.resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                try {
                    if (context.contentResolver.delete(track.uri, null, null) > 0) finishDelete(track)
                    else context.toast("Não foi possível excluir.")
                } catch (_: Exception) {
                    context.toast("Não foi possível excluir.")
                }
            } else finishDelete(track)
        }
    }

    // ---------------- reprodução
    fun playTrack(track: AudioTrack, queue: List<AudioTrack>) {
        val player = controller
        if (player == null) {
            context.toast("O player ainda está iniciando. Tente de novo em instantes.")
            return
        }
        val safeQueue = if (queue.any { it.id == track.id }) queue else listOf(track)
        val index = safeQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        player.setMediaItems(safeQueue.map { toMediaItem(context, it) }, index, 0L)
        player.prepare()
        player.play()
        currentTrack = track
    }

    fun playList(list: List<AudioTrack>, shuffled: Boolean) {
        val player = controller
        if (player == null) {
            context.toast("O player ainda está iniciando. Tente de novo em instantes.")
            return
        }
        if (list.isEmpty()) return
        val ordered = if (shuffled) list.shuffled() else list
        player.setMediaItems(ordered.map { toMediaItem(context, it) }, 0, 0L)
        player.prepare()
        player.play()
        currentTrack = ordered.first()
    }

    fun handleGesture(action: GestureAction) {
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.PREVIOUS -> controller?.seekToPrevious()
            GestureAction.NEXT -> controller?.seekToNext()
            GestureAction.VOLUME_UP -> adjustVolume(context, true)
            GestureAction.VOLUME_DOWN -> adjustVolume(context, false)
            GestureAction.PLAY_PAUSE -> controller?.togglePlayPause()
            GestureAction.TOGGLE_FAVORITE -> currentTrack?.let { vm.toggleFavorite(it.id) }
            GestureAction.TOGGLE_SHUFFLE -> controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
            GestureAction.CYCLE_REPEAT -> controller?.cycleRepeatMode()
            GestureAction.OPEN_QUEUE -> showQueue = true
            GestureAction.OPEN_MENU -> menuTrack = currentTrack
        }
    }

    val actions = remember(controller) {
        AppActions(
            play = { t, q -> playTrack(t, q) },
            playList = { l, s -> playList(l, s) },
            menu = { menuTrack = it },
            openFull = { showFull = true }
        )
    }

    BackHandler(enabled = nav.canGoBack && !showFull) { nav.back() }

    // ===================================================================== layout
    Box(
        Modifier.fillMaxSize().background(
            if (c.useGradients) Brush.verticalGradient(listOf(c.gradTop, c.bg, c.bg)) else Brush.verticalGradient(listOf(c.bg, c.bg))
        )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    val mini = currentTrack
                    if (settings.miniEnabled && mini != null) {
                        MiniPlayer(
                            track = mini, ps = ps, controller = controller, settings = settings,
                            onOpenFull = { showFull = true }, onMenu = { menuTrack = mini }
                        )
                    }
                    NavigationBar(containerColor = c.surface) {
                        NAV_ITEMS.forEach { (dest, icon, label) ->
                            NavigationBarItem(
                                selected = nav.destination == dest,
                                onClick = { nav.select(dest) },
                                icon = { androidx.compose.material3.Icon(icon, contentDescription = label) },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                Crossfade(targetState = nav.destination, animationSpec = tween(220), label = "destination") { dest ->
                    when (dest) {
                        "library" -> LibraryScreen(
                            data = data, vm = vm, nav = nav, ps = ps, actions = actions,
                            hasPermission = hasPermission, onRequestPermission = requestPermission,
                            onShowSort = { showSort = true },
                            onNewPlaylist = { pendingPlaylistTrack = null; showCreatePlaylist = true },
                            onDeletePlaylist = { playlistToDelete = it }
                        )
                        "search" -> SearchScreen(data, vm, nav, ps, actions)
                        "settings" -> SettingsScreen(
                            vm = vm, nav = nav, hasPermission = hasPermission,
                            hasCurrentTrack = currentTrack != null,
                            onOpenPlayer = { showFull = true },
                            onShowInLibrary = { nav.showTrackInLibrary(it) }
                        )
                        else -> HomeScreen(
                            data = data, vm = vm, nav = nav, ps = ps, current = currentTrack, actions = actions,
                            hasPermission = hasPermission, onRequestPermission = requestPermission
                        )
                    }
                }
            }
        }

        // Player Full: sobreposição com transição suave (não é Dialog, respeita áreas seguras e o botão voltar).
        val full = currentTrack
        AnimatedVisibility(
            visible = showFull && full != null,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200))
        ) {
            if (full != null) {
                FullPlayer(
                    track = full, ps = ps, controller = controller, settings = settings,
                    isFavorite = full.id in vm.favorites,
                    sleepActive = sleepState.mode != SleepMode.OFF,
                    onClose = { showFull = false },
                    onFavorite = { vm.toggleFavorite(full.id) },
                    onLyrics = { showLyrics = true },
                    onTimer = { showTimer = true },
                    onSpeed = { showSpeed = true },
                    onQueue = { showQueue = true },
                    onEqualizer = { showEqualizer = true },
                    onMenuAction = { a ->
                        when (a) {
                            PlayerMenuAction.DETAILS -> detailsTrack = full
                            PlayerMenuAction.EDIT -> editingTrack = full
                            PlayerMenuAction.SHARE -> shareTrack(context, full)
                            PlayerMenuAction.PLAYLIST -> { pendingPlaylistTrack = full; showAddToPlaylist = true }
                            PlayerMenuAction.RINGTONE -> setAsRingtone(context, full)
                            PlayerMenuAction.LOCATE -> openFileLocation(context, full)
                            PlayerMenuAction.LYRICS -> showLyrics = true
                            PlayerMenuAction.SETTINGS -> { showFull = false; nav.openSettings("player") }
                        }
                    },
                    onGesture = { handleGesture(it) }
                )
            }
        }
    }

    // ===================================================================== painéis e diálogos

    menuTrack?.let { track ->
        val openPlaylist = nav.playlist
        TrackMenuSheet(
            track = track,
            isFavorite = track.id in vm.favorites,
            actions = TrackMenuActions(
                onPlay = { playTrack(track, data.sorted.ifEmpty { listOf(track) }); menuTrack = null },
                onQueue = {
                    controller?.let { player ->
                        val item = toMediaItem(context, track)
                        if (player.mediaItemCount == 0) {
                            player.setMediaItem(item); player.prepare(); player.play(); currentTrack = track
                        } else player.addMediaItem(player.currentMediaItemIndex + 1, item)
                        context.toast("Adicionada à fila")
                    }
                    menuTrack = null
                },
                onFavorite = { vm.toggleFavorite(track.id); menuTrack = null },
                onDetails = { detailsTrack = track; menuTrack = null },
                onShare = { shareTrack(context, track); menuTrack = null },
                onEdit = { editingTrack = track; menuTrack = null },
                onRingtone = { setAsRingtone(context, track); menuTrack = null },
                onPlaylist = { pendingPlaylistTrack = track; showAddToPlaylist = true; menuTrack = null },
                onLocate = { openFileLocation(context, track); menuTrack = null },
                onRemoveFromPlaylist = if (nav.libraryTab == "Playlists" && openPlaylist != null) {
                    { vm.removeFromPlaylist(openPlaylist, track.id); menuTrack = null }
                } else null,
                onDelete = { deleteCandidate = track; menuTrack = null }
            ),
            onDismiss = { menuTrack = null }
        )
    }

    detailsTrack?.let { DetailsDialog(it) { detailsTrack = null } }

    editingTrack?.let { track ->
        EditTrackDialog(
            track = track,
            onSave = { title, artist, album, cover ->
                vm.saveTrackEdit(track.id, title.trim().ifBlank { null }, artist.trim().ifBlank { null }, album.trim().ifBlank { null }, cover)
                controller?.let { player ->
                    val updated = track.copy(
                        title = title.trim().ifBlank { track.title },
                        artist = artist.trim().ifBlank { track.artist },
                        album = album.trim().ifBlank { track.album }
                    )
                    for (i in 0 until player.mediaItemCount) {
                        if (i != player.currentMediaItemIndex && player.getMediaItemAt(i).mediaId == track.id.toString()) {
                            player.replaceMediaItem(i, toMediaItem(context, updated))
                        }
                    }
                }
                editingTrack = null
            },
            onDismiss = { editingTrack = null }
        )
    }

    deleteCandidate?.let { track ->
        ConfirmDialog(
            title = "Excluir música?",
            message = "\"${track.title}\" será apagada do aparelho. O Android vai pedir uma confirmação final.",
            confirmLabel = "Excluir",
            onConfirm = {
                deleteCandidate = null
                pendingDelete = track
                when (requestDelete(context, track, deleteLauncher)) {
                    DeleteResult.DONE -> { pendingDelete = null; finishDelete(track) }
                    DeleteResult.PENDING -> Unit
                    DeleteResult.FAILED -> {
                        pendingDelete = null
                        context.toast("Não foi possível excluir. Verifique as permissões do app.", true)
                    }
                }
            },
            onDismiss = { deleteCandidate = null }
        )
    }

    playlistToDelete?.let { name ->
        ConfirmDialog(
            title = "Excluir playlist?",
            message = "A playlist \"$name\" será removida. As músicas continuam no aparelho.",
            confirmLabel = "Excluir",
            onConfirm = {
                vm.deletePlaylist(name)
                if (nav.playlist == name) nav.clearDetail()
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onCreate = { raw ->
                val created = vm.createPlaylist(raw, pendingPlaylistTrack?.id)
                if (created != null) {
                    if (pendingPlaylistTrack != null) context.toast("Adicionada à playlist $created")
                    pendingPlaylistTrack = null
                }
                showCreatePlaylist = false
            },
            onDismiss = { showCreatePlaylist = false; pendingPlaylistTrack = null }
        )
    }

    if (showAddToPlaylist) {
        AddToPlaylistDialog(
            names = playlistNames,
            onPick = { name ->
                pendingPlaylistTrack?.let { vm.addToPlaylist(name, it.id) }
                showAddToPlaylist = false
                pendingPlaylistTrack = null
                context.toast("Adicionada à playlist $name")
            },
            onCreateNew = { showAddToPlaylist = false; showCreatePlaylist = true },
            onDismiss = { showAddToPlaylist = false; pendingPlaylistTrack = null }
        )
    }

    if (showSort) {
        SortSheet(
            options = sortOptions, sortBy = vm.sortBy, descending = vm.sortDescending,
            onChange = { by, desc -> vm.updateSort(by, desc) },
            onDismiss = { showSort = false }
        )
    }

    if (showLyrics) {
        currentTrack?.let { track ->
            LyricsSheet(
                track = track,
                settings = settings,
                lyrics = lyricsText,
                loading = lyricsLoading,
                hasSaved = remember(track.id, vm.lyricsVersion, lyricsText) { vm.hasSavedLyrics(track.id) },
                hasPickedFile = vm.hasCustomLyricsFile(track.id),
                onSave = { plain, synced -> vm.saveLyrics(track, plain, synced) },
                onDelete = { vm.deleteLyrics(track) },
                onPickFile = { lyricsPicker.launch(arrayOf("*/*")) },
                onClearPickedFile = { vm.setLyricsUri(track.id, null) },
                onEnable = { vm.updateSettings { it.copy(lyricsEnabled = true) } },
                onDismiss = { showLyrics = false }
            )
        }
    }

    if (showSpeed) {
        SpeedDialog(
            current = ps.speed,
            onPick = { speed -> controller?.setPlaybackSpeed(speed); showSpeed = false },
            onDismiss = { showSpeed = false }
        )
    }

    if (showTimer) {
        TimerDialog(
            state = sleepState,
            onTimed = { minutes -> SleepTimer.startTimed(minutes); context.toast("Timer de $minutes min ativado"); showTimer = false },
            onEndOfTrack = { SleepTimer.startEndOfTrack(); context.toast("Vai parar no fim da música"); showTimer = false },
            onCancelTimer = { SleepTimer.cancel(); showTimer = false },
            onDismiss = { showTimer = false }
        )
    }

    if (showEqualizer) EqualizerDialog(onDismiss = { showEqualizer = false })
    if (showQueue) QueueSheet(ps = ps, controller = controller, onDismiss = { showQueue = false })
}
