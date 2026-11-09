package com.augustos.augmusic

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.Reader

/**
 * Ordem de busca:
 *  1. arquivo escolhido pelo usuário (via seletor do sistema, funciona no Android 11+);
 *  2. .lrc / .txt com o mesmo nome do áudio, na mesma pasta (só funciona onde o app tem acesso
 *     direto a arquivos; no Android 11+ o sistema bloqueia esse acesso, por isso o item 1 existe).
 * Marcações de tempo do formato LRC ([00:12.34]) são removidas para a letra ficar legível.
 */
internal object LyricsLoader {
    private const val MAX_CHARS = 30_000
    private val TIME_TAG = Regex("\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?]")
    private val WORD_TAG = Regex("<\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?>")
    private val META_LINE = Regex("^\\[[A-Za-z]+:[^\\]]*]\\s*$")

    suspend fun load(context: Context, store: UserData, track: AudioTrack): String? =
        withContext(Dispatchers.IO) {
            try {
                store.lyricsUri(track.id)?.let { saved ->
                    readUri(context, Uri.parse(saved))?.let { return@withContext clean(it) }
                }
                val path = track.path ?: return@withContext null
                val file = File(path)
                val parent = file.parentFile ?: return@withContext null
                val base = file.nameWithoutExtension
                listOf(
                    File(parent, "$base.lrc"),
                    File(parent, "$base.txt"),
                    File(parent, "lyrics.lrc"),
                    File(parent, "lyrics.txt")
                ).firstOrNull { it.isFile && it.canRead() }
                    ?.let { readFile(it) }
                    ?.let { clean(it) }
            } catch (_: Exception) {
                null
            }
        }

    private fun clean(raw: String): String? =
        raw.removePrefix("\uFEFF")
            .lineSequence()
            .mapNotNull { line ->
                if (META_LINE.matches(line.trim())) null
                else WORD_TAG.replace(TIME_TAG.replace(line, ""), "").trim()
            }
            .joinToString("\n")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
            .ifEmpty { null }

    private fun readLimited(reader: Reader): String {
        val buf = CharArray(MAX_CHARS)
        var total = 0
        while (total < MAX_CHARS) {
            val n = reader.read(buf, total, MAX_CHARS - total)
            if (n < 0) break
            total += n
        }
        return String(buf, 0, total)
    }

    private fun readUri(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { readLimited(it.bufferedReader(Charsets.UTF_8)) }
    } catch (_: Exception) {
        null
    }

    private fun readFile(file: File): String =
        file.inputStream().use { readLimited(it.bufferedReader(Charsets.UTF_8)) }
}
