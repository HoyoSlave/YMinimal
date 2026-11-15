package io.ymusic.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

internal enum class ListOrientation { Vertical, Horizontal, Both }

internal val KeyEvent.hasCommandModifier: Boolean get() = isCtrlPressed || isMetaPressed || isAltPressed

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@ReadOnlyComposable
internal fun Modifier.listKeyboardNavigation(
    orientation: ListOrientation = ListOrientation.Vertical,
): Modifier {
    val focusManager = LocalFocusManager.current
    val horizontal = orientation == ListOrientation.Horizontal || orientation == ListOrientation.Both
    val vertical = orientation == ListOrientation.Vertical || orientation == ListOrientation.Both
    return focusGroup().onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.hasCommandModifier) return@onKeyEvent false
        val dir = when (event.key) {
            Key.DirectionDown -> if (vertical) FocusDirection.Down else return@onKeyEvent false
            Key.DirectionUp -> if (vertical) FocusDirection.Up else return@onKeyEvent false
            Key.DirectionRight -> if (horizontal) FocusDirection.Right else return@onKeyEvent false
            Key.DirectionLeft -> if (horizontal) FocusDirection.Left else return@onKeyEvent false
            else -> return@onKeyEvent false
        }
        focusManager.moveFocus(dir)
    }
}

@Composable
internal fun Modifier.tvTextFieldDpad(singleLine: Boolean): Modifier {
    if (!platformIsTelevision) return this
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    return onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown || event.hasCommandModifier) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionUp -> singleLine && focusManager.moveFocus(FocusDirection.Up)
            Key.DirectionDown -> singleLine && focusManager.moveFocus(FocusDirection.Down)
            Key.DirectionCenter -> {
                keyboard?.show()
                keyboard != null
            }
            else -> false
        }
    }
}
