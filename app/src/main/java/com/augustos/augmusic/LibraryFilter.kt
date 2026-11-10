package com.augustos.augmusic

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract

/**
 * Whitelist / Blacklist por pasta.
 * O seletor nativo (ACTION_OPEN_DOCUMENT_TREE) devolve um URI de árvore; ele é convertido em caminho
 * absoluto e comparado com o caminho de cada faixa. O filtro roda em memória sobre a lista já lida do
 * MediaStore — trocar de modo ou de pastas NÃO dispara nova varredura.
 */
internal object LibraryFilter {
    private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

    private val internalRoot: String get() = Environment.getExternalStorageDirectory().absolutePath

    /** Retorna null para provedores que não correspondem a uma pasta real (ex.: Downloads virtual). */
    fun treeUriToPath(uri: Uri): String? = try {
        if (uri.authority != EXTERNAL_STORAGE_AUTHORITY) null
        else {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val idx = docId.indexOf(':')
            if (idx < 0) null
            else {
                val volume = docId.substring(0, idx)
                val rel = docId.substring(idx + 1).trim('/')
                val base = if (volume.equals("primary", ignoreCase = true)) internalRoot else "/storage/$volume"
                if (rel.isEmpty()) base else "$base/$rel"
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun isUnder(path: String?, folder: String): Boolean =
        path != null && path.startsWith(folder.trimEnd('/') + "/", ignoreCase = true)

    fun isInFolders(track: AudioTrack, folders: List<String>): Boolean = folders.any { isUnder(track.path, it) }

    fun apply(tracks: List<AudioTrack>, s: AppSettings): List<AudioTrack> = when (s.libraryMode) {
        LibraryMode.WHITELIST -> tracks.filter { isInFolders(it, s.whitelist) }
        LibraryMode.BLACKLIST ->
            if (s.blacklist.isEmpty()) tracks else tracks.filterNot { isInFolders(it, s.blacklist) }
    }

    fun displayName(path: String): String = when {
        path == internalRoot -> "Armazenamento interno"
        path.startsWith("$internalRoot/") -> "Armazenamento interno/" + path.removePrefix("$internalRoot/")
        else -> path
    }

    fun folderOf(path: String?): String? = path?.substringBeforeLast('/', "")?.takeIf { it.isNotEmpty() }

    /** "/storage/emulated/0/Music/Rock" -> "primary:Music/Rock" (para abrir no app Arquivos). */
    fun toDocumentId(path: String): String? = when {
        path == internalRoot -> "primary:"
        path.startsWith("$internalRoot/") -> "primary:" + path.removePrefix("$internalRoot/")
        path.startsWith("/storage/") -> {
            val rest = path.removePrefix("/storage/")
            val vol = rest.substringBefore('/')
            "$vol:" + rest.substringAfter('/', "")
        }
        else -> null
    }
}
