package io.ymusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ElevatedBarDialog(
    title: String,
    onDismiss: () -> Unit,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    buttons: @Composable RowScope.() -> Unit,
    maxWidth: Dp = 560.dp,
    titleAction: (@Composable () -> Unit)? = null,
    dismissOnClickOutside: Boolean = true,
    keyboardNav: Boolean = false,
    body: @Composable () -> Unit,
) {
    val wide = maxWidth > 560.dp
    val properties = if (wide) {
        wideDialogProperties(dismissOnClickOutside)
    } else {
        DialogProperties(usePlatformDefaultWidth = true, dismissOnClickOutside = dismissOnClickOutside)
    }
    Dialog(onDismissRequest = onDismiss, properties = properties) {
        BoxWithConstraints(if (wide) Modifier.safeDrawingPadding() else Modifier) {
            val windowHeight = this.maxHeight
            val bodyMax = if (windowHeight.value.isFinite() && windowHeight > 0.dp) {
                (windowHeight * 0.85f - 120.dp).coerceAtLeast(240.dp)
            } else {
                420.dp
            }
            val focusManager = LocalFocusManager.current
            val titleFocus = remember { FocusRequester() }
            var titleHadFocus by remember { mutableStateOf(false) }
            var titleFocusable by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { titleFocus.requestFocus() }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = DesignColors.SurfaceElevated,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .then(if (wide) Modifier.padding(horizontal = 24.dp) else Modifier)
                    .widthIn(min = 280.dp, max = maxWidth)
                    .then(if (keyboardNav) Modifier.onKeyEvent { arrowMovesFocus(it, focusManager) } else Modifier),
            ) {
                Column {
                    val topDividerAlpha by animateFloatAsState(if (canScrollBackward) 1f else 0f)
                    Column(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 24.dp,
                                    end = if (titleAction != null) 16.dp else 24.dp,
                                    top = 22.dp,
                                    bottom = 14.dp,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                title,
                                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(titleFocus)
                                    .onFocusChanged {
                                        if (it.isFocused) titleHadFocus = true else if (titleHadFocus) titleFocusable = false
                                    }
                                    .focusProperties { canFocus = titleFocusable }
                                    .focusable(),
                            )
                            titleAction?.invoke()
                        }
                        HorizontalDivider(color = DesignColors.Divider, modifier = Modifier.alpha(topDividerAlpha))
                    }
                    Box(Modifier.weight(1f, fill = false).heightIn(max = bodyMax)) { body() }
                    val bottomDividerAlpha by animateFloatAsState(if (canScrollForward) 1f else 0f)
                    Column(Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = DesignColors.Divider, modifier = Modifier.alpha(bottomDividerAlpha))
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            content = buttons,
                        )
                    }
                }
            }
        }
    }
}

private fun arrowMovesFocus(event: KeyEvent, focusManager: FocusManager): Boolean {
    if (event.type != KeyEventType.KeyDown || event.hasCommandModifier) return false
    return when (event.key) {
        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Next)
        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Previous)
        else -> false
    }
}

@Composable
fun DialogSecondaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = DesignColors.InkMuted),
    ) { Text(text) }
}

internal expect fun wideDialogProperties(dismissOnClickOutside: Boolean): DialogProperties
