package com.augustos.augmusic

import android.app.Application
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Dono do estado do app. A leitura do MediaStore acontece em Dispatchers.IO.
 * rawTracks = tudo que o MediaStore devolveu; tracks = rawTracks já filtrado por Whitelist/Blacklist.
 * Trocar o modo/pastas só refaz o filtro em memória (sem nova varredura).
 */
internal class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = UserData(application)

    var settings by mutableStateOf(store.loadSettings())
        private set
    var rawTracks by mutableStateOf<List<AudioTrack>>(emptyList())
        private set
    var tracks by mutableStateOf<List<AudioTrack>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var hasLoadedOnce by mutableStateOf(false)
        private set
    var favorites by mutableStateOf(store.loadFavorites())
        private set
    var playlists by mutableStateOf(store.loadPlaylists())
        private set
    var sortBy by mutableStateOf(store.sortBy)
        private set
    var sortDescending by mutableStateOf(store.sortDescending)
        private set
    var lyricsVersion by mutableIntStateOf(0)
        private set

    private var loadJob: Job? = null
    private var debounceJob: Job? = null
    private var observer: ContentObserver? = null

    val askedNotifications: Boolean get() = store.askedNotifications
    fun markAskedNotifications() { store.askedNotifications = true }

    private val app: Application get() = getApplication<Application>()

    // ---------------------------------------------------------------- configurações
    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val old = settings
        val next = transform(old)
        if (next == old) return
        settings = next
        store.saveSettings(next)
        val filterChanged = next.libraryMode != old.libraryMode ||
            next.whitelist != old.whitelist || next.blacklist != old.blacklist
        if (filterChanged) applyFilter()
    }

    private fun applyFilter() {
        tracks = LibraryFilter.apply(rawTracks, settings)
    }

    // ---------------------------------------------------------------- biblioteca
    fun onPermissionGranted() {
        startObserving()
        if (!hasLoadedOnce && !isLoading) refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            isLoading = true
            try {
                ArtworkLoader.invalidateNegativeCaches()
                val result = withContext(Dispatchers.IO) {
                    runCatching { LibraryRepository.loadTracks(app, store) }
                }
                // Em caso de erro (ex.: permissão revogada) mantém a lista atual em vez de zerá-la.
                result.onSuccess {
                    rawTracks = it
                    applyFilter()
                }
                hasLoadedOnce = true
            } finally {
                isLoading = false
            }
        }
    }

    /** Atualiza a lista sozinho quando músicas são adicionadas/removidas no aparelho. */
    private fun startObserving() {
        if (observer != null) return
        val obs = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { scheduleRefresh() }
        }
        observer = obs
        runCatching {
            app.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, obs)
        }
    }

    private fun scheduleRefresh() {
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(1500)
            refresh()
        }
    }

    override fun onCleared() {
        observer?.let { runCatching { app.contentResolver.unregisterContentObserver(it) } }
        observer = null
        super.onCleared()
    }

    /** Verifica, uma a uma, quais faixas do MediaStore já não podem ser abertas. */
    suspend fun findUnavailable(): List<AudioTrack> = withContext(Dispatchers.IO) {
        rawTracks.filter { t ->
            try {
                app.contentResolver.openFileDescriptor(t.uri, "r")?.use { false } ?: true
            } catch (_: Exception) {
                true
            }
        }
    }

    suspend fun isAvailable(t: AudioTrack): Boolean = withContext(Dispatchers.IO) {
        try {
            app.contentResolver.openFileDescriptor(t.uri, "r")?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
    }

    // ---------------------------------------------------------------- pastas
    /** Retorna false se o URI não corresponde a uma pasta real. */
    fun addFolder(mode: LibraryMode, treeUri: Uri): Boolean {
        val path = LibraryFilter.treeUriToPath(treeUri) ?: return false
        updateSettings { s ->
            when (mode) {
                LibraryMode.WHITELIST -> s.copy(whitelist = (s.whitelist + path).distinct())
                LibraryMode.BLACKLIST -> s.copy(blacklist = (s.blacklist + path).distinct())
            }
        }
        return true
    }

    fun removeFolder(mode: LibraryMode, path: String) {
        updateSettings { s ->
            when (mode) {
                LibraryMode.WHITELIST -> s.copy(whitelist = s.whitelist - path)
                LibraryMode.BLACKLIST -> s.copy(blacklist = s.blacklist - path)
            }
        }
    }

    // ---------------------------------------------------------------- ordenação
    fun updateSort(by: String, descending: Boolean) {
        sortBy = by
        sortDescending = descending
        store.sortBy = by
        store.sortDescending = descending
    }

    // ---------------------------------------------------------------- favoritos
    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        store.saveFavorites(favorites)
    }

    // ---------------------------------------------------------------- playlists
    private fun findPlaylistKey(name: String): String? =
        playlists.keys.firstOrNull { it.equals(name.trim(), ignoreCase = true) }

    fun createPlaylist(rawName: String, firstTrackId: Long? = null): String? {
        val name = rawName.trim().take(60)
        if (name.isEmpty()) return null
        val key = findPlaylistKey(name) ?: name
        val current = playlists[key].orEmpty()
        val next = if (firstTrackId != null && firstTrackId !in current) current + firstTrackId else current
        playlists = playlists + (key to next)
        store.savePlaylists(playlists)
        return key
    }

    fun addToPlaylist(name: String, id: Long) {
        val current = playlists[name] ?: return
        if (id in current) return
        playlists = playlists + (name to (current + id))
        store.savePlaylists(playlists)
    }

    fun removeFromPlaylist(name: String, id: Long) {
        val current = playlists[name] ?: return
        playlists = playlists + (name to (current - id))
        store.savePlaylists(playlists)
    }

    fun deletePlaylist(name: String) {
        playlists = playlists - name
        store.savePlaylists(playlists)
    }

    // ---------------------------------------------------------------- edição / exclusão
    fun saveTrackEdit(id: Long, title: String?, artist: String?, album: String?, cover: String?) {
        store.saveTrackEdit(id, title, artist, album, cover)
        refresh()
    }

    fun onTrackDeleted(id: Long) {
        rawTracks = rawTracks.filterNot { it.id == id }
        applyFilter()
        favorites = favorites - id
        store.saveFavorites(favorites)
        playlists = playlists.mapValues { (_, ids) -> ids - id }
        store.savePlaylists(playlists)
        store.clearTrackData(id)
        LyricsStorage.delete(app, id)
    }

    // ---------------------------------------------------------------- letras
    fun setLyricsUri(id: Long, uri: String?) {
        store.setLyricsUri(id, uri)
        lyricsVersion++
    }

    fun hasCustomLyricsFile(id: Long): Boolean = store.lyricsUri(id) != null
    fun hasSavedLyrics(id: Long): Boolean = LyricsStorage.has(app, id)

    suspend fun loadLyrics(track: AudioTrack): String? = LyricsLoader.load(app, store, track)

    suspend fun saveLyrics(track: AudioTrack, plain: String, synced: String? = null) {
        withContext(Dispatchers.IO) { LyricsStorage.save(app, track.id, plain.trim(), synced) }
        lyricsVersion++
    }

    suspend fun deleteLyrics(track: AudioTrack) {
        withContext(Dispatchers.IO) { LyricsStorage.delete(app, track.id) }
        store.setLyricsUri(track.id, null)
        lyricsVersion++
    }

    suspend fun savedLyricsCount(): Int = withContext(Dispatchers.IO) { LyricsStorage.count(app) }

    suspend fun clearAllLyrics() {
        withContext(Dispatchers.IO) { LyricsStorage.clearAll(app) }
        lyricsVersion++
    }

    // ---------------------------------------------------------------- backup
    private fun versionName(): String = try {
        @Suppress("DEPRECATION")
        app.packageManager.getPackageInfo(app.packageName, 0).versionName.orEmpty()
    } catch (_: Exception) {
        "13.0.0"
    }

    suspend fun exportBackup(uri: Uri, sections: BackupSections): Result<Unit> {
        val json = BackupManager.build(
            versionName(), sections, settings, AudioEffects.settings.value, sortBy, sortDescending,
            playlists, favorites, rawTracks.associateBy { it.id }
        )
        return BackupManager.write(app, uri, json)
    }

    suspend fun readBackup(uri: Uri): Result<ParsedBackup> = BackupManager.read(app, uri)

    /** Substitui os dados atuais pelos do backup (a confirmação é feita pela UI). */
    fun restoreBackup(backup: ParsedBackup, sections: BackupSections): RestoreReport {
        val warnings = ArrayList<String>()
        var notFound = 0
        var playlistsRestored = 0
        var favoritesRestored = 0
        val matcher = BackupManager.Matcher(rawTracks)

        if (sections.settings) {
            backup.settings?.let { s ->
                warnings += BackupManager.folderWarnings(s)
                // Substitui direto (inclui pastas); o filtro é refeito em memória.
                settings = s
                store.saveSettings(s)
                applyFilter()
            }
            backup.equalizer?.let { eq -> AudioEffects.update { eq } }
            backup.sortBy?.let { by -> updateSort(by, backup.sortDescending ?: false) }
        }
        if ((sections.playlists || sections.favorites) && rawTracks.isEmpty()) {
            warnings += "A biblioteca ainda não foi carregada (permissão de áudio?). Playlists e favoritos não puderam ser vinculados às músicas."
        } else {
            if (sections.playlists) backup.playlists?.let { list ->
                val restored = LinkedHashMap<String, List<Long>>()
                for ((name, refs) in list) {
                    val ids = refs.mapNotNull { r -> matcher.resolve(r).also { if (it == null) notFound++ } }.distinct()
                    restored[name] = ids
                }
                playlists = restored
                store.savePlaylists(restored)
                playlistsRestored = restored.size
            }
            if (sections.favorites) backup.favorites?.let { refs ->
                val ids = refs.mapNotNull { r -> matcher.resolve(r).also { if (it == null) notFound++ } }.toSet()
                favorites = ids
                store.saveFavorites(ids)
                favoritesRestored = ids.size
            }
        }
        if (notFound > 0) warnings += "$notFound música(s) do backup não existem neste aparelho e foram ignoradas."
        return RestoreReport(warnings, playlistsRestored, favoritesRestored, notFound)
    }
}
