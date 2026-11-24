package com.music.zenith.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.zenith.ui.components.backdrop.Backdrop
import com.music.zenith.ui.icons.SfIcons
import com.music.zenith.ui.screens.ChartCountryOption
import com.music.zenith.ui.screens.chartCountryOptions
import dev.chrisbanes.haze.HazeState

/** The popup is capped so a long country list scrolls inside it instead of filling the screen. */
private val COUNTRY_LIST_MAX_HEIGHT = 380.dp

private class CountryButtonBounds {
    var rect: Rect = Rect.Zero
}

/**
 * The Charts country opener, Apple Music style: a glass capsule with the country
 * name and the up/down chevrons that mark a menu. It swells under a finger and
 * springs back ([glassJelly]), and reports its window bounds on tap so the popup
 * can grow out of it.
 */
@Composable
fun ChartCountryButton(
    label: String,
    onClick: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = CircleShape
    val glass = LocalLiquidGlassEnabled.current && isGlassSupported()
    val content = if (glass) glassContentColor() else MaterialTheme.colorScheme.onSurface
    val bounds = remember { CountryButtonBounds() }
    Row(
        modifier = modifier
            // Before the jelly layer, so the reported bounds are the resting ones.
            .onGloballyPositioned { bounds.rect = it.boundsInWindow() }
            .glassJelly(GLASS_JELLY_PRESS_WIDE)
            .clip(shape)
            .then(
                if (glass) {
                    Modifier.liquidGlass(shape, refract = false)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), shape)
                },
            )
            .border(GLASS_EDGE_WIDTH, GLASS_EDGE_COLOR, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = { onClick(bounds.rect) },
            )
            .height(42.dp)
            .padding(start = 20.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = content,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.width(10.dp))
        UpDownChevrons(color = content.copy(alpha = 0.72f))
    }
}

/** The stacked up/down chevrons of an iOS menu opener, drawn rather than loaded. */
@Composable
private fun UpDownChevrons(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 10.dp, height = 16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val up = Path().apply {
            moveTo(0f, h * 0.34f)
            lineTo(w / 2f, h * 0.10f)
            lineTo(w, h * 0.34f)
        }
        val down = Path().apply {
            moveTo(0f, h * 0.66f)
            lineTo(w / 2f, h * 0.90f)
            lineTo(w, h * 0.66f)
        }
        drawPath(up, color, style = stroke)
        drawPath(down, color, style = stroke)
    }
}

/**
 * The country popup: a frosted panel that grows out of [anchor] with the macOS
 * morph, plays jelly under a dragging finger and folds back into the button.
 * Drawn at the screen root by the host, like every other page menu.
 */
@Composable
fun ChartCountryMenu(
    open: Boolean,
    anchor: Rect?,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    backdrop: Backdrop?,
    hazeState: HazeState?,
) {
    val options = remember { chartCountryOptions() }
    IosMenuPopover(
        item = if (open) Unit else null,
        anchor = anchor,
        onDismiss = onDismiss,
        backdrop = backdrop,
        hazeState = hazeState,
        placement = IosMenuPlacement.BelowStart,
        glassRim = true,
        frostAlpha = 0.55f,
    ) {
        ChartCountryMenuBody(options = options, selected = selected, onPick = onPick)
    }
}

@Composable
private fun ChartCountryMenuBody(
    options: List<ChartCountryOption>,
    selected: String,
    onPick: (String) -> Unit,
) {
    val colors = iosMenuColors()
    // Opens with the chosen country in view rather than at the top of the list.
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (options.indexOfFirst { it.code == selected } - 1).coerceAtLeast(0),
    )
    Text(
        text = "Select a country",
        color = colors.secondaryLabel,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 6.dp),
    )
    LazyColumn(
        state = listState,
        modifier = Modifier.heightIn(max = COUNTRY_LIST_MAX_HEIGHT),
    ) {
        items(options, key = { it.code }) { option ->
            IosMenuRow(
                label = option.name,
                icon = if (option.code == selected) SfIcons.Check else null,
                onClick = { onPick(option.code) },
                compact = true,
            )
            if (option.code == "ZZ") IosMenuSeparator(colors)
        }
    }
}