package io.ymusic.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

internal val ChipPaddingHorizontal = 14.dp
internal val ChipPaddingVertical = 7.dp
internal val ChipIconPaddingStart = 10.dp
private val ChipTapOverhang = 4.dp

internal fun Modifier.chipPadding(leadingIcon: Boolean = false): Modifier = padding(
    start = if (leadingIcon) ChipIconPaddingStart else ChipPaddingHorizontal,
    end = ChipPaddingHorizontal,
    top = ChipPaddingVertical,
    bottom = ChipPaddingVertical,
)

@Composable
internal fun ChipTapArea(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selected: Boolean? = null,
    content: @Composable (indication: Modifier) -> Unit,
) {
    if (onClick == null) {
        Box(modifier, propagateMinConstraints = true) { content(Modifier) }
        return
    }
    Box(modifier.bleed(0.dp, ChipTapOverhang), propagateMinConstraints = true) {
        Box(
            Modifier
                .clickable(interactionSource, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
                .then(if (selected != null) Modifier.semantics { this.selected = selected } else Modifier)
                .padding(vertical = ChipTapOverhang),
            propagateMinConstraints = true,
        ) {
            content(Modifier.indication(interactionSource, LocalIndication.current))
        }
    }
}

internal fun Modifier.bleed(horizontal: Dp, vertical: Dp): Modifier = layout { measurable, constraints ->
    val h = horizontal.roundToPx()
    val v = vertical.roundToPx()
    val placeable = measurable.measure(constraints.offset(horizontal = 2 * h, vertical = 2 * v))
    layout(placeable.width - 2 * h, placeable.height - 2 * v) { placeable.place(-h, -v) }
}
