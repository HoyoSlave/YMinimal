package io.ymusic.ui

import kmphttp.AsyncInterceptor
import kmphttp.Cache
import kmphttp.DefaultPlatformCache
import kmphttp.HttpLogInterceptor
import kmphttp.KmpHttpClient
import kmphttp.create
import okio.FileSystem
import okio.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val DefaultHttpCacheBytes: Long = 32L * 1024 * 1024

class AppHttpClients(
    cacheDir: Path,
    maxSizeBytes: Long = DefaultHttpCacheBytes,
    fileSystem: FileSystem = FileSystem.SYSTEM,
    logger: kmpcommon.log.Logger? = null,
) {

    private val httpLogger = logger?.fork("kmphttp")
    private val diskCache: Cache = run {
        fileSystem.createDirectories(cacheDir)
        DefaultPlatformCache(cacheDir, maxSizeBytes)
    }

    private val base: KmpHttpClient = KmpHttpClient.create {
        defaultExecuteTimeout = APP_HTTP_TIMEOUT
        cache = diskCache
        this.logger = httpLogger // `logger` alone resolves to the constructor parameter
    }

    val engine: KmpHttpClient = base.newBuilder().apply {
        httpLogger?.let { interceptors += HttpLogInterceptor(it) }
    }.build()

    val image: KmpHttpClient = base.newBuilder().apply {
        cache = null
        networkInterceptors += AsyncInterceptor { chain ->
            val response = chain.proceed(chain.request)
            if (response.code == 404 && chain.request.url.toString().contains("/img/no_thumbnail")) {
                response.newBuilder().code(200).message("OK").build()
            } else {
                response
            }
        }
    }.build()
}

val APP_HTTP_TIMEOUT: Duration = 10.seconds
