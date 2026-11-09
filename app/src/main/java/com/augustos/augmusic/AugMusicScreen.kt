package com.augustos.augmusic

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import java.text.Collator

// ===================================================================== helpers

private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

private fun audioPermissionName(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun hasAudioPermission(context: Context): Boolean =
    context.checkSelfPermission(audioPermissionName()) == PackageManager.PERMISSION_GRANTED

private fun Context.toast(message: String, long: Boolean = false) {
    Toast.makeText(this, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
}

/**
 * Antes a ordenação era refeita a cada recomposição (a cada 400 ms) e "Data modificada" lia o disco
 * dentro do comparador. Gênero, Número da faixa, Ano e Compositor apareciam no menu mas não faziam
 * nada (caíam no "else" e ordenavam por título). Agora todos os critérios são reais e o resultado
 * é memorizado.
 */
internal fun sortTracks(list: List<AudioTrack>, sortBy: String, descending: Boolean): List<AudioTrack> {
    val byTitle = compareBy<AudioTrack, String>(collator) { it.title }
    val comparator: Comparator<AudioTrack> = when (sortBy) {
        "Álbum" -> compareBy<AudioTrack, String>(collator) { it.album }.thenBy { it.trackNumber }.then(byTitle)
        "Artista" -> compareBy<AudioTrack, String>(collator) { it.artist }
            .then(compareBy<AudioTrack, String>(collator) { it.album })
            .thenBy { it.trackNumber }.then(byTitle)
        "Artista do álbum" -> compareBy<AudioTrack, String>(collator) { it.albumArtist.ifBlank { it.artist } }
            .then(compareBy<AudioTrack, String>(collator) { it.album })
            .thenBy { it.trackNumber }.then(byTitle)
        "Gênero" -> compareBy<AudioTrack, String>(collator) { it.genre }.then(byTitle)
        "Número da faixa" -> compareBy<AudioTrack> { it.trackNumber }.then(byTitle)
        "Duração" -> compareBy<AudioTrack> { it.duration }.then(byTitle)
        "Ano" -> compareBy<AudioTrack> { it.year }.then(byTitle)
        "Compositor" -> compareBy<AudioTrack, String>(collator) { it.composer }.then(byTitle)
        "Data modificada" -> compareBy<AudioTrack> { it.dateModified }.then(byTitle)
        "Data adicionada" -> compareBy<AudioTrack> { it.dateAdded }.then(byTitle)
        else -> byTitle
    }
    return list.sortedWith(if (descending) comparator.reversed() else comparator)
}

private data class Group(val key: String, val title: String, val subtitle: String, val rep: AudioTrack)

private enum class DeleteResult { DONE, PENDING, FAILED }

private fun requestDelete(
    context: Context,
    track: AudioTrack,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
): DeleteResult {
    return try {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                val pi = MediaStore.createDeleteRequest(context.contentResolver, listOf(track.uri))
                launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                DeleteResult.PENDING
            }
            else -> {
                try {
                    if (context.contentResolver.delete(track.uri, null, null) > 0) DeleteResult.DONE
                    else DeleteResult.FAILED
                } catch (e: SecurityException) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                        launcher.launch(IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build())
                        DeleteResult.PENDING
                    } else DeleteResult.FAILED
                }
            }
        }
    } catch (_: Exception) {
        DeleteResult.FAILED
    }
}

private fun setAsRingtone(context: Context, track: AudioTrack) {
    if (!Settings.System.canWrite(context)) {
        context.toast("Permita \"Modificar configurações do sistema\" para o WMusic e toque de novo na opção.", true)
        try {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
        return
    }
    try {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, track.uri)
        context.toast("Definida como toque")
    } catch (_: Exception) {
        context.toast("O Android recusou definir este arquivo como toque.", true)
    }
}

private fun shareTrack(context: Context, track: AudioTrack) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(track.uri) ?: "audio/*"
        putExtra(Intent.EXTRA_STREAM, track.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Compartilhar música"))
    } catch (_: ActivityNotFoundException) {
        context.toast("Nenhum app disponível para compartilhar.")
    }
}

// ===================================================================== tela

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AugMusicScreen(controller: MediaController?, vm: MainViewModel) {
    val context = LocalContext.current
    val focusManager: FocusManager = LocalFocusManager.current
    val ps = rememberPlayerState(controller)
    val tracks = vm.tracks

    // ---------------- navegação / filtros (sobrevivem a recriação da tela)
    var search by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableStateOf("Músicas") }
    var collectionType by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionKey by rememberSaveable { mutableStateOf<String?>(null) }
    var collectionLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }

    // ---------------- diálogos
    var menuTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var detailsTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var editingTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var deleteCandidate by remember { mutableStateOf<AudioTrack?>(null) }
    var pendingDelete by remember { mutableStateOf<AudioTrack?>(null) }
    var pendingPlaylistTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var playlistToDelete by remember { mutableStateOf<String?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
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
    var permissionDenied by remember { mutableStateOf(false) }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        if (granted) vm.onPermissionGranted()
    }

    LaunchedEffect(Unit) {
        if (hasPermission) vm.onPermissionGranted() else permissionLauncher.launch(audioPermissionName())
    }
    // O usuário pode conceder a permissão em Configurações e voltar ao app.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = hasAudioPermission(context)
        if (granted && !hasPermission) {
            hasPermission = true
            vm.onPermissionGranted()
        } else if (!granted && hasPermission) {
            hasPermission = false
        }
    }
    // Sem POST_NOTIFICATIONS (Android 13+) a notificação de mídia não aparece.
    LaunchedEffect(hasPermission) {
        if (hasPermission && Build.VERSION.SDK_INT >= 33 && !vm.askedNotifications &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            vm.markAskedNotifications()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---------------- erros de reprodução (o serviço já pula para a próxima faixa)
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

    // ---------------- dados derivados (memorizados: nada é recalculado a cada frame)
    val sortOptions = remember {
        buildList {
            add("Nome"); add("Álbum"); add("Artista")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { add("Artista do álbum"); add("Gênero") }
            add("Número da faixa"); add("Duração"); add("Ano"); add("Compositor")
            add("Data modificada"); add("Data adicionada")
        }
    }
    val trackById = remember(tracks) { tracks.associateBy { it.id } }
    val sortedTracks = remember(tracks, vm.sortBy, vm.sortDescending) {
        sortTracks(tracks, vm.sortBy, vm.sortDescending)
    }
    val query = remember(search) { normalizeForSearch(search.trim()) }
    val searched = remember(sortedTracks, query) {
        if (query.isEmpty()) sortedTracks else sortedTracks.filter { it.searchKey.contains(query) }
    }
    val artistCounts = remember(tracks) { tracks.groupingBy { it.artistKey }.eachCount() }
    val albumCounts = remember(tracks) { tracks.groupingBy { it.albumKey }.eachCount() }

    val curPlaylist: String? = selectedPlaylist
    val curKey: String? = collectionKey
    val inCollection = curKey != null
    val visibleTracks: List<AudioTrack> = remember(
        searched, selectedTab, curPlaylist, curKey, vm.favorites, vm.playlists, trackById, query
    ) {
        when {
            selectedTab == "Favoritos" -> searched.filter { it.id in vm.favorites }
            selectedTab == "Playlists" && curPlaylist != null ->
                // Na ordem da própria playlist (antes saía na ordem global de ordenação).
                vm.playlists[curPlaylist].orEmpty().mapNotNull { trackById[it] }
                    .filter { query.isEmpty() || it.searchKey.contains(query) }
            selectedTab == "Álbuns" && curKey != null ->
                searched.filter { it.albumKey == curKey }
                    .sortedWith(compareBy<AudioTrack> { it.trackNumber }.then(compareBy<AudioTrack, String>(collator) { it.title }))
            selectedTab == "Artistas" && curKey != null ->
                searched.filter { it.artistKey == curKey }
            else -> searched
        }
    }
    val groups: List<Group> = remember(searched, selectedTab, artistCounts, albumCounts) {
        when (selectedTab) {
            "Álbuns" -> searched.distinctBy { it.albumKey }
                .map { Group(it.albumKey, it.album, it.albumArtist.ifBlank { it.artist }, it) }
                .sortedWith(compareBy<Group, String>(collator) { it.title })
            "Artistas" -> searched.distinctBy { it.artistKey }
                .map { Group(it.artistKey, it.artist, "${artistCounts[it.artistKey] ?: 0} músicas", it) }
                .sortedWith(compareBy<Group, String>(collator) { it.title })
            else -> emptyList()
        }
    }
    val playlistNames = remember(vm.playlists) { vm.playlists.keys.sortedWith(compareBy<String, String>(collator) { it }) }
    val versionLabel = remember { appVersionLabel(context) }

    // Mantém currentTrack sincronizado com o player e com edições de metadados.
    LaunchedEffect(ps.currentMediaId, trackById) {
        ps.currentMediaId?.let { id -> trackById[id]?.let { currentTrack = it } }
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
                // Android 10: o usuário autorizou; agora é preciso repetir a exclusão.
                try {
                    if (context.contentResolver.delete(track.uri, null, null) > 0) finishDelete(track)
                    else context.toast("Não foi possível excluir.")
                } catch (_: Exception) {
                    context.toast("Não foi possível excluir.")
                }
            } else {
                finishDelete(track)
            }
        }
    }

    // ---------------- ações de reprodução
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

    fun goBack() {
        collectionType = null
        collectionKey = null
        collectionLabel = null
        selectedPlaylist = null
    }

    val inDetail = collectionLabel != null || selectedPlaylist != null
    BackHandler(enabled = inDetail) { goBack() }

    // ===================================================================== layout
    Scaffold(
        containerColor = AugInk,
        bottomBar = {
            NavigationBar(containerColor = AugPanel) {
                APP_TABS.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab; goBack() },
                        icon = {
                            Icon(
                                when (tab) {
                                    "Álbuns" -> Icons.Default.Album
                                    "Playlists" -> Icons.Default.PlaylistPlay
                                    "Artistas" -> Icons.Default.Person
                                    "Favoritos" -> Icons.Default.Favorite
                                    else -> Icons.Default.LibraryMusic
                                }, contentDescription = tab
                            )
                        },
                        label = { Text(tab, fontSize = 10.sp, maxLines = 1) },
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
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text("W", color = AugLime, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
                    Text("music", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(start = 2.dp, bottom = 5.dp))
                    Text("  $versionLabel", color = AugMuted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 7.dp))
                }
                if (vm.isLoading) {
                    CircularProgressIndicator(color = AugLime, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = {
                    if (hasPermission) vm.refresh() else permissionLauncher.launch(audioPermissionName())
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
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(17.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AugLime,
                    unfocusedBorderColor = AugPanel,
                    focusedContainerColor = AugPanel,
                    unfocusedContainerColor = AugPanel
                )
            )

            Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (inDetail) {
                    IconButton(onClick = { goBack() }) { Icon(Icons.Default.ArrowBack, "Voltar", tint = AugLime) }
                }
                Text(
                    when {
                        collectionLabel != null -> collectionLabel!!.uppercase()
                        selectedPlaylist != null -> selectedPlaylist!!.uppercase()
                        selectedTab == "Favoritos" -> "FAVORITOS"
                        selectedTab == "Álbuns" -> "ÁLBUNS"
                        selectedTab == "Artistas" -> "ARTISTAS"
                        selectedTab == "Playlists" -> "PLAYLISTS"
                        else -> "MÚSICAS"
                    },
                    color = AugMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                if (selectedTab == "Playlists" && selectedPlaylist == null) {
                    FilledIconButton(
                        onClick = { pendingPlaylistTrack = null; showCreatePlaylist = true },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = AugLime, contentColor = AugInk),
                        modifier = Modifier.size(38.dp)
                    ) { Icon(Icons.Default.Add, "Criar playlist") }
                } else if (selectedTab == "Músicas" && !inCollection) {
                    TextButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.Default.Sort, null, tint = AugLime, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("ORDENAR POR · ${vm.sortBy}", color = AugLime, fontSize = 10.sp, maxLines = 1)
                    }
                } else if (selectedTab != "Playlists" || selectedPlaylist != null) {
                    if (!((selectedTab == "Álbuns" || selectedTab == "Artistas") && !inCollection)) {
                        Text("${visibleTracks.size} faixas", color = AugMuted, fontSize = 12.sp)
                    }
                }
            }

            when {
                !hasPermission -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = { permissionLauncher.launch(audioPermissionName()) }) { Text("Permitir acesso às músicas") }
                        if (permissionDenied) {
                            // Depois de negar duas vezes o Android não mostra mais o pedido.
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                                )
                            }) { Text("Abrir configurações do app", color = AugLime) }
                        }
                    }
                }

                selectedTab == "Playlists" && selectedPlaylist == null -> {
                    if (playlistNames.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PlaylistAdd, null, tint = AugLime, modifier = Modifier.size(44.dp))
                                Text("Crie sua primeira playlist", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                                Text("Toque no + para começar", color = AugMuted, fontSize = 12.sp)
                            }
                        }
                    } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                        items(playlistNames, key = { it }) { name ->
                            val ids = vm.playlists[name].orEmpty()
                            val cover = ids.firstNotNullOfOrNull { trackById[it] }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { selectedPlaylist = name },
                                        onLongClick = { playlistToDelete = name }
                                    ).padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (cover != null) AlbumArtwork(cover, 56.dp)
                                else Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(AugPanel), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.QueueMusic, null, tint = AugLime)
                                }
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${ids.size} músicas", color = AugMuted, fontSize = 12.sp)
                                }
                                IconButton(onClick = { playlistToDelete = name }) {
                                    Icon(Icons.Default.Delete, "Excluir playlist", tint = AugMuted)
                                }
                                IconButton(onClick = { selectedPlaylist = name }) {
                                    Icon(Icons.Default.ChevronRight, "Abrir playlist", tint = AugMuted)
                                }
                            }
                        }
                    }
                }

                (selectedTab == "Álbuns" || selectedTab == "Artistas") && !inCollection -> {
                    if (groups.isEmpty() && !vm.isLoading) {
                        EmptyState(hasTracks = tracks.isNotEmpty(), modifier = Modifier.weight(1f))
                    } else LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        gridItems(groups, key = { it.key }) { group ->
                            Card(
                                Modifier.fillMaxWidth().clickable {
                                    collectionType = if (selectedTab == "Álbuns") "album" else "artist"
                                    collectionKey = group.key
                                    collectionLabel = group.title
                                },
                                colors = CardDefaults.cardColors(containerColor = AugPanel),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    if (selectedTab == "Artistas") {
                                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { ArtistArtwork(group.rep, 112.dp) }
                                    } else {
                                        AlbumArtwork(group.rep, modifier = Modifier.fillMaxWidth().aspectRatio(1f), sizePx = ArtSize.MEDIUM)
                                    }
                                    Text(group.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
                                    Text(group.subtitle, color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                !vm.hasLoadedOnce || (vm.isLoading && tracks.isEmpty()) ->
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AugLime)
                    }

                visibleTracks.isEmpty() -> EmptyState(hasTracks = tracks.isNotEmpty(), modifier = Modifier.weight(1f))

                else -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 10.dp)) {
                    items(visibleTracks, key = { it.id }) { track ->
                        val isCurrent = ps.currentMediaId == track.id
                        val favorite = track.id in vm.favorites
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp))
                                .combinedClickable(
                                    onClick = { playTrack(track, visibleTracks) },
                                    onLongClick = { menuTrack = track }
                                )
                                .padding(vertical = 8.dp, horizontal = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                AlbumArtwork(track, 48.dp)
                                if (isCurrent && ps.isPlaying) {
                                    Icon(
                                        Icons.Default.GraphicEq, "Tocando", tint = AugLime,
                                        modifier = Modifier.align(Alignment.BottomEnd).size(18.dp)
                                            .background(AugInk.copy(alpha = .85f), RoundedCornerShape(6.dp))
                                    )
                                }
                            }
                            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                // V10 mostrava o nome do ÁLBUM/ARTISTA em todas as faixas ao abrir uma coleção.
                                Text(track.title, color = if (isCurrent) AugLime else Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = AugMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { vm.toggleFavorite(track.id) }) {
                                Icon(
                                    if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    "Alternar favorito", tint = if (favorite) AugLime else AugMuted
                                )
                            }
                        }
                    }
                }
            }

            currentTrack?.let { track ->
                MiniPlayer(
                    track = track,
                    ps = ps,
                    controller = controller,
                    isFavorite = track.id in vm.favorites,
                    onOpenFull = { showFullPlayer = true },
                    onLongPress = { menuTrack = track },
                    onFavorite = { vm.toggleFavorite(track.id) },
                    onLyrics = { showLyrics = true },
                    onTimer = { showTimer = true },
                    onQueue = { showQueue = true },
                    onEqualizer = { showEqualizer = true }
                )
            }
        }
    }

    // ===================================================================== overlays

    val fullTrack = currentTrack
    if (showFullPlayer && fullTrack != null) {
        FullPlayerDialog(
            track = fullTrack,
            ps = ps,
            controller = controller,
            isFavorite = fullTrack.id in vm.favorites,
            onDismiss = { showFullPlayer = false },
            onFavorite = { vm.toggleFavorite(fullTrack.id) },
            onLyrics = { showLyrics = true },
            onTimer = { showTimer = true },
            onSpeed = { showSpeed = true },
            onQueue = { showFullPlayer = false; showQueue = true },
            onEqualizer = { showEqualizer = true },
            onMore = { menuTrack = fullTrack }
        )
    }

    if (showLyrics) {
        LyricsDialog(
            track = currentTrack,
            lyrics = lyricsText,
            loading = lyricsLoading,
            hasCustomFile = currentTrack?.let { vm.hasCustomLyrics(it.id) } == true,
            onPickFile = { lyricsPicker.launch(arrayOf("*/*")) },
            onClearFile = { currentTrack?.let { vm.setLyricsUri(it.id, null) } },
            onDismiss = { showLyrics = false }
        )
    }

    if (showSpeed) {
        SpeedDialog(
            current = ps.speed,
            onPick = { speed ->
                controller?.setPlaybackSpeed(speed)
                showSpeed = false
            },
            onDismiss = { showSpeed = false }
        )
    }

    if (showTimer) {
        TimerDialog(
            state = sleepState,
            onTimed = { minutes ->
                SleepTimer.startTimed(minutes)
                context.toast("Timer de $minutes min ativado")
                showTimer = false
            },
            onEndOfTrack = {
                SleepTimer.startEndOfTrack()
                context.toast("Vai parar no fim da música")
                showTimer = false
            },
            onCancelTimer = { SleepTimer.cancel(); showTimer = false },
            onDismiss = { showTimer = false }
        )
    }

    menuTrack?.let { track ->
        ModalBottomSheet(
            onDismissRequest = { menuTrack = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AugPanel,
            contentColor = Color.White
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(track.title, Modifier.padding(horizontal = 22.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, Modifier.padding(start = 22.dp, end = 22.dp, top = 3.dp, bottom = 8.dp), color = AugMuted, fontSize = 13.sp)
                MenuAction(Icons.Default.PlayArrow, "Reproduzir agora") {
                    playTrack(track, sortedTracks); menuTrack = null
                }
                MenuAction(Icons.Default.QueueMusic, "Adicionar à fila") {
                    controller?.let { player ->
                        val item = toMediaItem(context, track)
                        if (player.mediaItemCount == 0) {
                            player.setMediaItem(item); player.prepare(); player.play()
                        } else player.addMediaItem(player.currentMediaItemIndex + 1, item)
                    }
                    menuTrack = null
                }
                MenuAction(Icons.Default.Favorite, if (track.id in vm.favorites) "Remover dos favoritos" else "Adicionar aos favoritos") {
                    vm.toggleFavorite(track.id); menuTrack = null
                }
                MenuAction(Icons.Default.Info, "Detalhes do arquivo") { detailsTrack = track; menuTrack = null }
                MenuAction(Icons.Default.Share, "Compartilhar") { shareTrack(context, track); menuTrack = null }
                MenuAction(Icons.Default.Edit, "Editar informações") { editingTrack = track; menuTrack = null }
                MenuAction(Icons.Default.PlayCircle, "Definir como toque") { setAsRingtone(context, track); menuTrack = null }
                MenuAction(Icons.Default.PlaylistAdd, "Adicionar à playlist") {
                    pendingPlaylistTrack = track; showAddToPlaylist = true; menuTrack = null
                }
                val openPlaylist = selectedPlaylist
                if (selectedTab == "Playlists" && openPlaylist != null) {
                    MenuAction(Icons.Default.Close, "Remover desta playlist") {
                        vm.removeFromPlaylist(openPlaylist, track.id); menuTrack = null
                    }
                }
                MenuAction(Icons.Default.Delete, "Excluir música") { deleteCandidate = track; menuTrack = null }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    detailsTrack?.let { DetailsDialog(it) { detailsTrack = null } }

    editingTrack?.let { track ->
        EditTrackDialog(
            track = track,
            onSave = { title, artist, album, cover ->
                // Campo igual ao da tag do arquivo ou em branco = sem edição (volta ao original).
                vm.saveTrackEdit(track.id, title.trim().ifBlank { null }, artist.trim().ifBlank { null }, album.trim().ifBlank { null }, cover)
                // Mantém a fila do player com os metadados novos (exceto a faixa tocando agora,
                // para não interromper a reprodução).
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
                if (selectedPlaylist == name) selectedPlaylist = null
                playlistToDelete = null
            },
            onDismiss = { playlistToDelete = null }
        )
    }

    if (showCreatePlaylist) {
        CreatePlaylistDialog(
            onCreate = { rawName ->
                val created = vm.createPlaylist(rawName, pendingPlaylistTrack?.id)
                if (created != null) {
                    pendingPlaylistTrack = null
                    selectedTab = "Playlists"
                    selectedPlaylist = null
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

    if (showSortSheet) {
        SortSheet(
            options = sortOptions,
            sortBy = vm.sortBy,
            descending = vm.sortDescending,
            onChange = { by, desc -> vm.updateSort(by, desc) },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showEqualizer) EqualizerDialog(onDismiss = { showEqualizer = false })

    if (showQueue) QueueSheet(ps = ps, controller = controller, onDismiss = { showQueue = false })
}

@Composable
private fun EmptyState(hasTracks: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicOff, null, tint = AugMuted, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(10.dp))
            Text(if (hasTracks) "Nada encontrado" else "Nenhuma música encontrada", color = Color.White)
            Text("Adicione músicas ao aparelho e atualize a biblioteca.", color = AugMuted, fontSize = 12.sp)
        }
    }
}
