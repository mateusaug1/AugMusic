package com.augustos.augmusic

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Letras salvas DENTRO do app (filesDir/lyrics/<id>.txt). É a "associação da letra à música":
 * pode vir da pesquisa online, de colar/editar manualmente ou de um arquivo importado.
 * Se a letra encontrada for sincronizada, o .lrc original também é guardado em <id>.lrc
 * para suporte futuro — hoje a exibição é sempre estática (sem sincronização fictícia).
 */
internal object LyricsStorage {
    private fun dir(context: Context) = File(context.filesDir, "lyrics").apply { mkdirs() }

    fun save(context: Context, trackId: Long, plain: String, synced: String? = null) {
        File(dir(context), "$trackId.txt").writeText(plain, Charsets.UTF_8)
        val lrc = File(dir(context), "$trackId.lrc")
        if (synced.isNullOrBlank()) lrc.delete() else lrc.writeText(synced, Charsets.UTF_8)
    }

    fun read(context: Context, trackId: Long): String? =
        File(dir(context), "$trackId.txt").takeIf { it.isFile }?.readText(Charsets.UTF_8)?.takeIf { it.isNotBlank() }

    fun has(context: Context, trackId: Long): Boolean = File(dir(context), "$trackId.txt").isFile

    fun delete(context: Context, trackId: Long) {
        File(dir(context), "$trackId.txt").delete()
        File(dir(context), "$trackId.lrc").delete()
    }

    fun count(context: Context): Int = dir(context).listFiles { f -> f.extension == "txt" }?.size ?: 0

    fun clearAll(context: Context) {
        dir(context).listFiles()?.forEach { it.delete() }
    }
}

/**
 * Ordem de busca ao abrir a letra:
 *  1. letra salva no app;
 *  2. arquivo escolhido pelo usuário (seletor do sistema — funciona no Android 11+);
 *  3. .lrc / .txt com o mesmo nome do áudio, na mesma pasta (só onde o app tem acesso direto).
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
                LyricsStorage.read(context, track.id)?.let { return@withContext it }
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

    fun clean(raw: String): String? =
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

internal data class LyricsCandidate(
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val plain: String,
    val synced: String?
)

/**
 * Pesquisa de letras no LRCLIB (https://lrclib.net, serviço aberto e gratuito).
 * Só é chamada quando o usuário toca em "Pesquisar online"; nenhum dado é enviado automaticamente.
 */
internal object LrcLib {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    suspend fun search(track: AudioTrack): Result<List<LyricsCandidate>> = withContext(Dispatchers.IO) {
        try {
            val query = StringBuilder("https://lrclib.net/api/search?track_name=").append(enc(track.title))
            if (!track.artist.equals("Artista Desconhecido", ignoreCase = true)) {
                query.append("&artist_name=").append(enc(track.artist))
            }
            val conn = URL(query.toString()).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("User-Agent", "WMusic/13.0 (Android)")
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode != 200) error("Servidor respondeu ${conn.responseCode}")
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val arr = JSONArray(body)
                val list = ArrayList<LyricsCandidate>()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val synced = o.optString("syncedLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                    val plainRaw = o.optString("plainLyrics", "").takeIf { it.isNotBlank() && it != "null" }
                    val plain = plainRaw ?: synced?.let { LyricsLoader.clean(it) } ?: continue
                    list.add(
                        LyricsCandidate(
                            title = o.optString("trackName", ""),
                            artist = o.optString("artistName", ""),
                            album = o.optString("albumName", ""),
                            durationSec = o.optInt("duration", 0),
                            plain = plain,
                            synced = synced
                        )
                    )
                }
                val target = (track.duration / 1000L).toInt()
                Result.success(list.sortedBy { if (it.durationSec > 0) abs(it.durationSec - target) else 9999 }.take(15))
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
