package io.ymusic.ui

import io.ymusic.download.ResolvedVideo
import io.ymusic.download.SabrSpecBuilder
import io.ymusic.download.SabrSpecResolver
import io.ymusic.player.YtEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class YtEngineSabrSpecResolver(
    private val engine: suspend () -> YtEngine,
) : SabrSpecResolver {

    private val mutex = Mutex()

    override suspend fun resolve(videoId: String): ResolvedVideo = mutex.withLock {
        val player = engine()
        withContext(Dispatchers.Main) { player.init() }
        val playerResponse = player.requestPlayerResponse(videoId)
        val poToken = player.requestPoToken(videoId)
        SabrSpecBuilder.resolve(videoId, playerResponse, poToken)
    }

    suspend fun playerResponse(videoId: String): String = mutex.withLock {
        val player = engine()
        withContext(Dispatchers.Main) { player.init() }
        player.requestPlayerResponse(videoId)
    }
}
