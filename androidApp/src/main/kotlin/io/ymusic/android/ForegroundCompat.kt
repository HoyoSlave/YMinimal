package io.ymusic.android

import android.app.Service
import androidx.core.app.ServiceCompat
import kmpcommon.log.Logger

internal fun Service.stopForegroundSafely(flags: Int, log: Logger?) {
    try {
        ServiceCompat.stopForeground(this, flags)
    } catch (e: NullPointerException) {
        log?.log(Logger.Level.WARN, "stopForeground failed inside system_server", e)
    }
}
