package io.ymusic.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.ymusic.mediastore.model.MediaStoreBucket
import io.ymusic.settings.AppSettings
import io.ymusic.settings.flow
import io.ymusic.settings.put
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.download_location_cannot_create
import io.ymusic.ui.resources.download_location_missing
import io.ymusic.ui.resources.download_location_not_set
import io.ymusic.ui.resources.download_location_prompt_message
import io.ymusic.ui.resources.download_location_prompt_not_now
import io.ymusic.ui.resources.download_location_prompt_set_default
import io.ymusic.ui.resources.download_location_read_only
import io.ymusic.ui.resources.menu_download_location
import kmpcommon.uri.Uri
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DownloadSettingsRows(graph: AppGraph) {
    if (graph.mediaStore != null) DownloadLocationRow(graph)
    QuickDownloadRow(graph)
}

@Composable
private fun DownloadLocationRow(graph: AppGraph) {
    if (graph.mediaStore == null) return
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    val scans = LocalScanController.current
    val name = rememberDownloadLocationName(graph)

    // No dialog: tapping goes straight to the OS folder picker (adds the folder to the Library and
    // sets it as the location). Existing folders are re-designated from the Library tab's menu instead.
    SettingRow(
        title = stringResource(Res.string.menu_download_location),
        value = name ?: stringResource(Res.string.download_location_not_set),
    ) {
        scope.launch { pickDownloadFolder(graph, snackbar, scans) }
    }
}

@Composable
internal fun rememberDownloadLocationName(graph: AppGraph): String? {
    val mediaStore = graph.mediaStore ?: return null
    val location by remember(graph) { graph.settings.flow(AppSettings.downloadLocation) }.collectAsState()
    var name by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(location, mediaStore) {
        name = if (location.isBlank()) {
            null
        } else {
            mediaStore.findBucketByUri(Uri.parse(location))
                ?.takeIf { !it.excluded }
                ?.let { it.aliasName?.takeIf(String::isNotBlank) ?: it.name }
        }
    }
    return name
}

internal suspend fun pickDownloadFolder(graph: AppGraph, snackbar: (String) -> Unit, scans: ScanController?): Boolean {
    val pick = graph.libraryFolderPicker ?: return false
    val uri = pick() ?: return false // canceled
    if (!assignDownloadFolder(graph, uri, snackbar)) return false
    val rescan = graph.rescanLibrary
    if (rescan != null && scans != null) scans.start { onProgress -> rescan(uri, onProgress) }
    return true
}

enum class FolderWriteProblem { NoPermission, Missing, CannotCreateFiles }

internal suspend fun assignDownloadFolder(graph: AppGraph, uri: String, snackbar: (String) -> Unit): Boolean {
    val problem = graph.probeDownloadFolder(uri)
    if (problem != null) {
        snackbar(getString(problem.message()))
        return false
    }
    graph.settings.put(AppSettings.downloadLocation, uri)
    return true
}

private fun FolderWriteProblem.message(): StringResource = when (this) {
    FolderWriteProblem.NoPermission -> Res.string.download_location_read_only
    FolderWriteProblem.Missing -> Res.string.download_location_missing
    FolderWriteProblem.CannotCreateFiles -> Res.string.download_location_cannot_create
}

internal suspend fun hasUsableDownloadLocation(graph: AppGraph): Boolean {
    val store = graph.mediaStore ?: return false
    val loc = graph.settings.flow(AppSettings.downloadLocation).value
    if (loc.isBlank()) return false
    val bucket = store.findBucketByUri(Uri.parse(loc)) ?: return false
    return !bucket.excluded && graph.canWriteFolder(loc)
}

@Composable
internal fun DefaultDownloadLocationPrompt(graph: AppGraph) {
    val mediaStore = graph.mediaStore ?: return
    if (graph.downloads == null) return
    val scope = rememberCoroutineScope()
    val location by remember(graph) { graph.settings.flow(AppSettings.downloadLocation) }.collectAsState()

    var candidate by remember { mutableStateOf<MediaStoreBucket?>(null) }
    LaunchedEffect(mediaStore) {
        var previous = mediaStore.childBuckets(null).size
        mediaStore.bucketsChanges().collect {
            val roots = mediaStore.childBuckets(null)
            val locationUnset = graph.settings.flow(AppSettings.downloadLocation).value.isBlank()
            // Only offer a folder that passes the test write; the user didn't ask, so a failure stays silent.
            if (previous == 0 && roots.isNotEmpty() && locationUnset) {
                roots.first().takeIf { graph.probeDownloadFolder(it.uri.toString()) == null }?.let { candidate = it }
            }
            previous = roots.size
        }
    }

    val bucket = candidate
    if (bucket != null && location.isBlank()) {
        ElevatedBarDialog(
            title = stringResource(Res.string.menu_download_location),
            onDismiss = { candidate = null },
            canScrollBackward = false,
            canScrollForward = false,
            buttons = {
                DialogSecondaryButton(stringResource(Res.string.download_location_prompt_not_now)) { candidate = null }
                TextButton(onClick = {
                    scope.launch { graph.settings.put(AppSettings.downloadLocation, bucket.uri.toString()) }
                    candidate = null
                }) { Text(stringResource(Res.string.download_location_prompt_set_default)) }
            },
        ) {
            Text(
                stringResource(Res.string.download_location_prompt_message, bucket.name),
                style = TextStyle(fontSize = 14.sp, color = DesignColors.Ink),
                modifier = Modifier.padding(horizontal = DesignSpacing.DialogRowHorizontal, vertical = DesignSpacing.Space16),
            )
        }
    }
}

@Composable
internal fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = DesignSpacing.SettingsRowHorizontal, vertical = DesignSpacing.SettingsRowVertical),
        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space16),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.Ink),
            modifier = Modifier.weight(1f),
        )
        Text(value, style = TextStyle(fontSize = 13.sp, color = DesignColors.InkSecondary))
    }
}
