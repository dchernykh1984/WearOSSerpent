package com.dchernykh.serpent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material3.Text
import com.dchernykh.serpent.layout.BOARD_EDGE
import com.dchernykh.serpent.layout.Board
import com.dchernykh.serpent.layout.SCREEN_PADDING
import com.dchernykh.serpent.layout.centeredBox
import kotlin.math.roundToInt
import com.dchernykh.serpent.layout.Box as LayoutBox

/**
 * The menus: a vertical stack of lines and buttons, centred on the board under a
 * panel dark enough to read over a half-drawn snake.
 *
 * Everything is sized from the board rather than in fixed dp, so the same stack
 * fills the same proportion of a 384px watch and a 466px one.
 */
sealed interface MenuItem {
    val height: Int

    data class Line(
        override val height: Int,
        val color: Color,
        val text: String,
    ) : MenuItem

    data class Action(
        override val height: Int,
        val text: String,
        val onClick: () -> Unit,
    ) : MenuItem

    data class Gap(
        override val height: Int,
    ) : MenuItem
}

/** The type scale and spacing of a menu, all derived from the board. */
class MenuMetrics(
    board: Board,
) {
    val big = (board.size * 0.13f).roundToInt()
    val row = (board.size * 0.1f).roundToInt()
    val small = (board.size * 0.085f).roundToInt()
    val button = (board.size * 0.16f).roundToInt()
    val gap = (board.size * 0.04f).roundToInt()
    val maxWidth = board.size * 0.86f
}

@Composable
fun MenuOverlay(
    screenSize: Int,
    board: Board,
    metrics: MenuMetrics,
    items: List<MenuItem>,
) {
    val stackHeight = items.sumOf { it.height }
    val top = board.y + ((board.size - stackHeight) / 2f).roundToInt()
    val panelTop = maxOf(board.y, top - metrics.gap)
    val panelHeight = minOf(board.size, stackHeight + 2 * metrics.gap)

    Box(
        modifier =
            Modifier
                .absoluteBox(LayoutBox(board.x, panelTop, board.size, panelHeight))
                // In pixels, like every other measurement here. As dp it would be
                // scaled by the watch's density and come out twice as round as the
                // frame it sits inside.
                .clip(RoundedCornerShape(with(LocalDensity.current) { (BOARD_EDGE * 4).toDp() }))
                // Not opaque: seeing the wreck of the game under the panel is how
                // you find out what you ran into.
                .background(ColorBackground.copy(alpha = 210f / 255f)),
    )

    var y = top
    for (item in items) {
        val box = centeredBox(screenSize, y, item.height, metrics.maxWidth, SCREEN_PADDING)
        when (item) {
            is MenuItem.Gap -> Unit
            is MenuItem.Line ->
                MenuLine(box, item.color, item.text)
            is MenuItem.Action ->
                MenuButton(box, item.text, item.onClick)
        }
        y += item.height
    }
}

@Composable
private fun MenuLine(
    box: LayoutBox,
    color: Color,
    text: String,
) {
    Box(modifier = Modifier.absoluteBox(box), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = color,
            fontSize = with(LocalDensity.current) { (box.h * 0.76f).toSp() },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MenuButton(
    box: LayoutBox,
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier =
            Modifier
                .absoluteBox(box)
                .clip(RoundedCornerShape(percent = 50))
                .background(if (pressed) ColorButtonPressed else ColorButton)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = ColorText,
            fontSize = with(LocalDensity.current) { (box.h * 0.46f).toSp() },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
