package com.augustos.augmusic

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Acesso centralizado ao SharedPreferences "wmusic_preferences" (mesmo arquivo das versões
 * anteriores, portanto favoritos, edições e ordenação do usuário são preservados).
 */
internal class UserData(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    companion object {
        const val PREFS = "wmusic_preferences"
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_SORT_DESC = "sort_descending"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_PLAYLISTS_V2 = "playlists_v2"
        private const val LEGACY_PLAYLIST_NAMES = "playlist_names"
        private const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"
    }

    // ---------- ordenação ----------
    var sortBy: String
        get() = prefs.getString(KEY_SORT_BY, "Nome") ?: "Nome"
        set(value) { prefs.edit().putString(KEY_SORT_BY, value).apply() }

    var sortDescending: Boolean
        get() = prefs.getBoolean(KEY_SORT_DESC, false)
        set(value) { prefs.edit().putBoolean(KEY_SORT_DESC, value).apply() }

    var askedNotifications: Boolean
        get() = prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)
        set(value) { prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, value).apply() }

    // ---------- favoritos ----------
    fun loadFavorites(): Set<Long> =
        (prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun saveFavorites(ids: Set<Long>) {
        prefs.edit().putStringSet(KEY_FAVORITES, ids.map { it.toString() }.toSet()).apply()
    }

    // ---------- playlists ----------
    /**
     * Playlists agora ficam em um único JSON (nome -> ids, em ordem). O formato antigo usava
     * "playlist_ids_<hashCode do nome>", sujeito a colisões (ex.: "Aa" e "BB" têm o mesmo hash).
     * Na primeira execução da V12 os dados antigos são migrados; as chaves antigas não são apagadas.
     */
    fun loadPlaylists(): Map<String, List<Long>> {
        val raw = prefs.getString(KEY_PLAYLISTS_V2, null)
        if (raw != null) {
            return try {
                val obj = JSONObject(raw)
                val out = LinkedHashMap<String, List<Long>>()
                val names = obj.keys()
                while (names.hasNext()) {
                    val name = names.next()
                    val arr = obj.optJSONArray(name) ?: continue
                    val ids = ArrayList<Long>(arr.length())
                    for (i in 0 until arr.length()) ids.add(arr.optLong(i))
                    out[name] = ids.distinct()
                }
                out
            } catch (_: Exception) {
                emptyMap()
            }
        }
        val legacyNames = prefs.getStringSet(LEGACY_PLAYLIST_NAMES, emptySet()) ?: emptySet()
        val migrated = LinkedHashMap<String, List<Long>>()
        for (name in legacyNames) {
            migrated[name] = prefs.getString("playlist_ids_${name.hashCode()}", "")
                .orEmpty()
                .split(',')
                .mapNotNull { it.trim().toLongOrNull() }
                .distinct()
        }
        if (migrated.isNotEmpty()) savePlaylists(migrated)
        return migrated
    }

    fun savePlaylists(playlists: Map<String, List<Long>>) {
        val obj = JSONObject()
        for ((name, ids) in playlists) {
            val arr = JSONArray()
            ids.forEach { arr.put(it) }
            obj.put(name, arr)
        }
        prefs.edit().putString(KEY_PLAYLISTS_V2, obj.toString()).apply()
    }

    // ---------- edições por faixa (título / artista / álbum / capa) ----------
    fun getOverride(id: Long, field: String): String? =
        prefs.getString("track_${id}_$field", null)?.takeIf { it.isNotBlank() }

    /** Campo nulo = remove a edição e volta a usar a tag original do arquivo. */
    fun saveTrackEdit(id: Long, title: String?, artist: String?, album: String?, cover: String?) {
        val e = prefs.edit()
        fun put(field: String, value: String?) {
            if (value.isNullOrBlank()) e.remove("track_${id}_$field")
            else e.putString("track_${id}_$field", value)
        }
        put("title", title)
        put("artist", artist)
        put("album", album)
        put("cover", cover)
        e.apply()
    }

    fun lyricsUri(id: Long): String? = getOverride(id, "lyrics")

    fun setLyricsUri(id: Long, uri: String?) {
        val e = prefs.edit()
        if (uri.isNullOrBlank()) e.remove("track_${id}_lyrics") else e.putString("track_${id}_lyrics", uri)
        e.apply()
    }

    fun clearTrackData(id: Long) {
        val e = prefs.edit()
        listOf("title", "artist", "album", "cover", "lyrics").forEach { e.remove("track_${id}_$it") }
        e.apply()
    }

    // ---------- configurações V13 (JSON único; entra no backup) ----------
    fun loadSettings(): AppSettings =
        prefs.getString("settings_v13", null)
            ?.let { raw -> try { AppSettings.fromJson(org.json.JSONObject(raw)) } catch (_: Exception) { null } }
            ?: AppSettings()

    fun saveSettings(s: AppSettings) {
        prefs.edit().putString("settings_v13", s.toJson().toString()).apply()
    }

    // ---------- equalizador ----------
    fun loadEq(): EqSettings {
        val bands = prefs.getString("eq_bands", null)
            ?.split(',')
            ?.mapNotNull { it.toFloatOrNull() }
            ?.takeIf { it.size == EQ_BAND_COUNT }
            ?: List(EQ_BAND_COUNT) { 0f }
        return EqSettings(
            enabled = prefs.getBoolean("eq_enabled", false),
            preset = prefs.getString("eq_preset", "Flat") ?: "Flat",
            bands = bands,
            bassBoost = prefs.getFloat("eq_bass", 35f),
            virtualizer = prefs.getFloat("eq_virtualizer", 20f),
            reverb = prefs.getFloat("eq_reverb", 0f)
        )
    }

    fun saveEq(s: EqSettings) {
        prefs.edit()
            .putBoolean("eq_enabled", s.enabled)
            .putString("eq_preset", s.preset)
            .putString("eq_bands", s.bands.joinToString(","))
            .putFloat("eq_bass", s.bassBoost)
            .putFloat("eq_virtualizer", s.virtualizer)
            .putFloat("eq_reverb", s.reverb)
            .apply()
    }
}
