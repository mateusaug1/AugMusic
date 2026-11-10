package com.augustos.augmusic

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Estado de navegação único: Início, Biblioteca, Pesquisa e Configurações (com sub-telas). */
@Stable
internal class AppNav {
    var destination by mutableStateOf("home")
    var libraryTab by mutableStateOf("Músicas")
    var collectionKey by mutableStateOf<String?>(null)
    var collectionLabel by mutableStateOf<String?>(null)
    var playlist by mutableStateOf<String?>(null)
    var settingsPage by mutableStateOf<String?>(null)
    var focusTrackId by mutableStateOf<Long?>(null)

    val inLibraryDetail: Boolean get() = collectionLabel != null || playlist != null
    val canGoBack: Boolean
        get() = destination != "home"

    fun clearDetail() {
        collectionKey = null
        collectionLabel = null
        playlist = null
    }

    /** Toque na barra inferior: muda de destino; tocar no destino atual volta à raiz dele. */
    fun select(dest: String) {
        if (dest == destination) {
            clearDetail()
            settingsPage = null
        }
        destination = dest
    }

    fun openLibrary(tab: String) {
        clearDetail()
        libraryTab = tab
        destination = "library"
    }

    fun openAlbum(key: String, label: String) {
        libraryTab = "Álbuns"; collectionKey = key; collectionLabel = label; playlist = null
        destination = "library"
    }

    fun openArtist(key: String, label: String) {
        libraryTab = "Artistas"; collectionKey = key; collectionLabel = label; playlist = null
        destination = "library"
    }

    fun openPlaylist(name: String) {
        libraryTab = "Playlists"; collectionKey = null; collectionLabel = null; playlist = name
        destination = "library"
    }

    fun openSettings(page: String?) {
        settingsPage = page
        destination = "settings"
    }

    fun showTrackInLibrary(track: AudioTrack) {
        clearDetail()
        libraryTab = "Músicas"
        focusTrackId = track.id
        destination = "library"
    }

    fun back() {
        when {
            destination == "settings" && settingsPage != null -> settingsPage = null
            destination == "library" && inLibraryDetail -> clearDetail()
            else -> destination = "home"
        }
    }
}

/** Ações compartilhadas entre telas (reprodução e menus). */
@Stable
internal class AppActions(
    val play: (AudioTrack, List<AudioTrack>) -> Unit,
    val playList: (List<AudioTrack>, Boolean) -> Unit,
    val menu: (AudioTrack) -> Unit,
    val openFull: () -> Unit
)

/** Mensagem do estado vazio conforme o motivo real (permissão, filtro, biblioteca vazia). */
internal fun emptyLibraryHint(vm: MainViewModel, hasPermission: Boolean): Pair<String, String> = when {
    !hasPermission -> "Sem acesso às músicas" to "Permita o acesso ao áudio para listar sua biblioteca."
    vm.settings.libraryMode == LibraryMode.WHITELIST && vm.settings.whitelist.isEmpty() ->
        "Whitelist sem pastas" to "Adicione pastas em Configurações › Biblioteca ou mude para Blacklist."
    vm.rawTracks.isNotEmpty() && vm.tracks.isEmpty() ->
        "Tudo está oculto pelos filtros" to "Revise as pastas em Configurações › Biblioteca."
    else -> "Nenhuma música encontrada" to "Adicione músicas ao aparelho e atualize a biblioteca."
}
