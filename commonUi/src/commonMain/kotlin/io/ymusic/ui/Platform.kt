package io.ymusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal expect val platformUsesTouch: Boolean

internal expect val platformIsTelevision: Boolean

internal expect val desktopLayoutHole: Boolean

@Composable
internal expect fun platformDynamicPalette(dark: Boolean): DesignPalette?

@Composable
internal expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

@Composable
internal expect fun rememberTouchExplorationEnabled(): Boolean

@Composable
internal expect fun SyncSystemBars(dark: Boolean)

@Composable
internal expect fun SyncImmersiveSystemBars(immersive: Boolean)

@Composable
internal expect fun SyncSensorRotation(locked: Boolean)

internal expect val rotationLockSupported: Boolean

@Composable
internal expect fun KeepScreenOn(enabled: Boolean)

@Composable
internal expect fun Modifier.keepBelowStatusBar(): Modifier

@Composable
internal expect fun Modifier.drawerEdgeGestureExclusion(enabled: Boolean): Modifier

expect fun Modifier.a11yActionLabel(label: String?): Modifier

internal expect fun Modifier.a11ySlider(
    label: String?,
    value: String,
    fraction: Float,
    enabled: Boolean,
    onSet: (Float) -> Unit,
): Modifier
