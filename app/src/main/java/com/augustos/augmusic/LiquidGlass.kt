@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.augustos.augmusic

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

/*
 * Liquid Glass para o WMusic (Jetpack Compose).
 *
 * Tradução do sistema Liquid Glass do iOS 26 para Compose:
 *   .glassEffect()               -> Modifier.glass()          (vidro translúcido + reflexo + borda de luz + sombra)
 *   .interactive()               -> rememberPress()/GlassCard (escala com mola ao tocar)
 *   GlassEffectContainer         -> GlassBackdrop             (fonte única de conteúdo para o blur)
 *   .buttonStyle(.glass[Prominent]) -> GlassButton(prominent = false/true)
 *   morphing entre elementos     -> indicador deslizante do GlassTabBar
 *
 * Blur real do que está ATRÁS do vidro: Android 12+ (RenderEffect). Em Android 8–11 o vidro fica
 * mais opaco para manter o texto legível (a skill pede contraste garantido sobre o vidro).
 */

internal val GLASS_BLUR_SUPPORTED: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** Altura ocupada pelas barras flutuantes (mini player + tab bar). As listas somam isto ao padding final. */
internal val LocalBottomInset = compositionLocalOf { 0.dp }

// ------------------------------------------------------------------ backdrop compartilhado

/** Camada que grava o conteúdo das telas; o vidro das barras a desenha desfocada por trás de si. */
@Stable
internal class GlassBackdrop(val layer: GraphicsLayer) {
    var origin by mutableStateOf(Offset.Zero)
}

internal val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

@Composable
internal fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/** Marca um conteúdo como "fonte" do blur (as telas rolam por baixo das barras de vidro). */
internal fun Modifier.glassSource(backdrop: GlassBackdrop): Modifier = this
    .onGloballyPositioned { backdrop.origin = it.positionInWindow() }
    .drawWithContentRecord(backdrop.layer)

private fun Modifier.drawWithContentRecord(layer: GraphicsLayer): Modifier =
    this.then(
        Modifier.drawWithContent {
            val w = size.width.roundToInt()
            val h = size.height.roundToInt()
            if (w > 0 && h > 0) {
                layer.record(this, layoutDirection, IntSize(w, h)) { this@drawWithContent.drawContent() }
                drawLayer(layer)
            } else {
                drawContent()
            }
        }
    )

// ------------------------------------------------------------------ materiais

private fun WColors.glassBase(): Color =
    if (dark) lerp(Color(0xFF1C211A), accent, 0.07f) else Color.White

private fun WColors.rimBrush(): Brush =
    if (dark) Brush.linearGradient(listOf(Color.White.copy(0.46f), Color.White.copy(0.07f), Color.White.copy(0.20f)))
    else Brush.linearGradient(listOf(Color.White.copy(0.95f), Color.Black.copy(0.05f), Color.Black.copy(0.10f)))

/** Cor de preenchimento dos painéis (sheets e menus): translúcida, mais opaca onde não há blur. */
internal fun WColors.glassPanelColor(blurBehind: Boolean): Color {
    val a = if (blurBehind) 0.74f else 0.95f
    return if (dark) lerp(Color(0xFF181C16), accent, 0.06f).copy(alpha = a)
    else Color(0xFFF8F9F5).copy(alpha = a)
}

/**
 * O "glassEffect" do WMusic: sombra suave, preenchimento translúcido em degradê, reflexo especular
 * no canto superior esquerdo e uma borda de luz. Com [backdrop] = true desfoca o conteúdo que está atrás
 * (Android 12+). [tint] colore o vidro (botões proeminentes, seleção).
 */
@Composable
internal fun Modifier.glass(
    shape: Shape,
    tint: Color? = null,
    tintAlpha: Float = 0.28f,
    elevation: Dp = 8.dp,
    backdrop: Boolean = false,
    pressed: Boolean = false
): Modifier {
    val c = WC
    val bd = if (backdrop) LocalGlassBackdrop.current else null
    val blurred = bd != null && GLASS_BLUR_SUPPORTED
    val radiusPx = with(LocalDensity.current) { 24.dp.toPx() }
    val blurLayer = if (blurred) rememberGraphicsLayer() else null
    if (blurLayer != null) {
        remember(blurLayer, radiusPx) { blurLayer.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp) }
    }
    var pos by remember { mutableStateOf(Offset.Zero) }
    val dark = c.dark
    val base = c.glassBase()
    val alpha = if (blurred) (if (dark) 0.44f else 0.56f) else (if (dark) 0.80f else 0.86f)
    val shadowColor = Color.Black.copy(alpha = if (dark) 0.45f else 0.16f)
    val rim = c.rimBrush()

    return this
        .then(
            if (elevation > 0.dp) Modifier.shadow(elevation, shape, clip = false, ambientColor = shadowColor, spotColor = shadowColor)
            else Modifier
        )
        .clip(shape)
        .then(if (blurred) Modifier.onGloballyPositioned { pos = it.positionInWindow() } else Modifier)
        .drawBehind {
            if (bd != null && blurLayer != null) {
                val w = size.width.roundToInt()
                val h = size.height.roundToInt()
                if (w > 0 && h > 0) {
                    val dx = pos.x - bd.origin.x
                    val dy = pos.y - bd.origin.y
                    blurLayer.record(this, layoutDirection, IntSize(w, h)) {
                        translate(-dx, -dy) { drawLayer(bd.layer) }
                    }
                    drawLayer(blurLayer)
                }
            }
            drawRect(Brush.verticalGradient(listOf(base.copy(alpha = alpha + 0.06f), base.copy(alpha = alpha - 0.04f))))
            if (tint != null && tintAlpha > 0f) {
                drawRect(Brush.verticalGradient(listOf(tint.copy(alpha = tintAlpha), tint.copy(alpha = tintAlpha * 0.78f))))
            }
            // reflexo especular
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.16f else 0.60f), Color.Transparent),
                    start = Offset.Zero, end = Offset(size.width * 0.75f, size.height)
                )
            )
            if (pressed) drawRect(Color.White.copy(alpha = if (dark) 0.10f else 0.30f))
        }
        .border(1.dp, rim, shape)
}

// ------------------------------------------------------------------ interação

@Stable
internal class PressState(val source: MutableInteractionSource, val pressed: Boolean, val scale: Float)

/** "interactive()": o vidro cede levemente ao toque e volta com uma mola. */
@Composable
internal fun rememberPress(enabled: Boolean = true, pressedScale: Float = 0.96f): PressState {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "glassPress"
    )
    return PressState(source, pressed && enabled, scale)
}

// ------------------------------------------------------------------ fundo ambiente

/** Fundo do app: o degradê original + duas luzes suaves da cor de destaque (dão o que o vidro "refratar"). */
@Composable
internal fun Modifier.liquidBackground(): Modifier {
    val c = WC
    return this
        .background(
            if (c.useGradients) Brush.verticalGradient(listOf(c.gradTop, c.bg, c.bg))
            else Brush.verticalGradient(listOf(c.bg, c.bg))
        )
        .drawBehind {
            if (c.useGradients) {
                val r = size.width * 0.95f
                val a1 = if (c.dark) 0.20f else 0.26f
                val a2 = if (c.dark) 0.11f else 0.16f
                val p1 = Offset(size.width * 0.12f, size.height * 0.10f)
                val p2 = Offset(size.width * 0.95f, size.height * 0.72f)
                drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = a1), Color.Transparent), center = p1, radius = r), radius = r, center = p1)
                drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = a2), Color.Transparent), center = p2, radius = r), radius = r, center = p2)
            }
        }
}

// ------------------------------------------------------------------ cartões, linhas e botões

/** Cartão de vidro. Com [onClick] ganha feedback de toque (escala + brilho). */
@Composable
internal fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color? = null,
    tintAlpha: Float = 0.16f,
    elevation: Dp = 6.dp,
    backdrop: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val press = rememberPress(enabled = onClick != null, pressedScale = 0.975f)
    val interactive = if (onClick != null) {
        Modifier.combinedClickable(
            interactionSource = press.source, indication = null,
            role = Role.Button, onLongClick = onLongClick, onClick = onClick
        )
    } else Modifier
    Column(
        modifier
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale }
            .then(interactive)
            .glass(shape, tint, tintAlpha, elevation, backdrop, press.pressed)
            .padding(contentPadding),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

/** Linha de lista: sem fundo em repouso; vidro com tom da cor de destaque quando [selected]; realce ao tocar. */
@Composable
internal fun GlassRowItem(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val c = WC
    val press = rememberPress(pressedScale = 0.985f)
    Row(
        modifier
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale }
            .combinedClickable(
                interactionSource = press.source, indication = null,
                role = Role.Button, onLongClick = onLongClick, onClick = onClick
            )
            .then(
                if (selected) Modifier.glass(shape, tint = c.accent, tintAlpha = 0.16f, elevation = 0.dp, pressed = press.pressed)
                else Modifier.clip(shape).background(if (press.pressed) c.text.copy(alpha = 0.07f) else Color.Transparent)
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/** Botão de vidro. [prominent] = vidro colorido (glassProminent); caso contrário vidro neutro (glass). */
@Composable
internal fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    prominent: Boolean = true,
    enabled: Boolean = true,
    tone: Color = WC.accent,
    content: @Composable RowScope.() -> Unit
) {
    val c = WC
    val press = rememberPress(enabled)
    val contentColor = if (prominent) onColorFor(tone) else c.text
    Row(
        modifier
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale; alpha = if (enabled) 1f else 0.45f }
            .clickable(interactionSource = press.source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .glass(
                RoundedCornerShape(50),
                tint = if (prominent) tone else null,
                tintAlpha = if (prominent) 0.92f else 0f,
                elevation = if (prominent) 10.dp else 4.dp,
                pressed = press.pressed
            )
            .heightIn(min = 46.dp)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)) { content() }
        }
    }
}

/** Botão de texto discreto (substitui o TextButton): sem fundo, realce suave e mola ao tocar. */
@Composable
internal fun GlassTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
    content: @Composable RowScope.() -> Unit
) {
    val c = WC
    val press = rememberPress(enabled, 0.97f)
    Row(
        modifier
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale; alpha = if (enabled) 1f else 0.4f }
            .clip(RoundedCornerShape(50))
            .background(if (press.pressed) c.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(interactionSource = press.source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides c.accentFg) {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)) { content() }
        }
    }
}

/** Botão circular só com ícone (cabeçalhos, mini player, ferramentas). */
@Composable
internal fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    iconSize: Dp = size * 0.5f,
    tint: Color = WC.text,
    prominent: Boolean = false,
    plain: Boolean = false,
    enabled: Boolean = true
) {
    val c = WC
    val press = rememberPress(enabled, 0.9f)
    Box(
        modifier
            .size(size)
            .graphicsLayer { scaleX = press.scale; scaleY = press.scale }
            .clickable(interactionSource = press.source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .then(
                if (plain) Modifier.clip(CircleShape).background(if (press.pressed) c.text.copy(alpha = 0.10f) else Color.Transparent)
                else Modifier.glass(
                    CircleShape,
                    tint = if (prominent) c.accent else null,
                    tintAlpha = if (prominent) 0.94f else 0f,
                    elevation = if (prominent) 8.dp else 3.dp,
                    pressed = press.pressed
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = if (prominent) c.onAccent else tint, modifier = Modifier.size(iconSize))
    }
}

// ------------------------------------------------------------------ navegação

internal class GlassTab(val id: String, val icon: ImageVector, val label: String)

/**
 * Tab bar flutuante em cápsula de vidro. A seleção é uma "gota" de vidro que desliza entre os itens
 * (o morphing da skill), com mola.
 */
@Composable
internal fun GlassTabBar(
    tabs: List<GlassTab>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = WC
    val selectedIndex = tabs.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    BoxWithConstraints(
        modifier.fillMaxWidth()
            .glass(RoundedCornerShape(32.dp), elevation = 16.dp, backdrop = true)
            .padding(6.dp)
    ) {
        val itemW = maxWidth / tabs.size
        val indicatorX by animateDpAsState(
            targetValue = itemW * selectedIndex,
            animationSpec = spring<Dp>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = "tabIndicator"
        )
        Box(
            Modifier.offset(x = indicatorX).width(itemW).height(56.dp)
                .glass(RoundedCornerShape(26.dp), tint = c.accent, tintAlpha = 0.24f, elevation = 0.dp)
        )
        Row(Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { i, tab ->
                val selected = i == selectedIndex
                val color by animateColorAsState(if (selected) c.accentFg else c.muted, tween(220), label = "tabTint")
                val source = remember { MutableInteractionSource() }
                Column(
                    Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(26.dp))
                        .selectable(
                            selected = selected, interactionSource = source, indication = null,
                            role = Role.Tab, onClick = { onSelect(tab.id) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tab.icon, null, tint = color, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        tab.label, color = color, fontSize = 11.sp, maxLines = 1,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ diálogos e painéis

/** AlertDialog em vidro: mesma assinatura do AlertDialog do Material, visual Liquid Glass. */
@Composable
internal fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    val c = WC
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val width = if (properties.usePlatformDefaultWidth) Modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
        else Modifier.fillMaxWidth()
        Column(
            modifier.then(width)
                .glass(RoundedCornerShape(32.dp), elevation = 28.dp)
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 14.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides c.text) {
                if (title != null) {
                    ProvideTextStyle(TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.text)) {
                        Box(Modifier.padding(bottom = 14.dp)) { title() }
                    }
                }
                if (text != null) {
                    ProvideTextStyle(TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = c.text)) {
                        Box(Modifier.weight(1f, fill = false)) { text() }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dismissButton?.invoke()
                confirmButton()
            }
        }
    }
}

@Composable
internal fun GlassGrabber() {
    val c = WC
    Box(
        Modifier.padding(top = 10.dp, bottom = 12.dp).width(40.dp).height(5.dp)
            .clip(CircleShape).background(c.text.copy(alpha = 0.30f))
    )
}

/**
 * ModalBottomSheet em vidro. O conteúdo do app atrás é desfocado pela AppRoot enquanto o painel está aberto,
 * então o painel pode ser translúcido sem perder legibilidade.
 */
@Composable
internal fun GlassModalSheet(
    onDismiss: () -> Unit,
    skipPartiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = WC
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        containerColor = c.glassPanelColor(GLASS_BLUR_SUPPORTED),
        contentColor = c.text,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = if (c.dark) 0.42f else 0.26f),
        dragHandle = { GlassGrabber() }
    ) {
        Column(
            Modifier.fillMaxWidth().background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = if (c.dark) 0.07f else 0.45f), Color.Transparent))
            ),
            content = content
        )
    }
}

internal val GlassMenuShape = RoundedCornerShape(22.dp)

@Composable
internal fun glassMenuContainer(): Color = WC.glassPanelColor(false)

@Composable
internal fun glassMenuBorder(): BorderStroke {
    val c = WC
    return BorderStroke(1.dp, if (c.dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.08f))
}

// ------------------------------------------------------------------ campos, interruptores, sliders

@Composable
internal fun glassTextFieldColors() = WC.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.text, unfocusedTextColor = c.text,
        focusedContainerColor = if (c.dark) Color.White.copy(alpha = 0.07f) else Color.White.copy(alpha = 0.65f),
        unfocusedContainerColor = if (c.dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.50f),
        focusedBorderColor = c.accent, unfocusedBorderColor = c.outline,
        cursorColor = c.accent,
        focusedLabelColor = c.accentFg, unfocusedLabelColor = c.muted,
        focusedPlaceholderColor = c.muted, unfocusedPlaceholderColor = c.muted
    )
}

@Composable
internal fun glassSwitchColors() = WC.let { c ->
    SwitchDefaults.colors(
        checkedThumbColor = Color.White, checkedTrackColor = c.accent, checkedBorderColor = Color.Transparent,
        uncheckedThumbColor = if (c.dark) Color.White.copy(alpha = 0.92f) else Color.White,
        uncheckedTrackColor = if (c.dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.14f),
        uncheckedBorderColor = Color.Transparent
    )
}

@Composable
internal fun glassSliderColors() = WC.let { c ->
    SliderDefaults.colors(
        thumbColor = if (c.dark) Color.White else c.accentFg, activeTrackColor = c.accent,
        inactiveTrackColor = if (c.dark) Color.White.copy(alpha = 0.18f) else Color.Black.copy(alpha = 0.14f)
    )
}
