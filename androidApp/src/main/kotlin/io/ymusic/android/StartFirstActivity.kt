package io.ymusic.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

internal fun Context.startFirstActivity(vararg intents: Intent): Boolean = intents.any { intent ->
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
