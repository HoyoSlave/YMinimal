package io.ymusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.Volatile

object VideoOverlayNative {
    var armed by mutableStateOf(false)

    var visible by mutableStateOf(true)

    var scrubbing by mutableStateOf(false)

    var menusOpen by mutableIntStateOf(0)
        internal set

    var showGen by mutableIntStateOf(0)
        internal set

    val showing: Boolean
        get() = armed && visible

    @Volatile
    var onOverlayPress: (() -> Unit)? = null

    fun poke() {
        onOverlayPress?.invoke()
        if (!armed) return
        if (visible) {
            visible = false
        } else {
            show()
        }
    }

    fun show() {
        visible = true
        showGen++
    }
}

@Composable
internal fun HoldVideoOverlay(open: Boolean) {
    if (!open) return
    DisposableEffect(Unit) {
        VideoOverlayNative.menusOpen++
        onDispose { VideoOverlayNative.menusOpen-- }
    }
}
