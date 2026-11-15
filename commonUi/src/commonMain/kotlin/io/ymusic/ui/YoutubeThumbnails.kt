package io.ymusic.ui

import androidx.compose.ui.unit.IntSize
import io.ymusic.dynamicengine.yt.YtThumb
import io.ymusic.imageloader.ImageRequest
import io.ymusic.imageloader.ImageSource
import io.ymusic.imageloader.SourceCrop
import io.ymusic.imageloader.imageCacheKey

internal fun youtubeImageKey(videoId: String): String = "yt:$videoId"

internal fun youtubeVideoIdOf(url: String): String? = YTIMG_VIDEO.find(url)?.groupValues?.get(1)

internal fun youtubeThumbnailSources(videoId: String): List<ImageSource> = listOf(
    ImageSource(youtubeThumbUrl(videoId), 320, 180),
    ImageSource("https://i.ytimg.com/vi/$videoId/hqdefault.jpg", 480, 360, SourceCrop.Letterbox16x9),
    ImageSource("https://i.ytimg.com/vi/$videoId/sddefault.jpg", 640, 480, SourceCrop.Letterbox16x9),
)

internal fun artRequestSources(url: String): Pair<String, List<ImageSource>> {
    googleArtSources(url)?.let { return it }
    val videoId = youtubeVideoIdOf(url) ?: return imageCacheKey(url) to listOf(ImageSource(url))
    return youtubeImageKey(videoId) to youtubeThumbnailSources(videoId)
}

const val NOW_PLAYING_ART_PX = 640

fun artImageRequest(url: String, width: Int, height: Int): ImageRequest {
    val (key, sources) = artRequestSources(url)
    return ImageRequest(key, sources, IntSize(width, height))
}

internal fun thumbRequestSources(thumbs: List<YtThumb>): Pair<String, List<ImageSource>>? {
    if (thumbs.isEmpty()) return null
    val listed = thumbs.map { ImageSource(it.url, it.w ?: 0, it.h ?: 0) }
    val largest = thumbs.largestThumbUrl() ?: thumbs.last().url
    googleArtSources(largest)?.let { (key, sized) -> return key to (sized + listed).distinctBy { it.url } }
    val videoId = thumbs.firstNotNullOfOrNull { youtubeVideoIdOf(it.url) }
        ?: return imageCacheKey(largest) to listed
    return youtubeImageKey(videoId) to youtubeThumbnailSources(videoId) + listed
}

internal fun googleArtSources(url: String): Pair<String, List<ImageSource>>? {
    val match = GOOGLE_SIZED_ART.find(url) ?: return null
    val (prefix, width, middle, height, rest) = match.destructured
    val w = width.toIntOrNull()?.takeIf { it > 0 } ?: return null
    val h = height.toIntOrNull()?.takeIf { it > 0 } ?: return null
    val sources = GOOGLE_ART_SIZES.map { size ->
        val sizedHeight = (size.toLong() * h / w).toInt()
        ImageSource("$prefix$size$middle$sizedHeight$rest", size, sizedHeight)
    }
    return "$prefix$middle$rest" to sources
}

private val GOOGLE_ART_SIZES = listOf(120, 240, 480, 720)

private val GOOGLE_SIZED_ART = Regex("""^(https?://[^/]+\.googleusercontent\.com/[^=]+=w)(\d+)(.*?-h)(\d+)(.*)$""")

private val YTIMG_VIDEO = Regex("""^https?://i\d*\.ytimg\.com/vi(?:_webp)?/([A-Za-z0-9_-]{11})/""")
