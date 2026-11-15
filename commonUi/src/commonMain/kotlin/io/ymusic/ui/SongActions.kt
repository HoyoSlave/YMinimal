package io.ymusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.mediastore.model.MediaStoreItem
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.link_copied
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

internal enum class LibraryGroupKind { Artist, Album }

internal class LibraryGroupReveal(val kind: LibraryGroupKind, val name: String)

internal val LocalLibraryGroupReveal = compositionLocalOf<MutableState<LibraryGroupReveal?>?> { null }

internal val LocalGoToLibraryGroup = compositionLocalOf<((LibraryGroupKind, MediaStoreItem) -> Unit)?> { null }

internal val LocalShareYoutube = compositionLocalOf<((kind: String, id: String) -> Unit)?> { null }

internal val LocalSongActions = compositionLocalOf<SongActions?> { null }

internal fun youtubeShareLink(kind: String, id: String): String? {
    if (kind == YtFeedItem.KIND_PLAYLIST && (id.startsWith("MPRE") || id.startsWith("FE"))) {
        return "https://music.youtube.com/browse/$id"
    }
    return when (kind) {
        YtFeedItem.KIND_VIDEO -> "watch?v=$id"
        YtFeedItem.KIND_PLAYLIST -> "playlist?list=${id.removePrefix("VL")}"
        YtFeedItem.KIND_CHANNEL -> id.takeIf { it.startsWith("UC") }?.let { "channel/$it" }
        else -> null
    }?.let { "https://www.ymusicapp.com/$it" }
}

@Stable
internal class SongActions(
    private val graph: AppGraph,
    private val scope: CoroutineScope,
    private val clipboard: Clipboard,
    private val showSnackbar: (String) -> Unit,
    private val reveal: (MediaStoreItem) -> Unit,
    private val goToGroup: (LibraryGroupKind, MediaStoreItem) -> Unit,
) {
    private var addingToPlaylist by mutableStateOf<MediaStoreItem?>(null)
    private var confirmingDelete by mutableStateOf<Pair<MediaStoreItem, (suspend () -> Unit)?>?>(null)

    val canDelete: Boolean get() = graph.deleteFromDisk != null && graph.mediaStore != null

    // One instance: a fresh value per provided() call would recompose every reader.
    private val revealInFileManager: RevealInFileManager? = run {
        val hook = graph.revealInFolder ?: return@run null
        val label = graph.revealInFolderLabel ?: return@run null
        RevealInFileManager(label) { uri -> scope.launch { hook(uri) } }
    }

    fun addToPlaylist(song: MediaStoreItem) {
        addingToPlaylist = song
    }

    fun requestDelete(song: MediaStoreItem, afterDelete: (suspend () -> Unit)? = null) {
        if (canDelete) confirmingDelete = song to afterDelete
    }

    fun shareYoutube(kind: String, id: String) {
        val link = youtubeShareLink(kind, id) ?: return
        val share = graph.shareText
        if (share != null) {
            share(link)
        } else {
            scope.launch {
                clipboard.setClipEntry(plainTextClipEntry(link))
                showSnackbar(getString(Res.string.link_copied))
            }
        }
    }

    @Composable
    fun provided(): Array<ProvidedValue<*>> {
        val store = graph.mediaStore
        return arrayOf(
            LocalSongActions provides this,
            LocalAddToPlaylist provides if (store != null) ::addToPlaylist else null,
            LocalLibraryDelete provides if (canDelete) ({ song -> requestDelete(song) }) else null,
            LocalLibraryReveal provides if (store != null) reveal else null,
            LocalRevealInFileManager provides revealInFileManager,
            LocalGoToLibraryGroup provides if (store != null) goToGroup else null,
            LocalShareYoutube provides ::shareYoutube,
        )
    }

    @Composable
    fun Dialogs() {
        val store = graph.mediaStore ?: return
        addingToPlaylist?.let { song ->
            AddToPlaylistDialog(store, song, onDismiss = { addingToPlaylist = null })
        }
        confirmingDelete?.let { (doomed, afterDelete) ->
            DeleteFromDiskDialog(
                doomed,
                onDismiss = { confirmingDelete = null },
                log = graph.logger?.fork("library"),
                delete = {
                    graph.deleteFromDisk!!.invoke(doomed)
                    store.deleteItem(doomed.id, hard = true)
                    afterDelete?.invoke()
                },
            )
        }
    }
}

@Composable
internal fun rememberSongActions(
    graph: AppGraph,
    showSnackbar: (String) -> Unit,
    reveal: (MediaStoreItem) -> Unit,
    goToGroup: (LibraryGroupKind, MediaStoreItem) -> Unit,
): SongActions {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val currentSnackbar by rememberUpdatedState(showSnackbar)
    val currentReveal by rememberUpdatedState(reveal)
    val currentGoToGroup by rememberUpdatedState(goToGroup)
    return remember(graph, clipboard) {
        SongActions(
            graph,
            scope,
            clipboard,
            showSnackbar = { currentSnackbar(it) },
            reveal = { currentReveal(it) },
            goToGroup = { kind, song -> currentGoToGroup(kind, song) },
        )
    }
}
