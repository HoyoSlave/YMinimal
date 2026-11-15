package io.ymusic.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

internal const val ACTION_PLAY_FROM_SEARCH = "io.ymusic.android.playback.PLAY_FROM_SEARCH"

class VoiceSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val forward = Intent(this, PlaybackService::class.java).setAction(ACTION_PLAY_FROM_SEARCH)
        intent.extras?.let { forward.putExtras(it) }
        ContextCompat.startForegroundService(this, forward)
        finish()
    }
}
