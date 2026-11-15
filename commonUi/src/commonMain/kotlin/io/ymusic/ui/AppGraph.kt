package io.ymusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import io.ymusic.dynamicengine.DynamicEngineManager
import io.ymusic.dynamicengine.yt.handleWebViewPlayNext
import io.ymusic.imageloader.ImageLoader
import io.ymusic.mediastore.BetterMediaStore
import io.ymusic.player.LoopMode
import io.ymusic.player.PlayableItem
import io.ymusic.player.Player
import io.ymusic.player.VideoQualityState
import io.ymusic.remoteconfig.ApiService
import io.ymusic.remoteconfig.RemoteConfigData
import io.ymusic.remoteconfig.StatUsage
import io.ymusic.settings.AppSettings
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.flow
import io.ymusic.settings.mapState
import io.ymusic.settings.put
import io.ymusic.ui.playhistory.PlayAgainRecorder
import io.ymusic.ui.playhistory.PlayHistoryStore
import io.ymusic.ui.sponsorblock.InMemorySponsorBlockStore
import io.ymusic.ui.sponsorblock.ServiceSponsorBlockApi
import io.ymusic.ui.sponsorblock.SponsorBlockApi
import io.ymusic.ui.sponsorblock.SponsorBlockCoordinator
import io.ymusic.ui.sponsorblock.SponsorBlockService
import io.ymusic.ui.sponsorblock.SponsorBlockStore
import kmpcommon.WorkerThread
import kmpcommon.log.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import projectcommon.error.DISK_FULL_CODE
import projectcommon.error.errorCode
import projectcommon.error.runSuspendCatching

const val PlaybackQualitySettingKey = "player.preferVideoQuality"
const val PlaybackAudioOnlyKey = "player.audioOnly"
const val PlaybackVolumeKey = "player.volume"
const val PlaybackSpeedKey = "player.speed"
const val PlaybackPitchKey = "player.pitch"
const val PlaybackEqKey = "player.eq"
const val PlaybackShuffleKey = "player.shuffle"
const val PlaybackLoopModeKey = "player.loopMode"

sealed interface ScanResult {
    data class Done(val scanned: Int, val added: Int, val updated: Int, val removed: Int, val failed: Int) : ScanResult

    data object NoFolders : ScanResult

    data object ExtractorMissing : ScanResult
}

typealias ScanAction = suspend (onProgress: (scanned: Int) -> Unit) -> ScanResult?

typealias RescanAction = suspend (folderUri: String?, onProgress: (scanned: Int) -> Unit) -> ScanResult?

class AppGraph(
    val engines: DynamicEngineManager,
    val player: Player,
    val mediaStore: BetterMediaStore? = null,
    val playHistory: PlayHistoryStore? = null,
    val guest: Boolean = false,
    val scanLibraryFolder: ScanAction? = null,
    val libraryFolderPicker: (suspend () -> String?)? = null,
    val canWriteFolder: (String) -> Boolean = { true },
    val probeDownloadFolder: suspend (String) -> FolderWriteProblem? = { null },
    val rescanLibrary: RescanAction? = null,
    val pruneUngrantedFolders: (suspend () -> Unit)? = null,
    val deleteFromDisk: (suspend (io.ymusic.mediastore.model.MediaStoreItem) -> Unit)? = null,
    val revealInFolder: (suspend (uri: String) -> Unit)? = null,
    val revealInFolderLabel: org.jetbrains.compose.resources.StringResource? = null,
    val shareText: ((text: String) -> Unit)? = null,
    val fileRenaming: FileRenaming? = null,
    val appVersion: String? = null,
    val appVersionName: String? = null,
    val appVersionCode: Int? = null,
    val runningBundles: io.ymusic.remoteconfig.RunningBundles? = null,
    val imageLoader: ImageLoader? = null,
    val logger: kmpcommon.log.Logger? = null,
    val settings: SettingsStore,
    val surfaceScalesDown: Boolean = false,
    val loginBrowser: LoginBrowser? = null,
    val cookieSignIn: CookieSignIn? = null,
    val exportSessionCookies: (suspend () -> List<SessionCookie>)? = null,
    val scanQrCode: (suspend () -> String?)? = null,
    val speechToText: (() -> kotlinx.coroutines.flow.Flow<SpeechEvent>)? = null,
    val confirmDeviceOwner: (suspend (title: String, subtitle: String) -> Boolean)? = null,
    val downloads: io.ymusic.download.DownloadService? = null,
    val captureFeedbackReport: (suspend (subject: String, message: String?) -> Unit)? = null,
    val platformInfo: String? = null,
    val createDocument: (suspend (suggestedName: String, mimeType: String) -> okio.Sink?)? = null,
    val openDocument: (suspend (mimeTypes: List<String>) -> okio.Source?)? = null,
    val devicePlaylists: DevicePlaylistSource? = null,
    val updates: AppUpdates? = null,
    val remoteConfig: SharedFlow<RemoteConfigData>? = null,
    val premium: StateFlow<Boolean> = MutableStateFlow(false),
    val openDonation: (() -> Unit)? = null,
    val stats: StatUsage? = null,
    val api: ApiService? = null,
    val windowResizing: MutableStateFlow<Boolean> = MutableStateFlow(false),
    val systemAudioEffects: Boolean = false,
    val audioFocusSetting: Boolean = false,
    val playerCache: io.ymusic.ytmedia.cache.MediaCache? = null,
    sponsorBlockApi: SponsorBlockApi? = api?.let(::ServiceSponsorBlockApi),
    sponsorBlockStore: SponsorBlockStore = InMemorySponsorBlockStore(),
    // Kept last so shells pass it as the trailing lambda.
    val playerSurface: @Composable (modifier: Modifier) -> Unit,
) {
    val sponsorBlock = SponsorBlockService(sponsorBlockApi, sponsorBlockStore, settings, logger)

    val quickDownload: StateFlow<QuickDownloadPreset> =
        settings.flow(AppSettings.quickDownloadPreset).mapState { QuickDownloadPreset.decode(it, downloads?.ffmpegAvailable == true) }

    suspend fun saveQuickDownload(preset: QuickDownloadPreset) = settings.put(AppSettings.quickDownloadPreset, preset.encode())
    val pip = PipCoordinator(player, settings)

    val temporaryUris = MutableStateFlow<Set<String>>(emptySet())

    internal val upgradeNotice by lazy { UpgradeNotice(settings, appVersionCode) }

    val playback by lazy {
        val onFailure = CoroutineExceptionHandler { _, e -> logger?.fork("playback")?.captureError(e, "playback coordinator failed") }
        PlaybackCoordinator(this, CoroutineScope(SupervisorJob() + Dispatchers.Default + onFailure))
    }

    internal val processScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, e -> logger?.fork("app")?.captureError(e, "process-scope job failed") },
    )

    internal val account by lazy { AccountController(this, processScope) }

    internal val scans by lazy { ScanController(processScope, logger?.fork("library-scan")) }

    private val appliedUiLanguage = MutableStateFlow(uiLanguageTag(settings).also(::applyUiLocale))

    val uiLanguage: StateFlow<String> = appliedUiLanguage

    fun reapplyUiLanguage() = applyUiLocale(appliedUiLanguage.value)

    init {
        // Settings reach the player, cache and locale here, not in App: the process also runs without the UI
        // (Android Auto, media buttons, the playback service).
        val bindings = processScope
        restoreUiLocale = { noRules, alreadyRestored ->
            val log = logger?.fork("ui-locale")
            if (alreadyRestored) {
                log?.log(Logger.Level.WARN, "plural rules still missing after restoring the app language", noRules)
            } else {
                log?.log(Logger.Level.WARN, "platform locale has no plural rules", noRules)
                reapplyUiLanguage()
            }
        }
        bindings.launch {
            settings.flow(AppSettings.uiLanguage).map { uiLanguageTag(settings) }.distinctUntilChanged().collect { tag ->
                applyUiLocale(tag)
                appliedUiLanguage.value = tag
            }
        }
        bindPlayerConfig(bindings)
        bindings.launch { pip.pictureHidden.collect { player.setVideoHidden(it) } }
        bindPlayerCacheLimit(bindings)
        SponsorBlockCoordinator(this, bindings)
        bindings.launch {
            player.watchNextSeen.collect { seen ->
                // The Recommended feed's raw material; a failed hand-off must never disturb playback.
                runSuspendCatching { engines.handleWebViewPlayNext(seen.videoId, seen.contentsJson) }
                    .onFailure { logger?.fork("watch-next-harvest")?.log(Logger.Level.WARN, "harvest hand-off failed", it) }
            }
        }
        // Process-lifetime like the graph itself: every queue is recorded, whatever screen is up.
        playHistory?.takeUnless { guest }?.let {
            PlayAgainRecorder(player, it.playAgain, logger?.fork("play-again")) { item ->
                item is PlayableItem.LocalMedia && item.uri in temporaryUris.value
            }
                .start(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        }
        bindings.launch {
            val savedLoop = settings.string(PlaybackLoopModeKey, "").value
            player.setLoopMode(LoopMode.entries.firstOrNull { it.name == savedLoop } ?: LoopMode.None)
            player.setShuffle(settings.string(PlaybackShuffleKey, "0").value == "1")
            player.queue.map { it.shuffle to it.loopMode }.distinctUntilChanged().collect { (shuffle, loop) ->
                settings.putString(PlaybackShuffleKey, if (shuffle) "1" else "0")
                settings.putString(PlaybackLoopModeKey, loop.name)
            }
        }
    }

    private fun bindPlayerConfig(scope: CoroutineScope) {
        scope.launch {
            val quality = settings.string(PlaybackQualitySettingKey, VideoQualityState.AUTO).value
            if (quality != VideoQualityState.AUDIO_ONLY) player.selectVideoQuality(quality)
            player.setAudioOnly(settings.string(PlaybackAudioOnlyKey, "1").value != "0")
            player.volume = settings.string(PlaybackVolumeKey, "1").value.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
            player.speed = settings.string(PlaybackSpeedKey, "1").value.toFloatOrNull()?.coerceIn(0.25f, 4f) ?: 1f
            player.setPitch(settings.string(PlaybackPitchKey, "1").value.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1f)
            settings.string(PlaybackEqKey, "").value.split(',').mapNotNull { it.toFloatOrNull() }.let { eq ->
                if (eq.size >= 12) {
                    player.setBassBoost(eq[0])
                    player.setVirtualizer(eq[1])
                    player.setEqBands(eq.drop(2).take(10))
                    eq.getOrNull(12)?.let { player.setPreamp(it) }
                }
            }
            launch {
                player.config.map { it.preferVideoQuality.first }.distinctUntilChanged()
                    .collect { settings.putString(PlaybackQualitySettingKey, it) }
            }
            launch {
                combine(player.config.map { it.audioOnly }, pip.demotedToAudioOnly) { audioOnly, demoted ->
                    audioOnly.takeUnless { demoted }
                }.filterNotNull().distinctUntilChanged()
                    .collect { settings.putString(PlaybackAudioOnlyKey, if (it) "1" else "0") }
            }
            launch {
                player.config.map { listOf(it.volume, it.speed, it.pitchFactor) to Triple(it.bassBoostDb, it.virtualizer, it.eqBands to it.preampDb) }
                    .distinctUntilChanged()
                    .collect { (levels, eq) ->
                        settings.putString(PlaybackVolumeKey, levels[0].toString())
                        settings.putString(PlaybackSpeedKey, levels[1].toString())
                        settings.putString(PlaybackPitchKey, levels[2].toString())
                        // bass, virtualizer, 10 bands, preamp — preamp last so 12-value saves still load.
                        settings.putString(PlaybackEqKey, "${eq.first},${eq.second},${eq.third.first.joinToString(",")},${eq.third.second}")
                    }
            }
        }
    }

    private fun bindPlayerCacheLimit(scope: CoroutineScope) {
        val cache = playerCache ?: return
        scope.launch {
            settings.flow(AppSettings.playerCacheMaxMb).collect { mb ->
                runSuspendCatching { withContext(Dispatchers.IO) { cache.blobs.setMaxSize(mb * 1024L * 1024L) } }
                    .onFailure { error ->
                        val log = logger?.fork("player-cache")
                        if (error.errorCode() == DISK_FULL_CODE) {
                            log?.log(Logger.Level.WARN, "player cache cap: storage is full", error)
                        } else {
                            log?.captureError(error, "player cache cap failed")
                        }
                    }
            }
        }
    }

    val videoFillScreen = mutableStateOf(false)

    val videoRotationLocked = mutableStateOf(false)

    val localeEpoch = MutableStateFlow(0)

    val refreshRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    fun requestRefresh() {
        refreshRequests.tryEmit(Unit)
    }
}

@WorkerThread
interface FileRenaming {
    fun nameOf(uri: String): String?

    fun rename(uri: String, newName: String): String
}
