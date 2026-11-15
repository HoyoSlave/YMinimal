package io.ymusic.android

import android.content.Context
import io.ymusic.settings.BoolSetting
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.flow
import io.ymusic.settings.put
import kmpcommon.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

internal class StartupCleanups(
    private val context: Context,
    private val settings: SettingsStore,
    private val logger: Logger?,
) {
    private class Cleanup(val done: BoolSetting, val run: () -> Unit)

    private val cleanups = listOf(
        // Builds up to 4.0.10 downloaded the update APK into cacheDir/update; it moved to filesDir/update.
        Cleanup(BoolSetting("clean_up_old_update_dir", false)) { File(context.cacheDir, "update").deleteRecursively() },
        // Builds up to 4.0.12 kept a rotating text log in filesDir/logs; the log moved to logs.db.
        Cleanup(BoolSetting("clean_up_text_log_dir", false)) { File(context.filesDir, "logs").deleteRecursively() },
    )

    suspend fun run() {
        delay(START_DELAY_MS)
        for (cleanup in cleanups) {
            if (settings.flow(cleanup.done).value) continue
            try {
                withContext(Dispatchers.IO) { cleanup.run() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger?.fork("startup-cleanup", mapOf("key" to cleanup.done.key))
                    ?.log(Logger.Level.WARN, "cleanup failed; retrying next launch", e)
                continue
            }
            settings.put(cleanup.done, true)
        }
    }

    private companion object {
        const val START_DELAY_MS = 10_000L
    }
}
