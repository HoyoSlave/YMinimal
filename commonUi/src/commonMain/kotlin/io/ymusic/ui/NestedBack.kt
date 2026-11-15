package io.ymusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
internal class NestedBackStack {
    private val entries = mutableStateListOf<() -> Unit>()

    val active: Boolean get() = entries.isNotEmpty()

    fun back(): Boolean {
        val step = entries.lastOrNull() ?: return false
        step()
        return true
    }

    fun add(step: () -> Unit) {
        entries.add(step)
    }

    fun remove(step: () -> Unit) {
        entries.remove(step)
    }
}

internal val LocalNestedBack = staticCompositionLocalOf<NestedBackStack?> { null }

@Composable
internal fun NestedBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val stack = LocalNestedBack.current ?: return
    val latest by rememberUpdatedState(onBack)
    DisposableEffect(stack, enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val step: () -> Unit = { latest() }
        stack.add(step)
        onDispose { stack.remove(step) }
    }
}
