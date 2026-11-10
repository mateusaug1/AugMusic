package com.augustos.augmusic

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal data class TrackRef(val id: Long, val title: String, val artist: String, val durationMs: Long)

internal data class BackupSections(
    val settings: Boolean = true,
    val playlists: Boolean = true,
    val favorites: Boolean = true
)

internal class ParsedBackup(
    val appVersion: String,
    val createdAt: Long,
    val settings: AppSettings?,
    val equalizer: EqSettings?,
    val sortBy: String?,
    val sortDescending: Boolean?,
    val playlists: List<Pair<String, List<TrackRef>>>?,
    val favorites: List<TrackRef>?
)

internal class RestoreReport(
    val warnings: List<String>,
    val playlistsRestored: Int,
    val favoritesRestored: Int,
    val tracksNotFound: Int
)

/**
 * Backup em JSON (apenas configurações e referências; NUNCA os arquivos de áudio).
 * Faixas são guardadas com id + título + artista + duração: os ids do MediaStore mudam entre
 * aparelhos/reinstalações, então na restauração a faixa é reencontrada pelo id (se bater título)
 * ou pela impressão digital título+artista+duração.
 */
internal object BackupManager {
    private const val FORMAT = "wmusic-backup"
    private const val SCHEMA = 1

    private fun ref(t: AudioTrack) = JSONObject()
        .put("id", t.id).put("title", t.title).put("artist", t.artist).put("duration", t.duration)

    fun build(
        versionName: String,
        sections: BackupSections,
        settings: AppSettings,
        eq: EqSettings,
        sortBy: String,
        sortDescending: Boolean,
        playlists: Map<String, List<Long>>,
        favorites: Set<Long>,
        byId: Map<Long, AudioTrack>
    ): JSONObject = JSONObject().apply {
        put("format", FORMAT)
        put("schema", SCHEMA)
        put("app", "WMusic")
        put("appVersion", versionName)
        put("createdAt", System.currentTimeMillis())
        if (sections.settings) {
            put("settings", settings.toJson())
            put("equalizer", JSONObject()
                .put("enabled", eq.enabled).put("preset", eq.preset)
                .put("bands", JSONArray(eq.bands.map { it.toDouble() }))
                .put("bassBoost", eq.bassBoost.toDouble()).put("virtualizer", eq.virtualizer.toDouble())
                .put("reverb", eq.reverb.toDouble()))
            put("sort", JSONObject().put("by", sortBy).put("descending", sortDescending))
        }
        if (sections.playlists) {
            val arr = JSONArray()
            for ((name, ids) in playlists) {
                val tracks = JSONArray()
                ids.forEach { id -> byId[id]?.let { tracks.put(ref(it)) } }
                arr.put(JSONObject().put("name", name).put("tracks", tracks))
            }
            put("playlists", arr)
        }
        if (sections.favorites) {
            val arr = JSONArray()
            favorites.forEach { id -> byId[id]?.let { arr.put(ref(it)) } }
            put("favorites", arr)
        }
    }

    suspend fun write(context: Context, uri: Uri, json: JSONObject): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(json.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Não foi possível abrir o arquivo de destino")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun read(context: Context, uri: Uri): Result<ParsedBackup> = withContext(Dispatchers.IO) {
        try {
            val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            if (size > 20L * 1024 * 1024) error("Arquivo grande demais para ser um backup do WMusic")
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("Não foi possível abrir o arquivo")
            Result.success(parse(text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parse(text: String): ParsedBackup {
        val o = try { JSONObject(text) } catch (_: Exception) { error("O arquivo não é um JSON válido") }
        if (o.optString("format") != FORMAT) error("Este arquivo não é um backup do WMusic")
        val schema = o.optInt("schema", 0)
        if (schema < 1 || schema > SCHEMA) error("Versão de backup não suportada (schema $schema)")

        fun refs(arr: JSONArray?): List<TrackRef> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val r = arr.optJSONObject(i) ?: return@mapNotNull null
                TrackRef(r.optLong("id", -1L), r.optString("title", ""), r.optString("artist", ""), r.optLong("duration", 0L))
            }
        }

        val eq = o.optJSONObject("equalizer")?.let { e ->
            val bands = e.optJSONArray("bands")?.let { a ->
                (0 until a.length()).map { a.optDouble(it, 0.0).toFloat().coerceIn(-12f, 12f) }
            }?.takeIf { it.size == EQ_BAND_COUNT } ?: List(EQ_BAND_COUNT) { 0f }
            EqSettings(
                enabled = e.optBoolean("enabled", false),
                preset = e.optString("preset", "Flat"),
                bands = bands,
                bassBoost = e.optDouble("bassBoost", 35.0).toFloat().coerceIn(0f, 100f),
                virtualizer = e.optDouble("virtualizer", 20.0).toFloat().coerceIn(0f, 100f),
                reverb = e.optDouble("reverb", 0.0).toFloat().coerceIn(0f, 100f)
            )
        }
        val playlists = o.optJSONArray("playlists")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val p = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = p.optString("name", "").trim()
                if (name.isEmpty()) null else name to refs(p.optJSONArray("tracks"))
            }
        }
        return ParsedBackup(
            appVersion = o.optString("appVersion", "?"),
            createdAt = o.optLong("createdAt", 0L),
            settings = o.optJSONObject("settings")?.let { AppSettings.fromJson(it) },
            equalizer = eq,
            sortBy = o.optJSONObject("sort")?.optString("by")?.takeIf { it.isNotBlank() },
            sortDescending = o.optJSONObject("sort")?.optBoolean("descending", false),
            playlists = playlists,
            favorites = if (o.has("favorites")) refs(o.optJSONArray("favorites")) else null
        )
    }

    /** Reencontra as faixas do backup na biblioteca atual. */
    class Matcher(tracks: List<AudioTrack>) {
        private val byId = tracks.associateBy { it.id }
        private val byKey = tracks.groupBy { normalizeForSearch(it.title) + "|" + normalizeForSearch(it.artist) }

        fun resolve(ref: TrackRef): Long? {
            byId[ref.id]?.let { t ->
                if (normalizeForSearch(t.title) == normalizeForSearch(ref.title)) return t.id
            }
            val candidates = byKey[normalizeForSearch(ref.title) + "|" + normalizeForSearch(ref.artist)].orEmpty()
            return candidates.firstOrNull { ref.durationMs <= 0L || kotlin.math.abs(it.duration - ref.durationMs) <= 2000L }?.id
        }
    }

    /** Verifica as pastas do backup neste aparelho. Mantém as entradas, mas avisa o que não existe. */
    fun folderWarnings(settings: AppSettings): List<String> {
        val out = ArrayList<String>()
        for ((label, list) in listOf("Whitelist" to settings.whitelist, "Blacklist" to settings.blacklist)) {
            for (path in list) {
                val dir = File(path)
                when {
                    !path.startsWith("/") -> out.add("$label: caminho inválido ignorado ($path)")
                    !dir.isDirectory -> out.add("$label: a pasta \"${LibraryFilter.displayName(path)}\" não foi encontrada ou não está acessível neste aparelho")
                }
            }
        }
        return out
    }
}
