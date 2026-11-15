package io.ymusic.ui

import io.ymusic.player.Player
import io.ymusic.player.VideoQualityState
import io.ymusic.player.isPlaying
import io.ymusic.settings.AppSettings
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.flow
import kotlinx.coroutines.flow.MutableStateFlow

class PipCoordinator(
    private val player: Player,
    private val settings: SettingsStore,
) {
    val nowPlayingOpen = MutableStateFlow(false)
    val videoFullscreen = MutableStateFlow(false)
    val inPip = MutableStateFlow(false)

    val demotedToAudioOnly = MutableStateFlow(false)

    val pictureHidden = MutableStateFlow(true)

    fun enablePip(): Boolean = settings.flow(AppSettings.enablePip).value

    fun playingVideo(): Boolean {
        if (player.config.value.audioOnly) return false
        val q = player.videoQuality.value
        if (q.preferredQuality == VideoQualityState.AUDIO_ONLY) return false
        if (q.actualQuality == VideoQualityState.AUDIO_ONLY) return false
        return isPlaying(player.status.value, player.queue.value)
    }

    fun autoEnterEligible(): Boolean {
        if (!enablePip()) return false
        if (!nowPlayingOpen.value && !videoFullscreen.value) return false
        return playingVideo()
    }

    fun onLeaveWithoutPip() {
        if (inPip.value) return
        if (playingVideo()) demoteToAudioOnly()
    }

    fun onWindowHidden() {
        demoteToAudioOnly()
    }

    fun onPipStarted() {
        inPip.value = true
    }

    fun onPipStopped(appInForeground: Boolean) {
        inPip.value = false
        if (appInForeground) {
            onEnterForeground()
            return
        }
        // X-close: pause so audio does not keep going in the background. AudioOnly so a
        // notification / lock-screen resume stays audio until the app is foregrounded.
        demoteToAudioOnly()
        player.pause()
    }

    private fun demoteToAudioOnly() {
        if (player.config.value.audioOnly) return
        if (player.videoQuality.value.preferredQuality == VideoQualityState.AUDIO_ONLY) return
        demotedToAudioOnly.value = true
        player.setAudioOnly(true)
    }

    fun onEnterForeground() {
        if (!demotedToAudioOnly.value) return
        player.setAudioOnly(false)
        demotedToAudioOnly.value = false
    }
}
