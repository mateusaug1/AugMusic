package com.augustos.augmusic

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =================================================================== blocos reutilizáveis

@Composable
private fun PageScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = WC
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 18.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar", tint = c.accentFg) }
            Text(title, color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
            content()
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String?, content: @Composable ColumnScope.() -> Unit) {
    val c = WC
    if (title != null) {
        Text(
            title.uppercase(), color = c.accentFg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 8.dp)
        )
    } else Spacer(Modifier.height(16.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(22.dp)).padding(vertical = 6.dp),
        content = content
    )
}

@Composable
private fun ToggleRow(title: String, subtitle: String? = null, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    val c = WC
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled) { onChange(!checked) }.padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = if (enabled) c.text else c.muted, fontWeight = FontWeight.Medium)
            if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun SliderRow(
    title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>,
    steps: Int = 0, onChange: (Float) -> Unit
) {
    val c = WC
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = c.text, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(valueText, color = c.accentFg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value, onValueChange = onChange, valueRange = range, steps = steps,
            colors = SliderDefaults.colors(thumbColor = c.accent, activeTrackColor = c.accent, inactiveTrackColor = c.outline)
        )
    }
}

@Composable
private fun <T> ChoiceRow(title: String, options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val c = WC
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text(title, color = c.text, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) -> WChip(label, value == selected, onClick = { onSelect(value) }) }
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector, title: String, subtitle: String? = null,
    destructive: Boolean = false, trailing: Boolean = false, onClick: () -> Unit
) {
    MenuRow(icon, title, subtitle, destructive, onClick)
    if (trailing) Unit
}

@Composable
private fun RestoreDefaultsButton(label: String = "Restaurar padrão", onClick: () -> Unit) {
    val c = WC
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        border = BorderStroke(1.dp, c.outline)
    ) {
        Icon(Icons.Default.Restore, null, tint = c.accentFg)
        Spacer(Modifier.width(8.dp))
        Text(label, color = c.text)
    }
}

@Composable
private fun Hint(text: String, warning: Boolean = false) {
    val c = WC
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        if (warning) {
            Icon(Icons.Default.Warning, null, tint = c.danger, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (warning) c.danger else c.muted, fontSize = 12.sp)
    }
}

// =================================================================== início das configurações

private class Category(val id: String, val icon: ImageVector, val title: String, val description: String)

private val CATEGORIES = listOf(
    Category("personalizacao", Icons.Default.Palette, "Personalização", "Temas, cores e aparência"),
    Category("player", Icons.Default.PlayCircle, "Player", "Player Full e Mini Player"),
    Category("biblioteca", Icons.Default.Folder, "Biblioteca", "Whitelist, Blacklist e pastas"),
    Category("letras", Icons.Default.Subtitles, "Letras", "Pesquisa e exibição"),
    Category("behavior", Icons.Default.TouchApp, "Behavior", "Gestos e ações"),
    Category("backup", Icons.Default.Backup, "Backup", "Exportação e restauração"),
    Category("localizador", Icons.Default.MyLocation, "Localizador", "Localização de músicas"),
    Category("sobre", Icons.Default.Info, "Sobre", "Informações e versão")
)

@Composable
internal fun SettingsScreen(
    vm: MainViewModel,
    nav: AppNav,
    hasPermission: Boolean,
    hasCurrentTrack: Boolean,
    onOpenPlayer: () -> Unit,
    onShowInLibrary: (AudioTrack) -> Unit
) {
    Crossfade(targetState = nav.settingsPage, animationSpec = tween(200), label = "settingsPage") { page ->
        val back = { nav.settingsPage = null }
        when (page) {
            "personalizacao" -> PersonalizationPage(vm, back)
            "player" -> PlayerPage(vm, hasCurrentTrack, onOpenPlayer, back)
            "biblioteca" -> LibraryPage(vm, hasPermission, back)
            "letras" -> LyricsPage(vm, back)
            "behavior" -> BehaviorPage(vm, back)
            "backup" -> BackupPage(vm, back)
            "localizador" -> LocatorPage(vm, onShowInLibrary, back)
            "sobre" -> AboutPage(vm, back)
            else -> SettingsHome(nav)
        }
    }
}

@Composable
private fun SettingsHome(nav: AppNav) {
    val c = WC
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        item {
            Text("Configurações", color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, modifier = Modifier.padding(top = 14.dp))
            Text("Ajuste o WMusic do seu jeito", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 14.dp))
        }
        items(CATEGORIES, key = { it.id }) { cat ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(22.dp))
                    .background(c.surface).border(1.dp, c.outline, RoundedCornerShape(22.dp))
                    .clickable { nav.settingsPage = cat.id }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(cat.icon, null, tint = c.accentFg)
                }
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(cat.title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(cat.description, color = c.muted, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = c.muted)
            }
        }
    }
}

// =================================================================== Personalização

@Composable
private fun PersonalizationPage(vm: MainViewModel, onBack: () -> Unit) {
    val c = WC
    val s = vm.settings
    PageScaffold("Personalização", onBack) {
        Section("Tema") {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(ThemeMode.AUTO, Icons.Default.SettingsBrightness, "Automático"),
                    Triple(ThemeMode.LIGHT, Icons.Default.LightMode, "Claro"),
                    Triple(ThemeMode.DARK, Icons.Default.DarkMode, "Escuro")
                ).forEach { (mode, icon, label) ->
                    val selected = s.themeMode == mode
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(if (selected) c.accent else c.surfaceHigh)
                            .clickable { vm.updateSettings { it.copy(themeMode = mode) } }.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(icon, null, tint = if (selected) c.onAccent else c.muted)
                        Spacer(Modifier.height(4.dp))
                        Text(label, color = if (selected) c.onAccent else c.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Hint("Automático acompanha o tema do Android.")
        }

        Section("Cor de destaque") {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ACCENT_SWATCHES.forEach { argb ->
                    val selected = !s.adaptiveColors && s.accentColor == argb
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(Color(argb))
                            .then(if (selected) Modifier.border(3.dp, c.text, CircleShape) else Modifier)
                            .clickable { vm.updateSettings { it.copy(accentColor = argb, adaptiveColors = false) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Default.Check, "Selecionada", tint = onColorFor(Color(argb)))
                    }
                }
            }
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(s.accentColor, hsv)
            SliderRow("Matiz personalizado", "${hsv[0].toInt()}°", hsv[0], 0f..360f) { hue ->
                vm.updateSettings { it.copy(accentColor = Color.hsv(hue, 0.55f, 0.95f).toArgb(), adaptiveColors = false) }
            }
            ToggleRow(
                "Cores adaptativas",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Usa as cores do papel de parede (Material You)"
                else "Disponível a partir do Android 12",
                s.adaptiveColors, enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) { on -> vm.updateSettings { it.copy(adaptiveColors = on) } }
            ToggleRow("Gradientes", "Degradê da cor de destaque nos fundos", s.gradients) { on ->
                vm.updateSettings { it.copy(gradients = on) }
            }
        }

        Section("Informações exibidas") {
            ToggleRow("Artista", null, s.showArtist) { on -> vm.updateSettings { it.copy(showArtist = on) } }
            ToggleRow("Álbum", null, s.showAlbum) { on -> vm.updateSettings { it.copy(showAlbum = on) } }
            ToggleRow("Informações técnicas", "Formato, bitrate e frequência no player", s.showTechInfo) { on ->
                vm.updateSettings { it.copy(showTechInfo = on) }
            }
            ToggleRow("Duração", null, s.showDuration) { on -> vm.updateSettings { it.copy(showDuration = on) } }
        }
        RestoreDefaultsButton { vm.updateSettings { it.withDefaultAppearance() } }
    }
}

// =================================================================== Player

@Composable
private fun PlayerPage(vm: MainViewModel, hasCurrentTrack: Boolean, onOpenPlayer: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = vm.settings
    PageScaffold("Player", onBack) {
        Section("Player Full") {
            ChoiceRow("Estilo da capa", CoverStyle.entries.map { it to it.label }, s.coverStyle) { v ->
                vm.updateSettings { it.copy(coverStyle = v) }
            }
            SliderRow("Tamanho da capa", "${s.coverSizePct}%", s.coverSizePct.toFloat(), 55f..100f) { v ->
                vm.updateSettings { it.copy(coverSizePct = v.toInt()) }
            }
            SliderRow("Cantos arredondados", "${s.coverCorner} dp", s.coverCorner.toFloat(), 0f..48f) { v ->
                vm.updateSettings { it.copy(coverCorner = v.toInt()) }
            }
            ToggleRow("Animações da capa", "Zoom ao pausar e transição entre músicas", s.coverAnimation) { on ->
                vm.updateSettings { it.copy(coverAnimation = on) }
            }
            ChoiceRow("Fundo", BackgroundMode.entries.map { it to it.label }, s.backgroundMode) { v ->
                vm.updateSettings { it.copy(backgroundMode = v) }
            }
            Hint(
                when (s.backgroundMode) {
                    BackgroundMode.DYNAMIC -> "Cores predominantes da capa, com transição suave entre músicas."
                    BackgroundMode.SOLID -> "Cor sólida do tema (a mais econômica em bateria)."
                    BackgroundMode.GRADIENT -> "Gradiente padrão do WMusic com a cor de destaque."
                    BackgroundMode.BLUR -> "A própria capa desfocada ao fundo."
                }
            )
            ChoiceRow("Estilo do botão play", ButtonStyle.entries.map { it to it.label }, s.buttonStyle) { v ->
                vm.updateSettings { it.copy(buttonStyle = v) }
            }
            SliderRow("Tamanho dos controles", "${s.controlsScalePct}%", s.controlsScalePct.toFloat(), 80f..130f) { v ->
                vm.updateSettings { it.copy(controlsScalePct = v.toInt()) }
            }
            ToggleRow("Controles secundários", "Aleatório, repetir e favorito", s.showSecondaryControls) { on ->
                vm.updateSettings { it.copy(showSecondaryControls = on) }
            }
            ActionRow(Icons.Default.PlayCircle, "Visualizar Player Full", "Abre o player com a música atual") {
                if (hasCurrentTrack) onOpenPlayer() else context.toast("Toque uma música primeiro.")
            }
        }

        Section("Mini Player") {
            ToggleRow("Mini Player ativado", "Barra de reprodução sobre a navegação", s.miniEnabled) { on ->
                vm.updateSettings { it.copy(miniEnabled = on) }
            }
            ChoiceRow("Aparência", MiniStyle.entries.map { it to it.label }, s.miniStyle) { v ->
                vm.updateSettings { it.copy(miniStyle = v) }
            }
            ToggleRow("Mostrar capa", null, s.miniShowCover) { on -> vm.updateSettings { it.copy(miniShowCover = on) } }
            ToggleRow("Mostrar título", null, s.miniShowTitle) { on -> vm.updateSettings { it.copy(miniShowTitle = on) } }
            ToggleRow("Mostrar artista", "Não aparece no estilo compacto", s.miniShowArtist) { on ->
                vm.updateSettings { it.copy(miniShowArtist = on) }
            }
            ToggleRow("Botão próxima música", null, s.miniShowNext) { on -> vm.updateSettings { it.copy(miniShowNext = on) } }
            ToggleRow("Indicador de progresso", null, s.miniShowProgress) { on ->
                vm.updateSettings { it.copy(miniShowProgress = on) }
            }
            ChoiceRow("Ao tocar no Mini Player", MiniTap.entries.map { it to it.label }, s.miniTap) { v ->
                vm.updateSettings { it.copy(miniTap = v) }
            }
        }
        RestoreDefaultsButton { vm.updateSettings { it.withDefaultPlayer() } }
    }
}

// =================================================================== Biblioteca

@Composable
private fun FolderList(
    title: String,
    description: String,
    active: Boolean,
    folders: List<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    val c = WC
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, color = c.text, fontWeight = FontWeight.SemiBold)
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ATIVA", color = c.onAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(c.accent).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(description, color = c.muted, fontSize = 12.sp)
            }
            TextButton(onClick = onAdd) {
                Icon(Icons.Default.Add, null, tint = c.accentFg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pasta", color = c.accentFg)
            }
        }
        if (folders.isEmpty()) {
            Text("Nenhuma pasta selecionada.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))
        }
        folders.forEach { path ->
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FolderOpen, null, tint = c.muted, modifier = Modifier.size(20.dp))
                Text(
                    LibraryFilter.displayName(path), color = c.text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 8.dp)
                )
                IconButton(onClick = { onRemove(path) }) { Icon(Icons.Default.Close, "Remover pasta", tint = c.muted) }
            }
        }
    }
}

@Composable
private fun LibraryPage(vm: MainViewModel, hasPermission: Boolean, onBack: () -> Unit) {
    val c = WC
    val context = LocalContext.current
    val s = vm.settings
    var pendingMode by remember { mutableStateOf(LibraryMode.BLACKLIST) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            if (!vm.addFolder(pendingMode, uri)) {
                context.toast("Esta pasta não pode ser usada (escolha uma pasta do armazenamento interno ou do cartão SD).", true)
            }
        }
    }
    PageScaffold("Biblioteca", onBack) {
        Section("Modo de pastas") {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    LibraryMode.WHITELIST to "Usa SOMENTE as pastas selecionadas",
                    LibraryMode.BLACKLIST to "Usa tudo, EXCETO as pastas selecionadas"
                ).forEach { (mode, desc) ->
                    val selected = s.libraryMode == mode
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .background(if (selected) c.accent else c.surfaceHigh)
                            .clickable { vm.updateSettings { it.copy(libraryMode = mode) } }.padding(14.dp)
                    ) {
                        Text(mode.label, color = if (selected) c.onAccent else c.text, fontWeight = FontWeight.Bold)
                        Text(desc, color = if (selected) c.onAccent.copy(alpha = 0.8f) else c.muted, fontSize = 11.sp)
                    }
                }
            }
            Hint("Só um modo fica ativo por vez. As listas dos dois modos são preservadas ao alternar.")
        }

        Section("Pastas") {
            FolderList(
                "Whitelist", "Somente estas pastas entram na biblioteca",
                s.libraryMode == LibraryMode.WHITELIST, s.whitelist,
                onAdd = { pendingMode = LibraryMode.WHITELIST; picker.launch(null) },
                onRemove = { vm.removeFolder(LibraryMode.WHITELIST, it) }
            )
            FolderList(
                "Blacklist", "Estas pastas são ignoradas por completo",
                s.libraryMode == LibraryMode.BLACKLIST, s.blacklist,
                onAdd = { pendingMode = LibraryMode.BLACKLIST; picker.launch(null) },
                onRemove = { vm.removeFolder(LibraryMode.BLACKLIST, it) }
            )
        }

        Section("Situação") {
            if (!hasPermission) Hint("Sem permissão de áudio: a biblioteca não pode ser lida.", warning = true)
            if (s.libraryMode == LibraryMode.WHITELIST && s.whitelist.isEmpty()) {
                Hint("A Whitelist está ativa e vazia, então nenhuma música aparece. Adicione ao menos uma pasta.", warning = true)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("${vm.tracks.size}", color = c.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("na biblioteca", color = c.muted, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text("${vm.rawTracks.size - vm.tracks.size}", color = c.text, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("ocultas pelo filtro", color = c.muted, fontSize = 12.sp)
                }
            }
            ActionRow(Icons.Default.Refresh, "Atualizar biblioteca", "Lê o MediaStore novamente") { vm.refresh() }
            Hint("Trocar de modo ou de pastas só refaz o filtro em memória, sem nova varredura. Pastas fora do alcance do MediaStore (ex.: sem mídia indexada) não aparecem.")
        }
    }
}

// =================================================================== Letras

@Composable
private fun LyricsPage(vm: MainViewModel, onBack: () -> Unit) {
    val s = vm.settings
    var saved by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(vm.lyricsVersion) { saved = vm.savedLyricsCount() }
    PageScaffold("Letras", onBack) {
        Section("Exibição") {
            ToggleRow("Exibir letras", "Mostra o botão de letras no player", s.lyricsEnabled) { on ->
                vm.updateSettings { it.copy(lyricsEnabled = on) }
            }
            SliderRow("Tamanho do texto", "${s.lyricsTextSize} sp", s.lyricsTextSize.toFloat(), 12f..30f) { v ->
                vm.updateSettings { it.copy(lyricsTextSize = v.toInt()) }
            }
        }
        Section("Pesquisa") {
            ToggleRow(
                "Pesquisa online", "Usa o serviço aberto LRCLIB, só quando você toca em \"Pesquisar online\"",
                s.lyricsOnlineSearch
            ) { on -> vm.updateSettings { it.copy(lyricsOnlineSearch = on) } }
            Hint("Abra o player, toque no ícone de letras e escolha: pesquisar online, colar/editar, ou escolher um arquivo .lrc/.txt. A letra fica salva no app e associada à música. Letras sincronizadas são guardadas para uso futuro, mas hoje são exibidas como texto estático.")
        }
        Section("Armazenamento") {
            Hint("$saved ${if (saved == 1) "letra salva" else "letras salvas"} no aparelho.")
            if (saved > 0) ActionRow(Icons.Default.Close, "Apagar todas as letras salvas", destructive = true) { confirmClear = true }
        }
        RestoreDefaultsButton { vm.updateSettings { it.withDefaultLyrics() } }
    }
    if (confirmClear) {
        ConfirmDialog(
            "Apagar letras?", "As $saved letras salvas pelo WMusic serão removidas. Arquivos .lrc/.txt externos não são afetados.",
            "Apagar", onConfirm = { confirmClear = false; scope.launch { vm.clearAllLyrics() } },
            onDismiss = { confirmClear = false }
        )
    }
}

// =================================================================== Behavior (gestos)

@Composable
private fun GestureCard(title: String, hint: String, cfg: GestureConfig, onChange: (GestureConfig) -> Unit) {
    val c = WC
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, fontWeight = FontWeight.Medium)
                Text(hint, color = c.muted, fontSize = 12.sp)
            }
            Switch(checked = cfg.enabled, onCheckedChange = { onChange(cfg.copy(enabled = it)) })
        }
        Spacer(Modifier.height(6.dp))
        Box {
            OutlinedButton(
                onClick = { open = true }, enabled = cfg.enabled,
                border = BorderStroke(1.dp, c.outline), modifier = Modifier.fillMaxWidth()
            ) {
                Text(cfg.action.label, color = if (cfg.enabled) c.text else c.muted, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ExpandMore, null, tint = c.muted)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                GestureAction.entries.forEach { a ->
                    DropdownMenuItem(
                        text = { Text(a.label, color = if (a == cfg.action) c.accentFg else c.text) },
                        onClick = { onChange(cfg.copy(action = a)); open = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun BehaviorPage(vm: MainViewModel, onBack: () -> Unit) {
    val s = vm.settings
    PageScaffold("Behavior", onBack) {
        Hint("Os gestos atuam sobre a capa no Player Full. O slider, os botões e o cabeçalho mantêm seus toques normais, e arrastar para baixo no cabeçalho continua minimizando o player — por isso não há conflito.")
        Section("Deslizar") {
            GestureCard("Deslizar para a direita", "Na capa do player", s.swipeRight) { v -> vm.updateSettings { it.copy(swipeRight = v) } }
            GestureCard("Deslizar para a esquerda", "Na capa do player", s.swipeLeft) { v -> vm.updateSettings { it.copy(swipeLeft = v) } }
            GestureCard("Deslizar para cima", "Volume sobe continuamente enquanto arrasta", s.swipeUp) { v -> vm.updateSettings { it.copy(swipeUp = v) } }
            GestureCard("Deslizar para baixo", "Volume desce continuamente enquanto arrasta", s.swipeDown) { v -> vm.updateSettings { it.copy(swipeDown = v) } }
        }
        Section("Toques") {
            GestureCard("Toque duplo", "Dois toques rápidos na capa", s.doubleTap) { v -> vm.updateSettings { it.copy(doubleTap = v) } }
            GestureCard("Pressionar e segurar", "Ação personalizada na capa", s.longPress) { v -> vm.updateSettings { it.copy(longPress = v) } }
        }
        RestoreDefaultsButton { vm.updateSettings { it.withDefaultGestures() } }
    }
}

// =================================================================== Backup

@Composable
private fun BackupPage(vm: MainViewModel, onBack: () -> Unit) {
    val c = WC
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportSections by remember { mutableStateOf(BackupSections()) }
    var pending by remember { mutableStateOf<ParsedBackup?>(null) }
    var restoreSections by remember { mutableStateOf(BackupSections()) }
    var report by remember { mutableStateOf<RestoreReport?>(null) }
    var busy by remember { mutableStateOf(false) }

    val createLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val r = vm.exportBackup(uri, exportSections)
                busy = false
                r.onSuccess { context.toast("Backup salvo") }
                r.onFailure { context.toast("Falha ao salvar o backup: ${it.message}", true) }
            }
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val r = vm.readBackup(uri)
                busy = false
                r.onSuccess { p ->
                    pending = p
                    restoreSections = BackupSections(p.settings != null, p.playlists != null, p.favorites != null)
                }
                r.onFailure { context.toast("Backup inválido: ${it.message}", true) }
            }
        }
    }

    PageScaffold("Backup", onBack) {
        Section("Criar backup") {
            Hint("Gera um arquivo JSON com suas preferências e referências. Os arquivos de áudio NÃO são incluídos.")
            CheckRow("Configurações e aparência", exportSections.settings) { exportSections = exportSections.copy(settings = it) }
            CheckRow("Playlists", exportSections.playlists) { exportSections = exportSections.copy(playlists = it) }
            CheckRow("Favoritos", exportSections.favorites) { exportSections = exportSections.copy(favorites = it) }
            Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Button(
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date())
                        createLauncher.launch("wmusic-backup-$stamp.json")
                    },
                    enabled = !busy && (exportSections.settings || exportSections.playlists || exportSections.favorites),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = c.accent, contentColor = c.onAccent)
                ) {
                    Icon(Icons.Default.Backup, null); Spacer(Modifier.width(8.dp)); Text("Escolher local e salvar")
                }
            }
        }
        Section("Restaurar") {
            Hint("Selecione um backup do WMusic. O conteúdo é validado e você confirma antes de qualquer substituição.")
            Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                OutlinedButton(
                    onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    enabled = !busy, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, c.outline)
                ) {
                    Icon(Icons.Default.Restore, null, tint = c.accentFg); Spacer(Modifier.width(8.dp))
                    Text("Importar arquivo de backup", color = c.text)
                }
            }
            if (busy) Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = c.accent)
                Spacer(Modifier.width(10.dp)); Text("Processando…", color = c.muted)
            }
        }
        Hint("Como os ids das músicas mudam entre aparelhos, playlists e favoritos são reencontrados por título, artista e duração. Pastas de Whitelist/Blacklist são conferidas e você é avisado se algo não existir neste aparelho.")
    }

    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Restaurar backup") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "WMusic ${p.appVersion}" + if (p.createdAt > 0) " · ${formatDateSeconds(p.createdAt / 1000L)}" else "",
                        color = c.muted, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    if (p.settings != null) CheckRow("Configurações e aparência", restoreSections.settings) { restoreSections = restoreSections.copy(settings = it) }
                    p.playlists?.let { pl ->
                        CheckRow("Playlists (${pl.size})", restoreSections.playlists) { restoreSections = restoreSections.copy(playlists = it) }
                    }
                    p.favorites?.let { fv ->
                        CheckRow("Favoritos (${fv.size})", restoreSections.favorites) { restoreSections = restoreSections.copy(favorites = it) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Isto SUBSTITUI as informações atuais das seções marcadas. Esta ação não pode ser desfeita.",
                        color = c.danger, fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = restoreSections.settings || restoreSections.playlists || restoreSections.favorites,
                    onClick = {
                        report = vm.restoreBackup(p, restoreSections)
                        pending = null
                    }
                ) { Text("Substituir e restaurar", color = c.accentFg) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancelar") } }
        )
    }

    report?.let { r ->
        AlertDialog(
            onDismissRequest = { report = null },
            title = { Text("Restauração concluída") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Playlists restauradas: ${r.playlistsRestored}")
                    Text("Favoritos restaurados: ${r.favoritesRestored}")
                    if (r.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("Avisos", fontWeight = FontWeight.Bold)
                        r.warnings.forEach { Text("• $it", fontSize = 12.sp, color = c.muted) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { report = null }) { Text("OK", color = c.accentFg) } }
        )
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = WC
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 14.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, color = c.text)
    }
}

// =================================================================== Localizador

@Composable
private fun LocatorPage(vm: MainViewModel, onShowInLibrary: (AudioTrack) -> Unit, onBack: () -> Unit) {
    val c = WC
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AudioTrack?>(null) }
    var unavailable by remember { mutableStateOf<List<AudioTrack>?>(null) }
    var checking by remember { mutableStateOf(false) }
    val q = remember(query) { normalizeForSearch(query.trim()) }
    val results = remember(vm.rawTracks, q) {
        if (q.isEmpty()) emptyList() else vm.rawTracks.filter { it.searchKey.contains(q) }.take(40)
    }

    PageScaffold("Localizador", onBack) {
        Section("Encontrar música") {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                placeholder = { Text("Título, artista ou álbum", color = c.muted) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = c.muted) },
                singleLine = true, shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = c.accent, unfocusedBorderColor = c.outline)
            )
            Hint("A busca considera todas as músicas que o Android indexou, inclusive as ocultas pelo filtro de pastas.")
            results.forEach { t ->
                val hidden = !LibraryFilter.apply(listOf(t), vm.settings).contains(t)
                Row(
                    Modifier.fillMaxWidth().clickable { selected = t }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtworkImage(t, Modifier.size(44.dp), RoundedCornerShape(12.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(t.title, color = c.text, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            LibraryFilter.folderOf(t.path)?.let { LibraryFilter.displayName(it) } ?: "Caminho não informado",
                            color = c.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (hidden) Text("OCULTA", color = c.danger, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (q.isNotEmpty() && results.isEmpty()) Hint("Nenhuma música encontrada.")
        }

        Section("Verificação") {
            Hint("Procura músicas que o Android ainda lista, mas cujo arquivo já não pode ser aberto (apagado, cartão SD removido, permissão revogada).")
            ActionRow(Icons.Default.Search, if (checking) "Verificando…" else "Verificar arquivos indisponíveis") {
                if (!checking) {
                    checking = true
                    scope.launch {
                        unavailable = vm.findUnavailable()
                        checking = false
                    }
                }
            }
            unavailable?.let { list ->
                if (list.isEmpty()) Hint("Todos os arquivos estão acessíveis.")
                else {
                    Hint("${list.size} arquivo(s) indisponível(is):", warning = true)
                    list.take(100).forEach { t ->
                        Text(
                            "• ${t.title} — ${t.path ?: t.uri}", color = c.muted, fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp).clickable { selected = t }
                        )
                    }
                }
            }
            ActionRow(Icons.Default.Refresh, "Atualizar biblioteca", "Remove entradas que não existem mais") { vm.refresh() }
        }
    }

    selected?.let { t ->
        val available by produceState<Boolean?>(null, t.id) { value = vm.isAvailable(t) }
        val hidden = !LibraryFilter.apply(listOf(t), vm.settings).contains(t)
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(t.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Artista: ${t.artist}")
                    Text("Álbum: ${t.album}")
                    Text("Pasta: ${LibraryFilter.folderOf(t.path)?.let { LibraryFilter.displayName(it) } ?: "não informada"}")
                    Text("Arquivo: ${t.path ?: "caminho não acessível"}", fontSize = 12.sp, color = c.muted)
                    Text("Tamanho: ${formatBytes(t.size)} · ${formatDuration(t.duration)}")
                    Text(
                        "Situação: " + when (available) {
                            null -> "verificando…"
                            true -> "disponível"
                            false -> "INDISPONÍVEL (arquivo não pode ser aberto)"
                        },
                        color = if (available == false) c.danger else c.text
                    )
                    if (hidden) Text("Esta música está fora da biblioteca por causa do modo ${vm.settings.libraryMode.label}.", color = c.danger, fontSize = 12.sp)
                }
            },
            confirmButton = {
                Row {
                    if (!hidden) TextButton(onClick = { onShowInLibrary(t); selected = null }) { Text("Mostrar na biblioteca", color = c.accentFg) }
                    TextButton(onClick = { openFileLocation(context, t) }) { Text("Abrir pasta", color = c.accentFg) }
                }
            },
            dismissButton = { TextButton(onClick = { selected = null }) { Text("Fechar") } }
        )
    }
}

// =================================================================== Sobre

@Composable
private fun AboutPage(vm: MainViewModel, onBack: () -> Unit) {
    val c = WC
    val context = LocalContext.current
    val (versionName, versionCode) = remember {
        try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val name = info.versionName.orEmpty()
            @Suppress("DEPRECATION")
            val code = info.versionCode.toString()
            name to code
        } catch (_: Exception) {
            "13.0.0" to "13"
        }
    }
    PageScaffold("Sobre", onBack) {
        Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) { CoverPlaceholder(Modifier.fillMaxSize()) }
            Spacer(Modifier.height(14.dp))
            Text("WMusic ${appVersionLabel(context)}", color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Reprodutor de músicas local", color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Section("Versão") {
            InfoRow("Versão", versionName)
            InfoRow("Código da versão", versionCode)
            InfoRow("Compilação", BuildConfig.BUILD_DATE)
            InfoRow("Pacote", context.packageName)
            InfoRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            InfoRow("Aparelho", "${Build.MANUFACTURER} ${Build.MODEL}")
        }
        Section("Sua biblioteca") {
            InfoRow("Músicas", "${vm.tracks.size}")
            InfoRow("Playlists", "${vm.playlists.size}")
            InfoRow("Favoritos", "${vm.favorites.size}")
            InfoRow("Modo de pastas", vm.settings.libraryMode.label)
        }
        Section("Recursos") {
            Hint("Player Full com fundo dinâmico · Mini Player personalizável · Whitelist e Blacklist · Letras salvas e pesquisa online · Gestos configuráveis · Backup em JSON · Localizador de arquivos · Equalizador de 10 bandas · Timer de desligamento · Reprodução em segundo plano.")
        }
        Hint("Todos os dados ficam no aparelho. A única conexão com a internet é a pesquisa de letras, e só quando você pede.")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val c = WC
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.muted, fontSize = 13.sp, modifier = Modifier.weight(0.45f))
        Text(value, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.End, modifier = Modifier.weight(0.55f))
    }
}
