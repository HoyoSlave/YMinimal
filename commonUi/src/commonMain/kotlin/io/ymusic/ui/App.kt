package io.ymusic.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OndemandVideo
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import io.ymusic.download.DownloadFormat
import io.ymusic.dynamicengine.yt.YtAccountInfo
import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.YtFeedPage
import io.ymusic.dynamicengine.yt.YtLinkTarget
import io.ymusic.dynamicengine.yt.YtSection
import io.ymusic.dynamicengine.yt.extractAccountLibrary
import io.ymusic.dynamicengine.yt.extractHomeFeed
import io.ymusic.dynamicengine.yt.extractMusicBrowse
import io.ymusic.dynamicengine.yt.extractTrendingMusicFeed
import io.ymusic.dynamicengine.yt.getRecommendedFeed
import io.ymusic.dynamicengine.yt.resolveLink
import io.ymusic.player.PlayableItem
import io.ymusic.player.QueueOrigin
import io.ymusic.player.isEnded
import io.ymusic.player.isPlaying
import io.ymusic.player.showsVideo
import io.ymusic.settings.AppSettings
import io.ymusic.settings.flow
import io.ymusic.settings.put
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.captcha_verify
import io.ymusic.ui.resources.coming_soon
import io.ymusic.ui.resources.discover_needs_account
import io.ymusic.ui.resources.discover_no_anonymous_home
import io.ymusic.ui.resources.discover_no_recommendations
import io.ymusic.ui.resources.discover_no_top_tracks
import io.ymusic.ui.resources.discover_no_trending
import io.ymusic.ui.resources.error_captcha
import io.ymusic.ui.resources.error_generic
import io.ymusic.ui.resources.error_network
import io.ymusic.ui.resources.link_open_failed
import io.ymusic.ui.resources.link_queued_last
import io.ymusic.ui.resources.link_queued_next
import io.ymusic.ui.resources.link_unsupported
import io.ymusic.ui.resources.search_videos
import io.ymusic.ui.resources.section_catalogue
import io.ymusic.ui.resources.section_discover
import io.ymusic.ui.resources.section_donate
import io.ymusic.ui.resources.section_downloads
import io.ymusic.ui.resources.section_feedback
import io.ymusic.ui.resources.section_library
import io.ymusic.ui.resources.section_play_again
import io.ymusic.ui.resources.section_settings
import io.ymusic.ui.resources.section_telegram
import io.ymusic.ui.resources.section_yt_music
import io.ymusic.ui.resources.signin_button
import io.ymusic.ui.resources.tab_account_library
import io.ymusic.ui.resources.tab_home
import io.ymusic.ui.resources.tab_music
import io.ymusic.ui.resources.tab_recommended
import io.ymusic.ui.resources.tab_top_tracks
import io.ymusic.ui.resources.yt_playlist_sign_in
import kmpcommon.log.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import projectcommon.error.errorCode
import kotlin.math.abs
import kotlin.math.roundToInt

enum class AppSection(val id: Int, val label: StringResource, val icon: ImageVector, val group: Int) {
    Discover(1, Res.string.section_discover, Icons.Outlined.OndemandVideo, 0),
    YtMusic(2, Res.string.section_yt_music, Icons.Default.PlayCircleOutline, 0),
    Library(3, Res.string.section_library, Icons.Outlined.LibraryMusic, 0),
    PlayAgain(4, Res.string.section_play_again, Icons.Outlined.History, 1),
    Downloads(5, Res.string.section_downloads, Icons.Outlined.FileDownload, 1),
    Settings(6, Res.string.section_settings, Icons.Outlined.Settings, 1),
    Catalogue(7, Res.string.section_catalogue, Icons.Outlined.Palette, 1),
    Donate(8, Res.string.section_donate, Icons.Outlined.FavoriteBorder, 2),
    Telegram(9, Res.string.section_telegram, TelegramIcon, 2),
    Feedback(10, Res.string.section_feedback, Icons.AutoMirrored.Outlined.Chat, 2),
}

val LocalNavigateToSection = compositionLocalOf<(AppSection) -> Unit> { {} }

val LocalLibraryFolderReveal = compositionLocalOf<MutableState<Long?>?> { null }

@Composable
fun App(graph: AppGraph, catalogue: (@Composable () -> Unit)? = null) {
    App(graph, rememberStartSection(graph), catalogue)
}

private val QueueActions = setOf(LinkAction.QUEUE_NEXT, LinkAction.QUEUE_LAST)
private val DownloadActions = setOf(LinkAction.DOWNLOAD, LinkAction.DOWNLOAD_M4A, LinkAction.DOWNLOAD_MP3)
private val ResumableSections = setOf(AppSection.Discover, AppSection.Library, AppSection.YtMusic)

@Composable
fun rememberStartSection(graph: AppGraph): MutableState<AppSection> = rememberSaveable {
    val saved = graph.settings.flow(AppSettings.lastSection).value
    mutableStateOf(ResumableSections.firstOrNull { it.id == saved } ?: AppSection.Discover)
}

@Composable
fun App(
    graph: AppGraph,
    sectionState: MutableState<AppSection>,
    catalogue: (@Composable () -> Unit)? = null,
) {
    val tag by graph.uiLanguage.collectAsState()
    val themePref by remember(graph) { graph.settings.flow(AppSettings.theme) }.collectAsState()
    val accentPref by remember(graph) { graph.settings.flow(AppSettings.themeAccent) }.collectAsState()
    val palette = rememberStockAndroidPalette(themePref, isSystemInDarkTheme(), accentPref)
    var section by sectionState
    LaunchedEffect(graph.settings) {
        snapshotFlow { sectionState.value }
            .filter { it in ResumableSections }
            .collect { graph.settings.put(AppSettings.lastSection, it.id) }
    }
    key(tag) {
        val direction = if (isRtlLanguage(Locale.current.language)) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides direction) {
            YMusicTheme(palette) {
                val platformUriHandler = LocalUriHandler.current
                val uriHandler = remember(platformUriHandler) { SafeUriHandler(platformUriHandler, graph.logger?.fork("uri-handler")) }
                CompositionLocalProvider(
                    LocalImageLoader provides graph.imageLoader,
                    LocalRefreshRequests provides graph.refreshRequests,
                    LocalUriHandler provides uriHandler,
                ) {
                    AppRoot(graph, section, catalogue, uriFailures = uriHandler.failures, onSection = { section = it })
                }
            }
        }
    }
}

internal val LocalRefreshRequests = compositionLocalOf<Flow<Unit>?> { null }

internal val LocalSnackbar = compositionLocalOf<(String) -> Unit> { {} }

internal class CaptchaResolver(val label: StringResource, val resolve: (retry: () -> Unit) -> Unit)

internal val LocalCaptchaResolver = compositionLocalOf<CaptchaResolver?> { null }

internal class SignInResolver(val signedIn: Boolean, val signIn: (onSignedIn: () -> Unit) -> Unit)

internal val LocalSignInResolver = compositionLocalOf<SignInResolver?> { null }

internal val LocalMiniPlayerInset = compositionLocalOf { 0.dp }

private sealed interface AppLayer {
    class Search(val session: SearchSession) : AppLayer
    class LibrarySearch(val session: LibrarySearchSession) : AppLayer
    class Playlist(val session: PlaylistSession) : AppLayer
    class Channel(val session: ChannelSession) : AppLayer
    class Feed(val session: FeedDetailSession) : AppLayer
    class MusicBrowse(val session: MusicBrowseSession) : AppLayer
    class PlaylistDownload(val session: PlaylistDownloadSession) : AppLayer
    class Login(val state: LoginWebViewState, val switchAccount: Boolean = false) : AppLayer
    class Verify(val state: LoginWebViewState, val retries: MutableList<() -> Unit>) : AppLayer
}

private val AppLayer.log: Logger?
    get() = when (this) {
        is AppLayer.Search -> session.log
        is AppLayer.Playlist -> session.log
        is AppLayer.Channel -> session.log
        is AppLayer.Feed -> session.log
        is AppLayer.MusicBrowse -> session.log
        is AppLayer.LibrarySearch, is AppLayer.Login, is AppLayer.Verify, is AppLayer.PlaylistDownload -> null
    }

@Composable
private fun PictureHidden(graph: AppGraph, hidden: Boolean) {
    SideEffect { graph.pip.pictureHidden.value = hidden }
}

@Composable
private fun AppRoot(
    graph: AppGraph,
    section: AppSection,
    catalogue: (@Composable () -> Unit)?,
    uriFailures: Flow<String>,
    onSection: (AppSection) -> Unit,
) {
    DisposableEffect(graph) {
        onDispose { graph.pip.pictureHidden.value = true }
    }
    run {
        val scope = rememberCoroutineScope()
        val coordinator = graph.playback
        val inPip by graph.pip.inPip.collectAsState()
        if (inPip) {
            val pipStatus by graph.player.status.collectAsState()
            KeepScreenOn(pipStatus.state.isPlaying() && !pipStatus.state.isEnded() && !showsAudioOnly(graph.player))
            PictureHidden(graph, hidden = false)
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                VideoHole(graph.player, graph.playerSurface, Modifier.fillMaxSize())
            }
            return@run
        }
        val libraryReveal = remember { mutableStateOf<Long?>(null) }
        val libraryGroupReveal = remember { mutableStateOf<LibraryGroupReveal?>(null) }
        val accountController = graph.account
        val signedIn by accountController.signedIn.collectAsState()
        val account by accountController.account.collectAsState()
        val profileReady by accountController.profileReady.collectAsState()
        val loginEpoch by accountController.loginEpoch.collectAsState()
        val scanController = graph.scans
        val drawer = rememberDrawerState(DrawerValue.Closed)
        var railToggled by rememberSaveable { mutableStateOf<Boolean?>(null) }
        var usesRail by remember { mutableStateOf(false) }
        if (catalogue == null) {
            LaunchedEffect(section) {
                if (section == AppSection.Catalogue) onSection(AppSection.Discover)
            }
        }
        val discoverTabs = remember(graph) { discoverTabsFor(graph) }
        var discoverTab by rememberSaveable {
            val saved = graph.settings.flow(AppSettings.lastDiscoverTab).value
            mutableStateOf(discoverTabs.firstOrNull { it.id == saved } ?: DiscoverTab.Music)
        }
        LaunchedEffect(discoverTab) { graph.settings.put(AppSettings.lastDiscoverTab, discoverTab.id) }
        var discoverDrag by remember { mutableFloatStateOf(0f) }
        val nowPlayingOpen by graph.pip.nowPlayingOpen.collectAsState()
        var nowPlayingPeeking by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        var miniHeightPx by remember { mutableIntStateOf(0) }
        val showSnackbar: (String) -> Unit = { message ->
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            }
        }
        DisposableEffect(snackbarHostState) {
            GlobalShortcutActions.showMessage = showSnackbar
            onDispose { GlobalShortcutActions.showMessage = {} }
        }
        LaunchedEffect(uriFailures) {
            uriFailures.collect { uri -> showSnackbar(getString(Res.string.link_open_failed, uri)) }
        }
        var pendingDownload by remember { mutableStateOf<YtFeedItem?>(null) }
        var pendingQuickDownload by remember { mutableStateOf<YtFeedItem?>(null) }
        var pendingFormatDownload by remember { mutableStateOf<Pair<YtFeedItem, DownloadFormat>?>(null) }
        val downloadLauncher = remember(graph) {
            graph.downloads?.let {
                object : DownloadLauncher {
                    override fun launch(video: YtFeedItem) {
                        pendingDownload = video
                    }

                    override val quickPreset = graph.quickDownload

                    override fun quickLaunch(video: YtFeedItem) {
                        pendingQuickDownload = video
                    }
                }
            }
        }
        val availableOffline = rememberAvailableOffline(graph)
        var ytPlaylistRequest by remember { mutableStateOf<YtPlaylistRequest?>(null) }
        val ytPlaylistSignIn = stringResource(Res.string.yt_playlist_sign_in)
        val ytPlaylistActions = remember(signedIn, ytPlaylistSignIn) {
            YtPlaylistActions(
                addToPlaylist = { videoId ->
                    if (signedIn) ytPlaylistRequest = YtPlaylistRequest.Add(videoId) else showSnackbar(ytPlaylistSignIn)
                },
                saveAsPlaylist = { videoIds ->
                    if (signedIn) {
                        ytPlaylistRequest = YtPlaylistRequest.Create(videoIds, fromQueue = true)
                    } else {
                        showSnackbar(ytPlaylistSignIn)
                    }
                },
            )
        }
        LaunchedEffect(scanController) {
            scanController.summaries.collect { showSnackbar(it) }
        }
        LaunchedEffect(coordinator) {
            coordinator.player.queue.collect {
                if (it.items.isEmpty()) {
                    graph.pip.nowPlayingOpen.value = false
                    nowPlayingPeeking = false
                }
            }
        }
        val discoverSessions = remember { mutableMapOf<String, FeedSession>() }
        val ytMusicSessions = remember { mutableMapOf<YtMusicTab, FeedSession>() }
        val localeEpoch by graph.localeEpoch.collectAsState()
        remember(localeEpoch, loginEpoch) {
            discoverSessions.values.forEach { it.close() }
            discoverSessions.clear()
            ytMusicSessions.values.forEach { it.close() }
            ytMusicSessions.clear()
        }
        val sectionLayers = remember { mutableMapOf<AppSection, SnapshotStateList<AppLayer>>() }
        val layers = sectionLayers.getOrPut(section) { mutableStateListOf() }
        val sectionStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
        fun parentLog() = layers.lastOrNull()?.log ?: graph.logger
        val openItem: (YtFeedItem) -> Unit = { item ->
            val url = item.url
            if (url != null) {
                if (url.startsWith(MusicBrowsePrefix)) {
                    if (item.kind == YtFeedItem.KIND_CHANNEL) {
                        layers.add(
                            AppLayer.Channel(
                                ChannelSession(graph, scope, item, initialUrl = url, parentLog = parentLog()) { u, c, l ->
                                    graph.engines.extractMusicBrowse(u, c, logger = l)
                                },
                            ),
                        )
                    } else {
                        layers.add(AppLayer.MusicBrowse(MusicBrowseSession(graph, scope, url, item, parentLog())))
                    }
                } else {
                    when (item.kind) {
                        YtFeedItem.KIND_PLAYLIST ->
                            layers.add(AppLayer.Playlist(PlaylistSession(graph, scope, url, item, parentLog())))
                        YtFeedItem.KIND_CHANNEL ->
                            layers.add(AppLayer.Channel(ChannelSession(graph, scope, item, initialUrl = url, parentLog = parentLog())))
                    }
                }
            }
        }
        fun openDeeplinkIn(stack: MutableList<AppLayer>, link: String, title: String?) {
            val seed = { kind: String ->
                YtFeedItem(kind = kind, id = link, title = title.orEmpty(), url = link)
            }
            if (link.startsWith(MusicBrowsePrefix)) {
                stack.add(
                    AppLayer.MusicBrowse(
                        MusicBrowseSession(graph, scope, link, seed(YtFeedItem.KIND_PLAYLIST), parentLog()),
                    ),
                )
            } else {
                when (val target = parseDeeplink(link)) {
                    is Deeplink.Playlist -> stack.add(
                        AppLayer.Playlist(
                            PlaylistSession(graph, scope, target.url, seed(YtFeedItem.KIND_PLAYLIST), parentLog()),
                        ),
                    )
                    is Deeplink.Channel -> stack.add(
                        AppLayer.Channel(
                            ChannelSession(
                                graph,
                                scope,
                                seed(YtFeedItem.KIND_CHANNEL),
                                initialUrl = target.url,
                                parentLog = parentLog(),
                            ),
                        ),
                    )
                    is Deeplink.Feed -> stack.add(
                        AppLayer.Feed(FeedDetailSession(graph, scope, target.url, title, parentLog())),
                    )
                    null -> Unit
                }
            }
        }
        val openDeeplink: (String, String?) -> Unit = { link, title -> openDeeplinkIn(layers, link, title) }
        val openSearch: () -> Unit = {
            val existing = layers.indexOfFirst { it is AppLayer.Search }
            when {
                existing >= 0 && existing == layers.lastIndex -> Unit
                existing >= 0 -> layers.add(layers.removeAt(existing))
                else -> layers.add(AppLayer.Search(SearchSession.youtube(graph, scope)))
            }
        }
        DisposableEffect(drawer, scope) {
            GlobalShortcutActions.toggleNavDrawer = {
                scope.launch { if (drawer.isOpen) drawer.close() else drawer.open() }
            }
            GlobalShortcutActions.closeNavDrawerIfOpen = {
                if (drawer.isOpen) {
                    scope.launch { drawer.close() }
                    true
                } else {
                    false
                }
            }
            onDispose {
                GlobalShortcutActions.toggleNavDrawer = {}
                GlobalShortcutActions.closeNavDrawerIfOpen = { false }
            }
        }
        if (section == AppSection.Discover && layers.isEmpty() && !nowPlayingOpen) {
            DisposableEffect(discoverTabs) {
                val remove = GlobalShortcutActions.pushTabHandler { i ->
                    discoverTabs.getOrNull(i - 1)?.let {
                        discoverTab = it
                        true
                    } ?: false
                }
                onDispose { remove() }
            }
        }
        val openMusicSearch: () -> Unit = {
            val existing = layers.indexOfFirst { it is AppLayer.Search }
            when {
                existing >= 0 && existing == layers.lastIndex -> Unit
                existing >= 0 -> layers.add(layers.removeAt(existing))
                else -> layers.add(AppLayer.Search(SearchSession.music(graph, scope)))
            }
        }
        val openPlaylistDownload: ((title: String, pager: Pager) -> Unit)? = graph.downloads?.let {
            { title, pager -> layers.add(AppLayer.PlaylistDownload(PlaylistDownloadSession(title, pager, graph.quickDownload.value))) }
        }
        var afterSignIn by remember { mutableStateOf<(() -> Unit)?>(null) }
        val popLayer: () -> Unit = popLayer@{
            val top = layers.removeLastOrNull() ?: return@popLayer
            when (top) {
                is AppLayer.Search -> top.session.close()
                is AppLayer.Playlist -> top.session.close()
                is AppLayer.Channel -> top.session.close()
                is AppLayer.Feed -> top.session.close()
                is AppLayer.MusicBrowse -> top.session.close()
                is AppLayer.LibrarySearch, is AppLayer.Login, is AppLayer.Verify, is AppLayer.PlaylistDownload -> Unit
            }
            if (top is AppLayer.Login) afterSignIn = null
            if (top is AppLayer.Login && top.switchAccount) accountController.onLoginComplete()
            if (top is AppLayer.Verify) {
                val retries = top.retries.toList()
                accountController.onCaptchaVerified { retries.forEach { it() } }
            }
        }
        val openLogin: () -> Unit = {
            val existing = layers.indexOfFirst { it is AppLayer.Login }
            when {
                existing >= 0 && existing == layers.lastIndex -> Unit
                existing >= 0 -> layers.add(layers.removeAt(existing))
                else -> layers.add(AppLayer.Login(LoginWebViewState()))
            }
        }
        val openSwitchAccount: () -> Unit = {
            val existing = layers.indexOfFirst { it is AppLayer.Login }
            when {
                existing >= 0 && existing == layers.lastIndex -> Unit
                existing >= 0 -> layers.add(layers.removeAt(existing))
                else -> layers.add(AppLayer.Login(LoginWebViewState(), switchAccount = true))
            }
        }
        val signInResolver = SignInResolver(signedIn) { onSignedIn ->
            graph.pip.nowPlayingOpen.value = false
            nowPlayingPeeking = false
            afterSignIn = onSignedIn
            openLogin()
        }
        val captchaResolver = if (graph.loginBrowser != null) {
            CaptchaResolver(Res.string.captcha_verify) { retry ->
                graph.pip.nowPlayingOpen.value = false
                nowPlayingPeeking = false
                val existing = layers.indexOfFirst { it is AppLayer.Verify }
                when {
                    existing >= 0 -> {
                        (layers[existing] as AppLayer.Verify).retries += retry
                        if (existing != layers.lastIndex) layers.add(layers.removeAt(existing))
                    }
                    else -> layers.add(AppLayer.Verify(LoginWebViewState(), mutableListOf(retry)))
                }
            }
        } else {
            CaptchaResolver(Res.string.signin_button) { _ ->
                graph.pip.nowPlayingOpen.value = false
                nowPlayingPeeking = false
                openLogin()
            }
        }
        val openLibrarySearch: () -> Unit = {
            val store = graph.mediaStore
            if (store != null) {
                val existing = layers.indexOfFirst { it is AppLayer.LibrarySearch }
                when {
                    existing >= 0 && existing == layers.lastIndex -> Unit
                    existing >= 0 -> layers.add(layers.removeAt(existing))
                    else -> layers.add(AppLayer.LibrarySearch(LibrarySearchSession(store)))
                }
            }
        }
        val sectionSearch: (() -> Unit)? = when {
            section == AppSection.Discover -> openSearch
            section == AppSection.YtMusic -> openMusicSearch
            section == AppSection.Library && graph.mediaStore != null -> openLibrarySearch
            else -> null
        }
        val hotkeySearch: () -> Unit = {
            if (sectionSearch != null) {
                sectionSearch()
                (layers.lastOrNull() as? AppLayer.Search)?.session?.editing = true
            }
        }
        val latestHotkeySearch by rememberUpdatedState(hotkeySearch)
        DisposableEffect(Unit) {
            GlobalShortcutActions.openSearch = { latestHotkeySearch() }
            onDispose { GlobalShortcutActions.openSearch = {} }
        }
        val searchOnline: (String) -> Unit = { q ->
            onSection(AppSection.Discover)
            val discover = sectionLayers.getOrPut(AppSection.Discover) { mutableStateListOf() }
            val existing = discover.indexOfFirst { it is AppLayer.Search }
            val layer = if (existing >= 0) {
                discover.removeAt(existing) as AppLayer.Search
            } else {
                AppLayer.Search(SearchSession.youtube(graph, scope))
            }
            layer.session.apply {
                field = TextFieldValue(q)
                submitted = q
                editing = false
                filterParams = null
                pager.refresh()
            }
            discover.add(layer)
        }
        val linkUnsupported = stringResource(Res.string.link_unsupported)
        val linkQueuedNext = stringResource(Res.string.link_queued_next)
        val linkQueuedLast = stringResource(Res.string.link_queued_last)
        val latestOnSection by rememberUpdatedState(onSection)
        fun openPage(link: String) {
            graph.pip.nowPlayingOpen.value = false
            latestOnSection(AppSection.Discover)
            openDeeplinkIn(sectionLayers.getOrPut(AppSection.Discover) { mutableStateListOf() }, link, null)
        }
        LaunchedEffect(graph, coordinator) {
            IncomingLinks.links.collect { (url, action) ->
                val log = graph.logger?.fork("IncomingLink", mapOf("action" to action.name))
                val target = try {
                    graph.engines.resolveLink(url, log)
                } catch (c: CancellationException) {
                    throw c
                } catch (e: Exception) {
                    log?.log(Logger.Level.WARN, "resolve link failed", e)
                    showSnackbar(
                        when (val code = e.errorCode() ?: "E20113") {
                            "network" -> getString(Res.string.error_network)
                            CAPTCHA_CODE -> getString(Res.string.error_captcha)
                            else -> getString(Res.string.error_generic, code)
                        },
                    )
                    return@collect
                }
                when (target) {
                    is YtLinkTarget.Video -> {
                        val video = target.item
                        val queueEmpty = coordinator.player.queue.value.items.isEmpty()
                        val downloads = graph.downloads
                        when {
                            action == LinkAction.PLAY_SINGLE || (queueEmpty && action in QueueActions) ->
                                coordinator.player.setQueue(
                                    listOf(video.toYtMedia()),
                                    startPositionMs = target.startMs ?: 0L,
                                    origin = QueueOrigin.Link,
                                )
                            action == LinkAction.QUEUE_NEXT -> {
                                coordinator.playNext(video)
                                showSnackbar(linkQueuedNext)
                            }
                            action == LinkAction.QUEUE_LAST -> {
                                coordinator.playLater(video)
                                showSnackbar(linkQueuedLast)
                            }
                            action == LinkAction.DOWNLOAD_M4A && downloads != null ->
                                pendingFormatDownload = video to DownloadFormat.M4A
                            action == LinkAction.DOWNLOAD_MP3 && downloads != null && downloads.ffmpegAvailable ->
                                pendingFormatDownload = video to DownloadFormat.MP3
                            action in DownloadActions && downloads != null -> pendingDownload = video
                            else -> {
                                val playlist = target.playlistUrl
                                coordinator.player.setQueue(
                                    listOf(video.toYtMedia()),
                                    startPositionMs = target.startMs ?: 0L,
                                    source = if (playlist != null) {
                                        NextQueueSource.playlist(graph.engines, playlist, listOf(video), null, log?.fork("queue-source"))
                                    } else {
                                        NextQueueSource.radio(graph.engines, video.id, log?.fork("queue-source"))
                                    },
                                    origin = QueueOrigin.Link,
                                )
                                graph.pip.nowPlayingOpen.value = true
                            }
                        }
                    }
                    is YtLinkTarget.Search -> {
                        graph.pip.nowPlayingOpen.value = false
                        searchOnline(target.query)
                    }
                    is YtLinkTarget.Playlist -> openPage(target.url)
                    is YtLinkTarget.Channel -> openPage(target.url)
                    is YtLinkTarget.MusicBrowse -> openPage(target.url)
                    null -> showSnackbar(linkUnsupported)
                }
            }
        }

        val uriHandler = LocalUriHandler.current
        val config by remember(graph) { graph.remoteConfig ?: emptyFlow() }.collectAsState(initial = null)
        var checkingUpdate by remember { mutableStateOf(false) }
        var askFeedbackConsent by remember { mutableStateOf(false) }
        fun openSection(target: AppSection) {
            val needsConsent = target == AppSection.Feedback && graph.captureFeedbackReport != null &&
                !graph.settings.flow(AppSettings.crashReports).value
            if (needsConsent) askFeedbackConsent = true else onSection(target)
        }
        if (askFeedbackConsent) {
            ReportsOffDialog(
                onContinue = {
                    askFeedbackConsent = false
                    scope.launch {
                        graph.settings.put(AppSettings.crashReports, true)
                        onSection(AppSection.Feedback)
                    }
                },
                onCancel = { askFeedbackConsent = false },
            )
        }
        fun openLibraryRoot() {
            graph.pip.nowPlayingOpen.value = false
            nowPlayingPeeking = false
            sectionLayers[AppSection.Library]?.clear()
            openSection(AppSection.Library)
        }
        val songActions = rememberSongActions(
            graph,
            showSnackbar,
            reveal = { song ->
                libraryReveal.value = song.bucketId
                openLibraryRoot()
            },
            goToGroup = { kind, song ->
                val name = when (kind) {
                    LibraryGroupKind.Artist -> song.artist
                    LibraryGroupKind.Album -> song.album
                }
                libraryGroupReveal.value = LibraryGroupReveal(kind, name.orEmpty())
                openLibraryRoot()
            },
        )
        val premium by graph.premium.collectAsState()
        var updateRecheck by remember { mutableStateOf(0) }
        val nestedBack = remember { NestedBackStack() }
        val backEnabled = drawer.isOpen || nowPlayingOpen || layers.isNotEmpty() || nestedBack.active ||
            section != AppSection.Discover
        val onBack: () -> Unit = {
            val top = layers.lastOrNull()
            when {
                drawer.isOpen -> scope.launch { drawer.close() }
                nowPlayingOpen -> graph.pip.nowPlayingOpen.value = false
                top is AppLayer.Search && top.session.editing && top.session.submitted != null ->
                    top.session.editing = false
                top is AppLayer.LibrarySearch && top.session.drill != null -> top.session.drill = null
                top is AppLayer.Login && top.state.canGoBack -> top.state.goBack()
                top is AppLayer.Verify && top.state.canGoBack -> top.state.goBack()
                layers.isNotEmpty() -> popLayer()
                nestedBack.active -> nestedBack.back()
                else -> onSection(AppSection.Discover)
            }
        }
        PlatformBackHandler(enabled = backEnabled, onBack = onBack)
        val latestBack by rememberUpdatedState(onBack.takeIf { backEnabled })
        DisposableEffect(Unit) {
            GlobalShortcutActions.back = {
                latestBack?.let {
                    it()
                    true
                } ?: false
            }
            onDispose { GlobalShortcutActions.back = { false } }
        }

        val queue by graph.player.queue.collectAsState()
        val playbackConfig by graph.player.config.collectAsState()
        val quality by graph.player.videoQuality.collectAsState()
        val needsSurface = queue.currentItem is PlayableItem.YtMedia ||
            (queue.currentItem != null && showsVideo(playbackConfig, quality))
        PictureHidden(graph, hidden = !nowPlayingOpen)

        val shellHidden = nowPlayingOpen && (desktopLayoutHole || !platformUsesTouch)
        val scrollHost = remember { ScrollHost() }
        Box(
            Modifier
                .fillMaxSize()
                .then(if (desktopLayoutHole) Modifier else Modifier.background(DesignColors.Surface))
                .drawerEdgeGestureExclusion(!usesRail && !drawer.isOpen && !nowPlayingOpen && layers.isEmpty()),
        ) {
            if (!desktopLayoutHole) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.safeDrawing)
                        .background(chromeTone(scrollHost.scrolled)),
                )
            }
            BoxWithConstraints(
                Modifier.fillMaxSize().safeDrawingPadding(),
            ) {
                val layoutHeight = maxHeight + keyboardOverlap()
                val railWindow = windowUsesRail(maxWidth, layoutHeight)
                val playerBar = desktopLayoutHole && railWindow
                val railStartsOpen = windowStartsRailOpen(maxWidth)
                val railExpanded = railToggled ?: railStartsOpen
                SideEffect { usesRail = railWindow }
                LaunchedEffect(railWindow, railStartsOpen) {
                    railToggled = null
                }
                val shellWidth = if (railWindow) maxWidth - DesignSpacing.RailExpandedWidth else maxWidth
                val wide = shellWidth > layoutHeight && shellWidth >= 600.dp || shellWidth >= 650.dp
                val drawShell = !nowPlayingOpen || nowPlayingPeeking
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithContent { if (drawShell) drawContent() }
                        .then(if (nowPlayingOpen && !shellHidden) Modifier.clearAndSetSemantics {} else Modifier),
                ) {
                    val selectSection: (AppSection) -> Unit = { selected ->
                        when (selected) {
                            AppSection.Donate -> graph.openDonation?.invoke()
                            AppSection.Telegram -> uriHandler.openUri(AppLinks(config).telegram)
                            else -> openSection(selected)
                        }
                        scope.launch { drawer.close() }
                    }
                    val onCheckUpdate: (() -> Unit)? = graph.updates?.let {
                        {
                            checkingUpdate = true
                            scope.launch { drawer.close() }
                        }
                    }
                    val navFrame: @Composable (@Composable () -> Unit) -> Unit = if (railWindow) {
                        { shell ->
                            AppRail(
                                rail = RailContent(
                                    section = section,
                                    sections = listedSections(catalogue != null, graph.guest, graph.openDonation != null),
                                    appVersion = graph.appVersion,
                                    premium = premium,
                                    updateWaiting = graph.updates?.status?.collectAsState()?.value is AppUpdateStatus.Offered,
                                    onCheckUpdate = onCheckUpdate,
                                    websiteUrl = AppLinks(config).website,
                                    onSelect = selectSection,
                                ),
                                expanded = railExpanded,
                                onToggle = {
                                    railToggled = !railExpanded
                                },
                                coordinator = coordinator,
                                surface = if (needsSurface && !nowPlayingOpen) graph.playerSurface else null,
                                onExpandPlayer = {
                                    graph.pip.nowPlayingOpen.value = true
                                },
                                chromeElevated = scrollHost.scrolled,
                                content = shell,
                            )
                        }
                    } else {
                        { shell ->
                            AppNavigationDrawer(
                                drawerState = drawer,
                                drawerContent = {
                                    AppDrawer(
                                        section = section,
                                        appVersion = graph.appVersion,
                                        guest = graph.guest,
                                        showCatalogue = catalogue != null,
                                        onSelect = selectSection,
                                        premium = premium,
                                        showDonate = graph.openDonation != null,
                                        websiteUrl = AppLinks(config).website,
                                        updates = graph.updates,
                                        onCheckUpdate = onCheckUpdate,
                                        open = drawer.isOpen,
                                    )
                                },
                                content = shell,
                            )
                        }
                    }
                    val shellFrame: @Composable (@Composable () -> Unit) -> Unit = if (playerBar) {
                        { shell ->
                            Column(Modifier.fillMaxSize()) {
                                Box(Modifier.weight(1f).fillMaxWidth()) { navFrame(shell) }
                                if (queue.currentItem != null) {
                                    DisposableEffect(Unit) { onDispose { miniHeightPx = 0 } }
                                    DesktopPlayerBar(
                                        coordinator,
                                        surface = if (needsSurface && !nowPlayingOpen) graph.playerSurface else null,
                                        background = chromeTone(scrollHost.scrolled),
                                        onExpand = { graph.pip.nowPlayingOpen.value = true },
                                        modifier = Modifier.onSizeChanged { miniHeightPx = it.height },
                                    )
                                }
                            }
                        }
                    } else {
                        navFrame
                    }
                    val shellContent: @Composable () -> Unit = {
                        val collapsible = platformUsesTouch && !wide &&
                            (section == AppSection.Discover || section == AppSection.YtMusic || section == AppSection.Library)
                        var toolbarHeightPx by remember { mutableStateOf(0) }
                        var toolbarOffsetPx by remember { mutableStateOf(0f) }
                        LaunchedEffect(section, discoverTab, layers.isEmpty(), collapsible) {
                            toolbarOffsetPx = 0f
                        }
                        val collapseConnection = remember(collapsible) {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    if (!collapsible) return Offset.Zero
                                    val previous = toolbarOffsetPx
                                    toolbarOffsetPx = (previous + available.y).coerceIn(-toolbarHeightPx.toFloat(), 0f)
                                    return Offset(0f, toolbarOffsetPx - previous)
                                }
                            }
                        }
                        CompositionLocalProvider(
                            LocalScrollHost provides scrollHost,
                            LocalSnackbar provides showSnackbar,
                            LocalScanController provides scanController,
                            LocalOpenNavDrawer provides if (railWindow) null else { { scope.launch { drawer.open() } } },
                            LocalDownloadLauncher provides downloadLauncher,
                            LocalYtPlaylistActions provides ytPlaylistActions,
                            LocalAvailableOffline provides availableOffline,
                            LocalNavigateToSection provides { openSection(it) },
                            LocalLibraryFolderReveal provides libraryReveal,
                            LocalLibraryGroupReveal provides libraryGroupReveal,
                            *songActions.provided(),
                            LocalCaptchaResolver provides captchaResolver,
                            LocalSignInResolver provides signInResolver,
                        ) {
                            DefaultDownloadLocationPrompt(graph)
                            Scaffold(
                                modifier = Modifier.nestedScroll(collapseConnection),
                                containerColor = Color.Transparent,
                                contentWindowInsets = WindowInsets(0),
                                topBar = {
                                    if (layers.isEmpty()) {
                                        ElevatedBarColumn(bottomShadow = section != AppSection.Library && section != AppSection.YtMusic) {
                                            Box(
                                                Modifier.clipToBounds().layout { measurable, constraints ->
                                                    val placeable = measurable.measure(constraints)
                                                    toolbarHeightPx = placeable.height
                                                    val height = (placeable.height + toolbarOffsetPx).roundToInt().coerceAtLeast(0)
                                                    layout(placeable.width, height) {
                                                        placeable.place(0, toolbarOffsetPx.roundToInt())
                                                    }
                                                },
                                            ) {
                                                DesignAppBar(
                                                    title = stringResource(
                                                        if (section == AppSection.Discover) discoverTab.label else section.label,
                                                    ),
                                                    onMenu = if (railWindow) null else { { scope.launch { drawer.open() } } },
                                                    onSearch = sectionSearch,
                                                    titleReserveWidthOf = if (section == AppSection.Discover && wide) {
                                                        discoverTabs.map { stringResource(it.label) }
                                                    } else {
                                                        emptyList()
                                                    },
                                                    inlineContent = if (section == AppSection.Discover && wide) {
                                                        {
                                                            DiscoverTabRow(
                                                                discoverTab,
                                                                inline = true,
                                                                dragProgress = discoverDrag,
                                                                tabs = discoverTabs,
                                                            ) { discoverTab = it }
                                                        }
                                                    } else {
                                                        null
                                                    },
                                                )
                                            }
                                            if (section == AppSection.Discover && !wide) {
                                                DiscoverTabRow(discoverTab, dragProgress = discoverDrag, tabs = discoverTabs) { discoverTab = it }
                                                HorizontalDivider(color = DesignColors.Divider)
                                            }
                                        }
                                    }
                                },
                            ) { padding ->
                                val screens = @Composable {
                                    when (val top = layers.lastOrNull()) {
                                        is AppLayer.Search -> key(top) {
                                            SearchScreen(
                                                graph,
                                                coordinator,
                                                onBack = popLayer,
                                                onOpen = openItem,
                                                session = top.session,
                                                onDeeplink = openDeeplink,
                                            )
                                        }
                                        is AppLayer.LibrarySearch -> key(top) {
                                            LibrarySearchScreen(
                                                top.session,
                                                coordinator,
                                                onBack = popLayer,
                                                onSearchOnline = searchOnline,
                                            )
                                        }
                                        is AppLayer.Playlist -> key(top) {
                                            PlaylistDetailScreen(
                                                coordinator,
                                                top.session,
                                                onBack = popLayer,
                                                onSearch = openSearch,
                                                onDownload = openPlaylistDownload,
                                            )
                                        }
                                        is AppLayer.PlaylistDownload -> key(top) {
                                            PlaylistDownloadScreen(graph, top.session, onBack = popLayer)
                                        }
                                        is AppLayer.Channel -> key(top) {
                                            ChannelDetailScreen(
                                                coordinator,
                                                top.session,
                                                onBack = popLayer,
                                                onSearch = if (top.session.url.startsWith(MusicBrowsePrefix)) openMusicSearch else openSearch,
                                                onOpen = openItem,
                                                onDeeplink = openDeeplink,
                                            )
                                        }
                                        is AppLayer.Feed -> key(top) {
                                            FeedDetailScreen(
                                                coordinator,
                                                top.session,
                                                onBack = popLayer,
                                                onSearch = openSearch,
                                                onOpen = openItem,
                                                onDeeplink = openDeeplink,
                                            )
                                        }
                                        is AppLayer.MusicBrowse -> key(top) {
                                            MusicDetailScreen(
                                                coordinator,
                                                top.session,
                                                onBack = popLayer,
                                                onSearch = openMusicSearch,
                                                onOpen = openItem,
                                                onDeeplink = openDeeplink,
                                                onDownload = openPlaylistDownload,
                                            )
                                        }
                                        is AppLayer.Login -> key(top) {
                                            YtLoginScreen(
                                                graph,
                                                top.state,
                                                onBack = popLayer,
                                                onComplete = {
                                                    val resume = afterSignIn
                                                    popLayer()
                                                    accountController.onLoginComplete(resume)
                                                },
                                                switchAccount = top.switchAccount,
                                            )
                                        }
                                        is AppLayer.Verify -> key(top) {
                                            YtVerifyScreen(graph, top.state, onDone = popLayer)
                                        }
                                        null -> sectionStateHolder.SaveableStateProvider(section.name) {
                                            when (section) {
                                                AppSection.Discover -> SwipeTabSwitcher(
                                                    discoverTab,
                                                    discoverTabs,
                                                    onSelect = { discoverTab = it },
                                                    onDragProgress = { discoverDrag = it },
                                                    leadingEdgeReserve = NavDrawerEdgeReserve,
                                                    onLeadingEdgeSwipe = if (railWindow) null else { { scope.launch { drawer.open() } } },
                                                ) { current ->
                                                    DiscoverFeed(
                                                        graph,
                                                        coordinator,
                                                        current,
                                                        localeEpoch,
                                                        loginEpoch,
                                                        discoverSessions,
                                                        scope,
                                                        signedIn = signedIn,
                                                        profileReady = profileReady,
                                                        account = account,
                                                        onLogin = openLogin,
                                                        onSignOut = { accountController.signOut() },
                                                        onSwitchAccount = openSwitchAccount,
                                                        onOpen = openItem,
                                                        onDeeplink = openDeeplink,
                                                        onSearch = openSearch,
                                                    )
                                                }
                                                AppSection.YtMusic -> YtMusicScreen(
                                                    graph,
                                                    coordinator,
                                                    ytMusicSessions,
                                                    localeEpoch + loginEpoch,
                                                    scope,
                                                    signedIn = signedIn,
                                                    onLogin = openLogin,
                                                    onOpen = openItem,
                                                    onDeeplink = openDeeplink,
                                                )
                                                AppSection.Library -> LibraryScreen(graph, coordinator)
                                                AppSection.PlayAgain -> PlayAgainScreen(graph, coordinator)
                                                AppSection.Downloads -> DownloadsScreen(graph, coordinator)
                                                AppSection.Settings -> SettingsScreen(
                                                    graph,
                                                    signedIn = signedIn,
                                                    onLogin = openLogin,
                                                )
                                                AppSection.Feedback -> FeedbackScreen(graph, onDismiss = { onSection(AppSection.Discover) })
                                                AppSection.Catalogue ->
                                                    if (catalogue != null) catalogue() else ComingSoon(section)
                                                else -> ComingSoon(section)
                                            }
                                        }
                                    }
                                }
                                val mini: @Composable (Modifier) -> Unit = { mod ->
                                    DisposableEffect(Unit) { onDispose { miniHeightPx = 0 } }
                                    MiniPlayer(
                                        coordinator,
                                        surface = if (needsSurface && !nowPlayingOpen) graph.playerSurface else null,
                                        onExpand = { graph.pip.nowPlayingOpen.value = true },
                                        modifier = mod.onSizeChanged { miniHeightPx = it.height },
                                    )
                                }
                                if (desktopLayoutHole) {
                                    Column(Modifier.fillMaxSize()) {
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .padding(padding)
                                                .background(DesignColors.Surface),
                                        ) { CompositionLocalProvider(LocalNestedBack provides nestedBack) { screens() } }
                                        if (!railWindow) mini(Modifier.fillMaxWidth())
                                    }
                                } else {
                                    val miniVisible = !railWindow && coordinator.player.queue.collectAsState().value.currentItem != null
                                    val miniInset = if (miniVisible) with(LocalDensity.current) { miniHeightPx.toDp() } else 0.dp
                                    val contentEndsAboveMini = rememberTouchExplorationEnabled()
                                    Box(Modifier.fillMaxSize()) {
                                        Column(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(padding)
                                                .padding(bottom = if (contentEndsAboveMini) miniInset else 0.dp),
                                        ) {
                                            CompositionLocalProvider(
                                                LocalMiniPlayerInset provides if (contentEndsAboveMini) 0.dp else miniInset,
                                                LocalNestedBack provides nestedBack,
                                            ) { screens() }
                                        }
                                        if (!railWindow) {
                                            mini(
                                                Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .navigationBarsPadding(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val latestShellContent by rememberUpdatedState(shellContent)
                    val movableShell = remember { movableContentOf { latestShellContent() } }
                    if (!shellHidden) shellFrame(movableShell)
                }
                UpdateDialog(graph, recheck = updateRecheck)
                UpgradeSnackbar(graph, snackbarHostState)
                if (checkingUpdate) {
                    CheckUpdateDialog(graph) { found ->
                        checkingUpdate = false
                        if (found) updateRecheck++
                    }
                }
            }
            CompositionLocalProvider(
                LocalSnackbar provides showSnackbar,
                LocalScanController provides scanController,
                LocalDownloadLauncher provides downloadLauncher,
                LocalAvailableOffline provides availableOffline,
                LocalYtPlaylistActions provides ytPlaylistActions,
                LocalCaptchaResolver provides captchaResolver,
                LocalSignInResolver provides signInResolver,
                *songActions.provided(),
            ) {
                if (nowPlayingOpen) {
                    NowPlayingScreen(
                        graph,
                        coordinator,
                        onClose = {
                            graph.pip.nowPlayingOpen.value = false
                            nowPlayingPeeking = false
                        },
                        onPeek = { nowPlayingPeeking = it > 0.5f },
                        onLogin = {
                            graph.pip.nowPlayingOpen.value = false
                            nowPlayingPeeking = false
                            openLogin()
                        },
                        onOpenChannel = { url, title ->
                            graph.pip.nowPlayingOpen.value = false
                            nowPlayingPeeking = false
                            openItem(
                                YtFeedItem(
                                    kind = YtFeedItem.KIND_CHANNEL,
                                    id = url,
                                    title = title,
                                    channelId = url,
                                    url = url,
                                ),
                            )
                        },
                    )
                }
                songActions.Dialogs()
                DownloadDialogHost(graph, pendingDownload) { pendingDownload = null }
                QuickDownloadHost(graph, pendingQuickDownload) { pendingQuickDownload = null }
                QuickDownloadHost(graph, pendingFormatDownload?.first, pendingFormatDownload?.second) { pendingFormatDownload = null }
                YtPlaylistHost(graph, ytPlaylistRequest) { ytPlaylistRequest = null }
            }
            Box(Modifier.fillMaxSize()) {
                val snackbarLift = if (nowPlayingOpen) 0.dp else with(LocalDensity.current) { miniHeightPx.toDp() }
                SnackbarHost(
                    snackbarHostState,
                    Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = snackbarLift),
                ) { data -> SwipeableSnackbar(data) }
            }
        }
    }
}

@Composable
private fun SwipeableSnackbar(data: SnackbarData) {
    key(data) {
        val swipeState = rememberSwipeToDismissBoxState()
        LaunchedEffect(swipeState.currentValue) {
            if (swipeState.currentValue != SwipeToDismissBoxValue.Settled) data.dismiss()
        }
        SwipeToDismissBox(swipeState, backgroundContent = {}) { Snackbar(data) }
    }
}

enum class DiscoverTab(val id: Int, val label: StringResource, val icon: ImageVector, val hasFeed: Boolean) {
    Home(1, Res.string.tab_home, Icons.Outlined.Home, hasFeed = true),
    Music(2, Res.string.tab_music, Icons.Outlined.MusicNote, hasFeed = true),
    Recommended(3, Res.string.tab_recommended, Icons.Default.StarBorder, hasFeed = true),
    TopTracks(4, Res.string.tab_top_tracks, Icons.AutoMirrored.Outlined.TrendingUp, hasFeed = true),
    Account(5, Res.string.tab_account_library, Icons.Outlined.Person, hasFeed = false),
}

internal fun discoverTabsFor(graph: AppGraph): List<DiscoverTab> =
    DiscoverTab.entries.filter { !graph.guest || (it != DiscoverTab.Account && it != DiscoverTab.TopTracks) }

@Composable
internal fun DiscoverTabRow(
    selected: DiscoverTab,
    inline: Boolean = false,
    dragProgress: Float = 0f,
    tabs: List<DiscoverTab> = DiscoverTab.entries,
    onSelect: (DiscoverTab) -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bounds = remember(rtl) { mutableStateMapOf<DiscoverTab, Pair<Dp, Dp>>() }
    val activeTabFocus = remember { FocusRequester() }
    BoxWithConstraints(if (inline) Modifier else Modifier.fillMaxWidth()) {
        LaunchedEffect(Unit) {
            if (selected in tabs) activeTabFocus.requestFocus()
        }
        val inlineTabWidth = (maxWidth / tabs.size.coerceAtLeast(1)).coerceAtMost(DiscoverInlineTabMaxWidth)
        Column(if (inline) Modifier else Modifier.fillMaxWidth()) {
            Row((if (inline) Modifier else Modifier.fillMaxWidth()).listKeyboardNavigation(ListOrientation.Horizontal)) {
                tabs.forEach { t ->
                    val active = t == selected
                    val label = stringResource(t.label)
                    Box(
                        (if (inline) Modifier.width(inlineTabWidth) else Modifier.weight(1f))
                            .onGloballyPositioned { c ->
                                val parent = c.parentLayoutCoordinates
                                val center = (parent?.localPositionOf(c)?.x ?: 0f) + c.size.width / 2f
                                val x = if (rtl && parent != null) parent.size.width - center else center
                                bounds[t] = with(density) { x.toDp() to c.size.width.toDp() }
                            }
                            .then(if (inline) Modifier.clip(RoundedCornerShape(12.dp)) else Modifier)
                            .then(if (active) Modifier.focusRequester(activeTabFocus) else Modifier)
                            .a11yActionLabel(label)
                            .semantics {
                                this.selected = active
                                role = Role.Tab
                            }
                            .clickable { onSelect(t) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            t.icon,
                            null,
                            Modifier.size(24.dp),
                            tint = if (active) DesignColors.Accent else DesignColors.InkMuted,
                        )
                    }
                }
            }
            val cur = bounds[selected]
            val i = tabs.indexOf(selected)
            val neighbor = tabs.getOrNull((i + if (dragProgress > 0f) 1 else -1).coerceIn(0, tabs.lastIndex))?.let { bounds[it] }
            val frac = abs(dragProgress) * TabIndicatorDragTravel
            val targetCenter = if (cur != null && neighbor != null) lerp(cur.first, neighbor.first, frac) else cur?.first ?: 0.dp
            val center by animateDpAsState(targetCenter, label = "discoverIndicator")
            val indicatorWidth = cur?.second ?: 0.dp
            Box(
                Modifier
                    .offset(x = center - indicatorWidth / 2)
                    .width(indicatorWidth)
                    .height(3.dp)
                    .background(DesignColors.Accent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
            )
        }
    }
}

private val DiscoverInlineTabMaxWidth = 88.dp

@Composable
private fun DiscoverFeed(
    graph: AppGraph,
    coordinator: PlaybackCoordinator,
    tab: DiscoverTab,
    localeEpoch: Int,
    loginEpoch: Int,
    sessions: MutableMap<String, FeedSession>,
    scope: CoroutineScope,
    signedIn: Boolean,
    profileReady: Boolean,
    account: YtAccountInfo?,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
    onSwitchAccount: () -> Unit,
    onOpen: ((YtFeedItem) -> Unit)? = null,
    onDeeplink: ((link: String, title: String?) -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
) {
    if (tab == DiscoverTab.Account) {
        if (!profileReady) {
            CenteredBox { CircularProgressIndicator() }
        } else if (signedIn) {
            val session = remember(tab, localeEpoch, loginEpoch) {
                sessions.getOrPut(tab.name) {
                    val log = graph.logger?.fork("AccountLibrary")
                    FeedSession(scope, log) { continuation ->
                        graph.engines.extractAccountLibrary(continuation = continuation, logger = log)
                    }.also { it.pager.refresh() }
                }
            }
            AccountLibraryScreen(
                session,
                coordinator,
                account,
                onSignOut = onSignOut,
                onSwitchAccount = onSwitchAccount,
                onOpen = onOpen,
                onDeeplink = onDeeplink,
            )
        } else {
            LoginPreface(onLogin)
        }
    } else if (!tab.hasFeed) {
        CenteredBox {
            Text(
                stringResource(Res.string.discover_needs_account, stringResource(tab.label)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        val localScope = rememberCoroutineScope()
        val localFeed = tab == DiscoverTab.Recommended || tab == DiscoverTab.TopTracks
        val session = remember(tab, localeEpoch, loginEpoch) {
            if (localFeed) {
                FeedSession(localScope, graph.logger?.fork(tab.name)) {
                    when (tab) {
                        DiscoverTab.TopTracks -> YtFeedPage(
                            sections = listOf(
                                YtSection(items = graph.playHistory?.youtubeTopChart().orEmpty()),
                            ),
                        )
                        else -> graph.engines.getRecommendedFeed()
                    }
                }.also { it.pager.refresh() }
            } else {
                sessions.getOrPut(tab.name) {
                    val log = graph.logger?.fork(tab.name)
                    val home: suspend (String?, Boolean) -> YtFeedPage = { continuation, refresh ->
                        val page = graph.engines.extractHomeFeed(continuation = continuation, logger = log, refresh = refresh)
                        page.copy(sections = page.sections.map { it.copy(items = rankMusicFirst(it.items)) })
                    }
                    FeedSession(
                        scope,
                        log,
                        reloadFetch = if (tab == DiscoverTab.Music) null else { continuation -> home(continuation, true) },
                    ) { continuation ->
                        when (tab) {
                            DiscoverTab.Music -> graph.engines.extractTrendingMusicFeed(logger = log)
                            else -> home(continuation, false)
                        }
                    }.also { it.pager.refresh() }
                }
            }
        }
        val removeFromTopTracks: ((YtFeedItem) -> Unit)? =
            if (tab == DiscoverTab.TopTracks) {
                { item ->
                    localScope.launch {
                        graph.playHistory?.deleteYoutube(item.id)
                        session.pager.reload()
                    }
                }
            } else {
                null
            }
        CompositionLocalProvider(LocalRemoveFromTopTracks provides removeFromTopTracks) {
            FeedList(
                session.pager,
                coordinator,
                emptyMessage = stringResource(
                    when (tab) {
                        DiscoverTab.Music -> Res.string.discover_no_trending
                        DiscoverTab.Recommended -> Res.string.discover_no_recommendations
                        DiscoverTab.TopTracks -> Res.string.discover_no_top_tracks
                        else -> Res.string.discover_no_anonymous_home
                    },
                ),
                emptyAction = if (tab == DiscoverTab.Home && onSearch != null) {
                    EmptyAction(stringResource(Res.string.search_videos), onSearch)
                } else {
                    null
                },
                onOpen = onOpen,
                onPlayItem = if (tab == DiscoverTab.TopTracks) {
                    { tapped ->
                        val videos = (session.pager.state.value as? Pager.State.Ready)
                            ?.sections.orEmpty()
                            .flatMap { it.items }
                            .filter { it.kind == YtFeedItem.KIND_VIDEO }
                        coordinator.playAll(
                            videos,
                            startIndex = videos.indexOfFirst { it.id == tapped.id }.coerceAtLeast(0),
                        )
                    }
                } else {
                    null
                },
                listState = session.listState,
                onDeeplink = onDeeplink,
            )
        }
    }
}

@Composable
private fun ComingSoon(section: AppSection) {
    CenteredBox {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(section.icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(Res.string.coming_soon, stringResource(section.label)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun keyboardOverlap(): Dp {
    val density = LocalDensity.current
    val withKeyboard = WindowInsets.safeDrawing.getBottom(density)
    val withoutKeyboard = WindowInsets.systemBars.union(WindowInsets.displayCutout).getBottom(density)
    return with(density) { (withKeyboard - withoutKeyboard).coerceAtLeast(0).toDp() }
}
