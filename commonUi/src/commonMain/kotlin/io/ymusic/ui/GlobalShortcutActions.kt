package io.ymusic.ui

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.concurrent.Volatile

object GlobalShortcutActions {
    @Volatile var openSearch: () -> Unit = {}

    @Volatile var toggleShortcutsHelp: () -> Unit = {}

    @Volatile var toggleNavDrawer: () -> Unit = {}

    @Volatile var closeNavDrawerIfOpen: () -> Boolean = { false }

    @Volatile var back: () -> Boolean = { false }

    @Volatile var showMessage: (String) -> Unit = {}

    // Cmd/Ctrl + 1..9 → the top-most pushed tab handler wins. A plain save/restore var is racy
    // across section transitions (the entering screen can capture the leaving screen's handler
    // as its "prior" before the leaving dispose fires, then dispose wipes the new one). Keep a
    // proper LIFO stack so order of compose/dispose doesn't matter.
    private val tabHandlers = mutableListOf<(Int) -> Boolean>()
    private val tabLock = SynchronizedObject()
    val selectTabByIndex: (Int) -> Boolean = { i ->
        synchronized(tabLock) { tabHandlers.lastOrNull() }?.invoke(i) ?: false
    }

    fun pushTabHandler(handler: (Int) -> Boolean): () -> Unit {
        synchronized(tabLock) { tabHandlers.add(handler) }
        return { synchronized(tabLock) { tabHandlers.remove(handler) } }
    }
}
