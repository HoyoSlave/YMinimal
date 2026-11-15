package io.ymusic.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.car.app.connection.CarConnection
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import io.ymusic.player.Player
import io.ymusic.player.isPlaying
import io.ymusic.settings.AppSettings
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.flow
import kmpcommon.log.Logger
import kmpcommon.log.info
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AudioFocusCoordinator(
    private val context: Context,
    private val player: Player,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
    private val log: Logger,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false

    private var inFocusStack = false
    private var resumeOnGain = false
    private var ducked = false
    private var noisyRegistered = false
    private var resumeAfterCall = false

    private fun playing(): Boolean = isPlaying(player.status.value, player.queue.value)

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        log.info("focus change ${focusChangeName(change)} playing=${playing()} resumeOnGain=$resumeOnGain ducked=$ducked")
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasFocus = false
                inFocusStack = false
                resumeOnGain = false
                restoreDuck()
                if (playing()) player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                if (playing()) {
                    resumeOnGain = true
                    player.pause()
                }
            }
            // Still the focus holder while ducked: a request now would take GAIN back from the ducking app.
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duck()
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasFocus = true
                restoreDuck()
                if (resumeOnGain) {
                    resumeOnGain = false
                    player.play()
                }
            }
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY && playing()) {
                log.info("becoming noisy, pause")
                player.pause()
            }
        }
    }

    fun start() {
        scope.launch {
            combine(player.status, player.queue) { status, queue -> isPlaying(status, queue) }
                .distinctUntilChanged()
                .collect { playing -> if (playing) registerNoisy() else unregisterNoisy() }
        }
        scope.launch {
            combine(settings.flow(AppSettings.audioFocus), carConnected()) { mode, inCar ->
                log.info("mode $mode car=$inCar")
                mode != AppSettings.AUDIO_FOCUS_CALLS || inCar
            }
                .distinctUntilChanged()
                .collectLatest { followFocus -> if (followFocus) followFocus() else pauseForCalls() }
        }
    }

    private fun carConnected(): Flow<Boolean> = callbackFlow {
        val type = CarConnection(context).type
        val observer = Observer<Int> { trySend(it != CarConnection.CONNECTION_TYPE_NOT_CONNECTED) }
        type.observeForever(observer)
        awaitClose { type.removeObserver(observer) }
    }.flowOn(Dispatchers.Main.immediate).distinctUntilChanged()

    private suspend fun followFocus() {
        try {
            combine(player.status, player.queue) { status, queue ->
                when {
                    queue.currentItem == null -> false
                    isPlaying(status, queue) -> true
                    else -> null
                }
            }.distinctUntilChanged().collect { want ->
                when (want) {
                    true -> requestFocus()
                    false -> abandonFocus()
                    null -> Unit
                }
            }
        } finally {
            restoreDuck()
            abandonFocus()
        }
    }

    private suspend fun pauseForCalls() {
        val unregister = if (Build.VERSION.SDK_INT >= 31) watchAudioMode() else watchCallState()
        try {
            awaitCancellation()
        } finally {
            unregister()
            resumeAfterCall = false
        }
    }

    private fun onCallChanged(inCall: Boolean) {
        log.info("call inCall=$inCall playing=${playing()} resumeAfterCall=$resumeAfterCall")
        if (inCall) {
            if (playing()) {
                resumeAfterCall = true
                player.pause()
            }
        } else if (resumeAfterCall) {
            resumeAfterCall = false
            if (!playing()) player.play()
        }
    }

    @RequiresApi(31)
    private fun watchAudioMode(): () -> Unit {
        val listener = AudioManager.OnModeChangedListener { mode -> onCallChanged(mode != AudioManager.MODE_NORMAL) }
        audioManager.addOnModeChangedListener(ContextCompat.getMainExecutor(context), listener)
        return { audioManager.removeOnModeChangedListener(listener) }
    }

    @Suppress("DEPRECATION")
    private fun watchCallState(): () -> Unit {
        val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val listener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                onCallChanged(state != TelephonyManager.CALL_STATE_IDLE)
            }
        }
        telephony.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
        return { telephony.listen(listener, PhoneStateListener.LISTEN_NONE) }
    }

    private fun requestFocus() {
        if (hasFocus) return
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val request = focusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setOnAudioFocusChangeListener(focusListener)
                .setWillPauseWhenDucked(false)
                .build()
                .also { focusRequest = it }
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (hasFocus) {
            inFocusStack = true
            resumeOnGain = false
            restoreDuck()
        }
        log.info("focus request result=$result")
    }

    private fun abandonFocus() {
        if (!inFocusStack) return
        if (Build.VERSION.SDK_INT >= 26) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        hasFocus = false
        inFocusStack = false
        resumeOnGain = false
        log.info("focus abandoned")
    }

    private fun duck() {
        if (ducked) return
        ducked = true
        log.info("duck")
        player.setDucked(true)
    }

    private fun restoreDuck() {
        if (!ducked) return
        ducked = false
        log.info("unduck")
        player.setDucked(false)
    }

    private fun registerNoisy() {
        if (noisyRegistered) return
        ContextCompat.registerReceiver(
            context,
            noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        noisyRegistered = true
    }

    private fun unregisterNoisy() {
        if (!noisyRegistered) return
        context.unregisterReceiver(noisyReceiver)
        noisyRegistered = false
    }

    private fun focusChangeName(change: Int): String = when (change) {
        AudioManager.AUDIOFOCUS_GAIN -> "gain"
        AudioManager.AUDIOFOCUS_LOSS -> "loss"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> "loss-transient"
        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> "loss-can-duck"
        else -> change.toString()
    }
}
