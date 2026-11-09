package com.augustos.augmusic

import android.app.Application
import android.database.ContentObserver
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

/**
 * Dono do estado da biblioteca. A leitura do MediaStore acontece em Dispatchers.IO
 * (na V10 rodava na thread principal e travava a UI em bibliotecas grandes).
 */
internal class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val store = UserData(application)

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

    // ---------- biblioteca ----------
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
                    runCatching { LibraryRepository.loadTracks(getApplication<Application>(), store) }
                }
                // Em caso de erro (ex.: permissão revogada) mantém a lista atual em vez de zerá-la.
                result.onSuccess { tracks = it }
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
            getApplication<Application>().contentResolver
                .registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, obs)
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
        observer?.let { runCatching { getApplication<Application>().contentResolver.unregisterContentObserver(it) } }
        observer = null
        super.onCleared()
    }

    // ---------- ordenação ----------
    fun updateSort(by: String, descending: Boolean) {
        sortBy = by
        sortDescending = descending
        store.sortBy = by
        store.sortDescending = descending
    }

    // ---------- favoritos ----------
    fun toggleFavorite(id: Long) {
        favorites = if (id in favorites) favorites - id else favorites + id
        store.saveFavorites(favorites)
    }

    // ---------- playlists ----------
    private fun findPlaylistKey(name: String): String? =
        playlists.keys.firstOrNull { it.equals(name.trim(), ignoreCase = true) }

    /** Cria (ou reutiliza, ignorando maiúsculas/minúsculas) e opcionalmente já adiciona uma faixa. */
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

    // ---------- edição / letras / exclusão ----------
    fun saveTrackEdit(id: Long, title: String?, artist: String?, album: String?, cover: String?) {
        store.saveTrackEdit(id, title, artist, album, cover)
        refresh()
    }

    fun setLyricsUri(id: Long, uri: String?) {
        store.setLyricsUri(id, uri)
        lyricsVersion++
    }

    fun hasCustomLyrics(id: Long): Boolean = store.lyricsUri(id) != null

    suspend fun loadLyrics(track: AudioTrack): String? =
        LyricsLoader.load(getApplication<Application>(), store, track)

    fun onTrackDeleted(id: Long) {
        tracks = tracks.filterNot { it.id == id }
        favorites = favorites - id
        store.saveFavorites(favorites)
        playlists = playlists.mapValues { (_, ids) -> ids - id }
        store.savePlaylists(playlists)
        store.clearTrackData(id)
    }
}
