package io.ymusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.extractMusicBrowse
import io.ymusic.dynamicengine.yt.extractMusicCharts
import io.ymusic.dynamicengine.yt.extractMusicExplore
import io.ymusic.dynamicengine.yt.extractMusicHome
import io.ymusic.dynamicengine.yt.extractMusicLibrary
import io.ymusic.dynamicengine.yt.extractMusicNewReleases
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.detail_playlist
import io.ymusic.ui.resources.play_all
import io.ymusic.ui.resources.shuffle_all
import io.ymusic.ui.resources.ytmusic_explore_empty
import io.ymusic.ui.resources.ytmusic_home_empty
import io.ymusic.ui.resources.ytmusic_library_empty
import io.ymusic.ui.resources.ytmusic_tab_charts
import io.ymusic.ui.resources.ytmusic_tab_explore
import io.ymusic.ui.resources.ytmusic_tab_home
import io.ymusic.ui.resources.ytmusic_tab_library
import io.ymusic.ui.resources.ytmusic_tab_new_releases
import kmpcommon.log.Logger
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max

const val MusicBrowsePrefix = "/ytmusic/browse"

enum class YtMusicTab(val label: StringResource) {
    Home(Res.string.ytmusic_tab_home),
    Explore(Res.string.ytmusic_tab_explore),
    NewReleases(Res.string.ytmusic_tab_new_releases),
    Charts(Res.string.ytmusic_tab_charts),
    Library(Res.string.ytmusic_tab_library),
}

@Composable
fun YtMusicScreen(
    graph: AppGraph,
    coordinator: PlaybackCoordinator,
    sessions: MutableMap<YtMusicTab, FeedSession>,
    localeEpoch: Int,
    scope: CoroutineScope,
    signedIn: Boolean = false,
    onLogin: () -> Unit = {},
    onOpen: ((YtFeedItem) -> Unit)? = null,
    onDeeplink: ((link: String, title: String?) -> Unit)? = null,
) {
    var tab by rememberSaveable { mutableStateOf(YtMusicTab.Home) }
    val tabs = remember(graph) { YtMusicTab.entries.filter { !graph.guest || it != YtMusicTab.Library } }
    DisposableEffect(tabs) {
        val remove = GlobalShortcutActions.pushTabHandler { i ->
            tabs.getOrNull(i - 1)?.let { tab = it; true } ?: false
        }
        onDispose { remove() }
    }
    var tabDrag by remember { mutableFloatStateOf(0f) }
    val openDrawer = LocalOpenNavDrawer.current
    Column(Modifier.fillMaxSize()) {
        ElevatedBarColumn {
            YtMusicTabRow(tabs, tab, tabDrag) { tab = it }
            HorizontalDivider(color = DesignColors.Divider)
        }
        SwipeTabSwitcher(
            tab,
            tabs,
            onSelect = { tab = it },
            onDragProgress = { tabDrag = it },
            leadingEdgeReserve = NavDrawerEdgeReserve,
            onLeadingEdgeSwipe = openDrawer,
        ) { current ->
            if (current == YtMusicTab.Library && !signedIn) {
                LoginPreface(onLogin)
            } else {
                val session = remember(current, localeEpoch) {
                    sessions.getOrPut(current) {
                        val log = graph.logger?.fork("YtMusic" + current.name)
                        FeedSession(
                            scope,
                            log,
                            reloadFetch = if (current == YtMusicTab.Home) {
                                { continuation -> graph.engines.extractMusicHome(continuation, log, refresh = true) }
                            } else {
                                null
                            },
                        ) { continuation ->
                            when (current) {
                                YtMusicTab.Explore -> graph.engines.extractMusicExplore(continuation, log)
                                YtMusicTab.NewReleases -> graph.engines.extractMusicNewReleases(continuation, log)
                                YtMusicTab.Charts -> graph.engines.extractMusicCharts(continuation, log)
                                YtMusicTab.Library -> graph.engines.extractMusicLibrary(continuation, log)
                                else -> graph.engines.extractMusicHome(continuation, log)
                            }
                        }.also { it.pager.refresh() }
                    }
                }
                FeedList(
                    session.pager,
                    coordinator,
                    emptyMessage = stringResource(
                        when (current) {
                            YtMusicTab.Explore -> Res.string.ytmusic_explore_empty
                            YtMusicTab.Library -> Res.string.ytmusic_library_empty
                            else -> Res.string.ytmusic_home_empty
                        },
                    ),
                    onOpen = onOpen,
                    listState = session.listState,
                    onDeeplink = onDeeplink,
                )
            }
        }
    }
}

@Composable
private fun YtMusicTabRow(
    tabs: List<YtMusicTab>,
    selected: YtMusicTab,
    dragProgress: Float,
    onSelect: (YtMusicTab) -> Unit,
) {
    UnderlineTabRow(selected, tabs, dragProgress, onSelect) { t, active ->
        TabLabel(stringResource(t.label), active)
    }
}

class MusicBrowseSession(
    graph: AppGraph,
    scope: CoroutineScope,
    val url: String,
    val item: YtFeedItem,
    parentLog: Logger?,
) {
    internal val log = parentLog?.fork("MusicBrowse", mapOf("url" to url))
    val feed = FeedSession(scope, log) { continuation ->
        graph.engines.extractMusicBrowse(url, continuation = continuation, logger = log)
    }

    init {
        feed.pager.refresh()
    }

    fun close() {
        feed.close()
    }
}

@Composable
fun MusicDetailScreen(
    coordinator: PlaybackCoordinator,
    session: MusicBrowseSession,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    onOpen: ((YtFeedItem) -> Unit)? = null,
    onDeeplink: ((link: String, title: String?) -> Unit)? = null,
    onDownload: ((title: String, pager: Pager) -> Unit)? = null,
) {
    val state by session.feed.pager.state.collectAsState()
    val ready = state as? Pager.State.Ready
    val videos = ready?.sections.orEmpty().flatMap { it.items }.filter { it.kind == YtFeedItem.KIND_VIDEO }
    val isFeed = session.url.contains("id=FEmusic")

    val store = coordinator.graph.mediaStore
    var creatingLocal by remember { mutableStateOf<String?>(null) }
    if (store != null) {
        creatingLocal?.let { title -> LocalPlaylistFromDownloadsDialog(store, title, session.feed.pager) { creatingLocal = null } }
    }

    Column(Modifier.fillMaxSize()) {
        DetailAppBar(
            if (isFeed) (ready?.pageHeader?.title ?: session.item.title) else stringResource(Res.string.detail_playlist),
            onBack,
            onSearch,
        )
        FeedList(
            session.feed.pager,
            coordinator,
            onPlayItem = if (isFeed) {
                null
            } else {
                { tapped ->
                    coordinator.playAll(
                        videos,
                        startIndex = videos.indexOfFirst { it.id == tapped.id }.coerceAtLeast(0),
                        source = ready.queueSource(coordinator.graph.engines, session.url, videos, session.log),
                    )
                }
            },
            topContent = if (isFeed) {
                if (videos.isEmpty()) {
                    null
                } else {
                    {
                        PlayAllBar(
                            onPlayAll = { coordinator.playAll(videos, shuffle = false) },
                            onShuffleAll = { coordinator.playAll(videos, startIndex = videos.indices.random(), shuffle = true) },
                        )
                    }
                }
            } else {
                {
                    val title = ready?.pageHeader?.title ?: session.item.title
                    PlaylistHeader(
                        title = title,
                        meta = ready?.pageHeader?.subtitleLine1 ?: session.item.subtitleLine1,
                        onShare = sharePlaylist(session.item, ready?.pageHeader),
                        onShuffle = if (videos.isEmpty()) {
                            null
                        } else {
                            {
                                coordinator.playAll(
                                    videos,
                                    startIndex = videos.indices.random(),
                                    shuffle = true,
                                    source = ready.queueSource(coordinator.graph.engines, session.url, videos, session.log),
                                )
                            }
                        },
                        onDownload = onDownload?.let { open -> { open(title, session.feed.pager) } },
                        onCreateLocal = store?.let { { creatingLocal = title } },
                    )
                }
            },
            onOpen = onOpen,
            listState = session.feed.listState,
            onDeeplink = onDeeplink,
        )
    }
}

@Composable
internal fun PlayAllBar(onPlayAll: () -> Unit, onShuffleAll: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = DesignSpacing.Space16, vertical = DesignSpacing.Space8),
        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space8),
    ) {
        PlayPill(Icons.Default.PlayArrow, stringResource(Res.string.play_all), filled = true, onClick = onPlayAll)
        PlayPill(Icons.Default.Shuffle, stringResource(Res.string.shuffle_all), filled = false, onClick = onShuffleAll)
    }
}

@Composable
internal fun PlayPill(icon: ImageVector, label: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    val scheme = MaterialTheme.colorScheme
    val content = if (filled) DesignColors.OnAccent else scheme.onSecondaryContainer
    Row(
        Modifier
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(DesignColors.Accent)
                } else {
                    Modifier.background(scheme.secondaryContainer).border(1.dp, DesignColors.OutlineSoft, shape)
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = content)
        Text(
            label,
            Modifier.offset { IntOffset(0, -max(1, 0.4.dp.roundToPx())) },
            maxLines = 1,
            softWrap = false,
            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = content),
        )
    }
}
