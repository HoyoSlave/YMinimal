package io.ymusic.ui

import io.ymusic.settings.AppSettings
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.flow

fun SettingsStore.downloadLocationFolder(): String? =
    flow(AppSettings.downloadLocation).value.takeIf { it.isNotBlank() }
