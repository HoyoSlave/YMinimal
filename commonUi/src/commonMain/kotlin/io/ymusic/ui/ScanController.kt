package io.ymusic.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.library_scan_failed
import io.ymusic.ui.resources.library_scan_ffmpeg_missing
import io.ymusic.ui.resources.library_scan_no_folders
import io.ymusic.ui.resources.library_scan_summary
import io.ymusic.ui.resources.library_scan_summary_failed
import kmpcommon.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import projectcommon.error.DISK_FULL_CODE
import projectcommon.error.errorCode
import projectcommon.log.captureUnlessEnvironmental

internal class ScanController(private val scope: CoroutineScope, private val log: Logger? = null) {
    var running by mutableStateOf(false)
        private set
    var scanned by mutableStateOf<Int?>(null)
        private set

    private val _summaries = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val summaries: SharedFlow<String> = _summaries.asSharedFlow()

    private val queued = mutableListOf<ScanAction>()

    fun start(action: ScanAction) {
        if (running) {
            queued += action
            return
        }
        running = true
        scope.launch {
            try {
                var next: ScanAction? = action
                while (next != null) {
                    run(next)
                    next = if (queued.isEmpty()) null else queued.removeAt(0)
                }
            } finally {
                queued.clear()
                running = false
                scanned = null
            }
        }
    }

    private suspend fun run(action: ScanAction) {
        scanned = null
        try {
            action { count -> scanned = count }?.let { _summaries.tryEmit(summaryOf(it)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log?.captureUnlessEnvironmental(e, "library scan failed")
            val code = e.errorCode() ?: "E50012"
            _summaries.tryEmit(if (code == DISK_FULL_CODE) errorText(code).load() else getString(Res.string.library_scan_failed, code))
        }
    }
}

private suspend fun summaryOf(result: ScanResult): String = when (result) {
    is ScanResult.Done -> if (result.failed > 0) {
        getString(Res.string.library_scan_summary_failed, result.scanned, result.added, result.updated, result.removed, result.failed)
    } else {
        getString(Res.string.library_scan_summary, result.scanned, result.added, result.updated, result.removed)
    }
    ScanResult.NoFolders -> getString(Res.string.library_scan_no_folders)
    ScanResult.ExtractorMissing -> getString(Res.string.library_scan_ffmpeg_missing)
}

internal val LocalScanController = compositionLocalOf<ScanController?> { null }
