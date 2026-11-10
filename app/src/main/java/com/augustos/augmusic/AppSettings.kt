package com.augustos.augmusic

import org.json.JSONArray
import org.json.JSONObject

internal enum class ThemeMode(val label: String) { AUTO("Automático"), LIGHT("Claro"), DARK("Escuro") }
internal enum class BackgroundMode(val label: String) {
    DYNAMIC("Dinâmico"), SOLID("Sólido"), GRADIENT("Gradiente"), BLUR("Desfocado")
}
internal enum class CoverStyle(val label: String) { ELEVATED("Elevada"), FLAT("Plana"), FRAMED("Moldura") }
internal enum class ButtonStyle(val label: String) { CIRCLE("Círculo"), SQUARE("Arredondado"), OUTLINED("Contorno") }
internal enum class MiniStyle(val label: String) { FLOATING("Flutuante"), BAR("Barra"), COMPACT("Compacto") }
internal enum class MiniTap(val label: String) { OPEN_PLAYER("Abrir player"), PLAY_PAUSE("Reproduzir/pausar") }
internal enum class LibraryMode(val label: String) { WHITELIST("Whitelist"), BLACKLIST("Blacklist") }

internal enum class GestureAction(val label: String) {
    NONE("Nenhuma"),
    PREVIOUS("Música anterior"),
    NEXT("Próxima música"),
    VOLUME_UP("Aumentar volume"),
    VOLUME_DOWN("Diminuir volume"),
    PLAY_PAUSE("Reproduzir / pausar"),
    TOGGLE_FAVORITE("Favoritar"),
    TOGGLE_SHUFFLE("Aleatório"),
    CYCLE_REPEAT("Modo de repetição"),
    OPEN_QUEUE("Abrir fila"),
    OPEN_MENU("Menu da música")
}

internal data class GestureConfig(val enabled: Boolean, val action: GestureAction)

internal inline fun <reified T : Enum<T>> JSONObject.enumOr(key: String, def: T): T =
    try { enumValueOf<T>(optString(key, def.name)) } catch (_: Exception) { def }

private fun JSONObject.gesture(key: String, def: GestureConfig): GestureConfig {
    val o = optJSONObject(key) ?: return def
    return GestureConfig(o.optBoolean("enabled", def.enabled), o.enumOr("action", def.action))
}

private fun JSONObject.strings(key: String): List<String> {
    val arr = optJSONArray(key) ?: return emptyList()
    return (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }.distinct()
}

/**
 * Todas as preferências visuais/comportamentais da V13 em um único objeto imutável,
 * persistido como JSON (o mesmo JSON entra no backup). Alterações valem imediatamente.
 */
internal data class AppSettings(
    // aparência
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val accentColor: Int = DEFAULT_ACCENT,
    val adaptiveColors: Boolean = false,
    val gradients: Boolean = true,
    // informações exibidas
    val showArtist: Boolean = true,
    val showAlbum: Boolean = true,
    val showTechInfo: Boolean = true,
    val showDuration: Boolean = true,
    // player full
    val coverStyle: CoverStyle = CoverStyle.ELEVATED,
    val coverSizePct: Int = 84,
    val coverCorner: Int = 28,
    val coverAnimation: Boolean = true,
    val backgroundMode: BackgroundMode = BackgroundMode.DYNAMIC,
    val buttonStyle: ButtonStyle = ButtonStyle.CIRCLE,
    val controlsScalePct: Int = 100,
    val showSecondaryControls: Boolean = true,
    // mini player
    val miniEnabled: Boolean = true,
    val miniStyle: MiniStyle = MiniStyle.FLOATING,
    val miniShowCover: Boolean = true,
    val miniShowTitle: Boolean = true,
    val miniShowArtist: Boolean = true,
    val miniShowNext: Boolean = true,
    val miniShowProgress: Boolean = true,
    val miniTap: MiniTap = MiniTap.OPEN_PLAYER,
    // letras
    val lyricsEnabled: Boolean = true,
    val lyricsOnlineSearch: Boolean = true,
    val lyricsTextSize: Int = 18,
    // biblioteca
    val libraryMode: LibraryMode = LibraryMode.BLACKLIST,
    val whitelist: List<String> = emptyList(),
    val blacklist: List<String> = emptyList(),
    // gestos (aplicados na capa do player completo)
    val swipeRight: GestureConfig = GestureConfig(true, GestureAction.PREVIOUS),
    val swipeLeft: GestureConfig = GestureConfig(true, GestureAction.NEXT),
    val swipeUp: GestureConfig = GestureConfig(true, GestureAction.VOLUME_UP),
    val swipeDown: GestureConfig = GestureConfig(true, GestureAction.VOLUME_DOWN),
    val doubleTap: GestureConfig = GestureConfig(true, GestureAction.PLAY_PAUSE),
    val longPress: GestureConfig = GestureConfig(true, GestureAction.OPEN_MENU)
) {
    fun withDefaultAppearance(): AppSettings {
        val d = AppSettings()
        return copy(
            themeMode = d.themeMode, accentColor = d.accentColor, adaptiveColors = d.adaptiveColors,
            gradients = d.gradients, showArtist = d.showArtist, showAlbum = d.showAlbum,
            showTechInfo = d.showTechInfo, showDuration = d.showDuration
        )
    }

    fun withDefaultPlayer(): AppSettings {
        val d = AppSettings()
        return copy(
            coverStyle = d.coverStyle, coverSizePct = d.coverSizePct, coverCorner = d.coverCorner,
            coverAnimation = d.coverAnimation, backgroundMode = d.backgroundMode,
            buttonStyle = d.buttonStyle, controlsScalePct = d.controlsScalePct,
            showSecondaryControls = d.showSecondaryControls, miniEnabled = d.miniEnabled,
            miniStyle = d.miniStyle, miniShowCover = d.miniShowCover, miniShowTitle = d.miniShowTitle,
            miniShowArtist = d.miniShowArtist, miniShowNext = d.miniShowNext,
            miniShowProgress = d.miniShowProgress, miniTap = d.miniTap
        )
    }

    fun withDefaultGestures(): AppSettings {
        val d = AppSettings()
        return copy(
            swipeRight = d.swipeRight, swipeLeft = d.swipeLeft, swipeUp = d.swipeUp,
            swipeDown = d.swipeDown, doubleTap = d.doubleTap, longPress = d.longPress
        )
    }

    fun withDefaultLyrics(): AppSettings {
        val d = AppSettings()
        return copy(lyricsEnabled = d.lyricsEnabled, lyricsOnlineSearch = d.lyricsOnlineSearch, lyricsTextSize = d.lyricsTextSize)
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("themeMode", themeMode.name); put("accentColor", accentColor)
        put("adaptiveColors", adaptiveColors); put("gradients", gradients)
        put("showArtist", showArtist); put("showAlbum", showAlbum)
        put("showTechInfo", showTechInfo); put("showDuration", showDuration)
        put("coverStyle", coverStyle.name); put("coverSizePct", coverSizePct)
        put("coverCorner", coverCorner); put("coverAnimation", coverAnimation)
        put("backgroundMode", backgroundMode.name); put("buttonStyle", buttonStyle.name)
        put("controlsScalePct", controlsScalePct); put("showSecondaryControls", showSecondaryControls)
        put("miniEnabled", miniEnabled); put("miniStyle", miniStyle.name)
        put("miniShowCover", miniShowCover); put("miniShowTitle", miniShowTitle)
        put("miniShowArtist", miniShowArtist); put("miniShowNext", miniShowNext)
        put("miniShowProgress", miniShowProgress); put("miniTap", miniTap.name)
        put("lyricsEnabled", lyricsEnabled); put("lyricsOnlineSearch", lyricsOnlineSearch)
        put("lyricsTextSize", lyricsTextSize)
        put("libraryMode", libraryMode.name)
        put("whitelist", JSONArray(whitelist)); put("blacklist", JSONArray(blacklist))
        put("gestures", JSONObject().apply {
            fun g(k: String, c: GestureConfig) =
                put(k, JSONObject().put("enabled", c.enabled).put("action", c.action.name))
            g("swipeRight", swipeRight); g("swipeLeft", swipeLeft); g("swipeUp", swipeUp)
            g("swipeDown", swipeDown); g("doubleTap", doubleTap); g("longPress", longPress)
        })
    }

    companion object {
        /** Tolerante: campo ausente/inválido assume o padrão (compatível com backups de outras versões). */
        fun fromJson(o: JSONObject): AppSettings {
            val d = AppSettings()
            val g = o.optJSONObject("gestures") ?: JSONObject()
            return AppSettings(
                themeMode = o.enumOr("themeMode", d.themeMode),
                accentColor = o.optInt("accentColor", d.accentColor),
                adaptiveColors = o.optBoolean("adaptiveColors", d.adaptiveColors),
                gradients = o.optBoolean("gradients", d.gradients),
                showArtist = o.optBoolean("showArtist", d.showArtist),
                showAlbum = o.optBoolean("showAlbum", d.showAlbum),
                showTechInfo = o.optBoolean("showTechInfo", d.showTechInfo),
                showDuration = o.optBoolean("showDuration", d.showDuration),
                coverStyle = o.enumOr("coverStyle", d.coverStyle),
                coverSizePct = o.optInt("coverSizePct", d.coverSizePct).coerceIn(55, 100),
                coverCorner = o.optInt("coverCorner", d.coverCorner).coerceIn(0, 48),
                coverAnimation = o.optBoolean("coverAnimation", d.coverAnimation),
                backgroundMode = o.enumOr("backgroundMode", d.backgroundMode),
                buttonStyle = o.enumOr("buttonStyle", d.buttonStyle),
                controlsScalePct = o.optInt("controlsScalePct", d.controlsScalePct).coerceIn(80, 130),
                showSecondaryControls = o.optBoolean("showSecondaryControls", d.showSecondaryControls),
                miniEnabled = o.optBoolean("miniEnabled", d.miniEnabled),
                miniStyle = o.enumOr("miniStyle", d.miniStyle),
                miniShowCover = o.optBoolean("miniShowCover", d.miniShowCover),
                miniShowTitle = o.optBoolean("miniShowTitle", d.miniShowTitle),
                miniShowArtist = o.optBoolean("miniShowArtist", d.miniShowArtist),
                miniShowNext = o.optBoolean("miniShowNext", d.miniShowNext),
                miniShowProgress = o.optBoolean("miniShowProgress", d.miniShowProgress),
                miniTap = o.enumOr("miniTap", d.miniTap),
                lyricsEnabled = o.optBoolean("lyricsEnabled", d.lyricsEnabled),
                lyricsOnlineSearch = o.optBoolean("lyricsOnlineSearch", d.lyricsOnlineSearch),
                lyricsTextSize = o.optInt("lyricsTextSize", d.lyricsTextSize).coerceIn(12, 30),
                libraryMode = o.enumOr("libraryMode", d.libraryMode),
                whitelist = o.strings("whitelist"),
                blacklist = o.strings("blacklist"),
                swipeRight = g.gesture("swipeRight", d.swipeRight),
                swipeLeft = g.gesture("swipeLeft", d.swipeLeft),
                swipeUp = g.gesture("swipeUp", d.swipeUp),
                swipeDown = g.gesture("swipeDown", d.swipeDown),
                doubleTap = g.gesture("doubleTap", d.doubleTap),
                longPress = g.gesture("longPress", d.longPress)
            )
        }
    }
}
