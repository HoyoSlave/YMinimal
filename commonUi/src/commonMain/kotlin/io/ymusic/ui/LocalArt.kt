package io.ymusic.ui

import androidx.compose.ui.unit.IntSize
import io.ymusic.imageloader.DecodedImage
import io.ymusic.imageloader.ImageFetcher
import io.ymusic.imageloader.ImageLoadException
import io.ymusic.imageloader.ImageLoader
import io.ymusic.imageloader.ImageRequest
import io.ymusic.imageloader.ImageScale
import io.ymusic.imageloader.ImageSourceCache
import io.ymusic.imageloader.decodeChecked
import io.ymusic.imageloader.decodeScaled
import io.ymusic.imageloader.decodeScaledFile
import io.ymusic.imageloader.sourceCacheKey
import io.ymusic.mediastore.BetterMediaStore
import io.ymusic.mediastore.model.MediaStoreItem
import kmpcommon.uri.Uri
import kmpcommon.uri.UriBuilder
import kmpffmpeg.metadata.FfmpegMetadataGetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.Source
import okio.use

fun localArtUrl(item: MediaStoreItem): String? =
    if (item.thumbnailStreamIndex != null || item.videoCodec != null || item.youtubeVid != null) {
        UriBuilder().scheme("art").authority("pic").appendPathSegment(item.id.toString()).buildString()
    } else {
        null
    }

fun youtubeThumbUrl(videoId: String): String = "https://i.ytimg.com/vi/$videoId/mqdefault.jpg"

fun youtubeArtUrl(videoId: String): String =
    UriBuilder().scheme("art").authority("yt").appendPathSegment(videoId).buildString()

fun fileArtUrl(uri: String, hasCover: Boolean, youtubeVid: String?, hasVideo: Boolean): String? = when {
    hasCover -> fileArtUrl(uri, ART_KIND_COVER)
    youtubeVid != null -> youtubeArtUrl(youtubeVid)
    hasVideo -> fileArtUrl(uri, ART_KIND_FRAME)
    else -> null
}

private fun fileArtUrl(uri: String, kind: String): String =
    UriBuilder().scheme("art").authority("file").appendQueryParam("kind", kind).appendQueryParam("uri", uri).buildString()

private const val ART_KIND_COVER = "cover"
private const val ART_KIND_FRAME = "frame"

fun interface YoutubeArt {
    suspend fun load(videoId: String, size: IntSize, scale: ImageScale): DecodedImage
}

fun loaderYoutubeArt(loader: () -> ImageLoader): YoutubeArt = YoutubeArt { videoId, size, scale ->
    val bitmap = loader().load(ImageRequest(youtubeImageKey(videoId), youtubeThumbnailSources(videoId), size, scale))
    DecodedImage(bitmap, bitmap.width, bitmap.height)
}

fun mediaStoreArtFetcher(
    getter: FfmpegMetadataGetter,
    store: BetterMediaStore,
    youtubeArt: YoutubeArt,
    coverCache: ImageSourceCache? = null,
    maxImageBytes: Long = 32L * 1024 * 1024,
): ImageFetcher = ImageFetcher { uri, size, scale ->
    if (uri.scheme?.toString() != "art") return@ImageFetcher null
    val url = uri.toString()
    if (uri.authority?.toString() == "yt") {
        val videoId = uri.path.toString().substringAfterLast('/').takeIf { it.isNotEmpty() }
            ?: throw ImageLoadException(ImageLoadException.Reason.Unsupported, "malformed art uri: $url")
        return@ImageFetcher youtubeArt.load(videoId, size, scale)
    }
    if (uri.authority?.toString() == "file") {
        val file = uri.queryParamValue("uri")?.let { Uri.parse(it) }
            ?: throw ImageLoadException(ImageLoadException.Reason.Unsupported, "malformed art uri: $url")
        val source = if (uri.queryParamValue("kind")?.toString() == ART_KIND_COVER) getter.getAttachedPic(file) else getter.extractThumbnail(file)
        val bytes = readCapped(source, maxImageBytes, url)
        return@ImageFetcher decodeChecked(url) { decodeScaled(bytes, size, scale) }
    }
    if (uri.authority?.toString() != "pic") return@ImageFetcher null
    val id = uri.path.toString().substringAfterLast('/').toLongOrNull()
        ?: throw ImageLoadException(ImageLoadException.Reason.Unsupported, "malformed art uri: $url")
    val item = store.findItem(id)
        ?: throw ImageLoadException(ImageLoadException.Reason.NotFound, "no media item for art uri: $url")
    val youtubeVid = item.youtubeVid
    val kind = when {
        item.thumbnailStreamIndex != null -> "cover"
        youtubeVid != null -> return@ImageFetcher youtubeArt.load(youtubeVid, size, scale)
        item.videoCodec != null -> "frame"
        else -> throw ImageLoadException(ImageLoadException.Reason.NotFound, "media item $id has no artwork source: $url")
    }
    suspend fun extract(): Source = if (kind == "cover") getter.getAttachedPic(item.uri) else getter.extractThumbnail(item.uri)
    if (coverCache != null) {
        val key = sourceCacheKey("$kind:${item.uri}:${item.fileSize}:${item.lastModified}")
        val file = coverCache.filePath(key) ?: extract().use { coverCache.write(key, it, maxImageBytes, url) }
        return@ImageFetcher decodeChecked(url) { decodeScaledFile(coverCache.fileSystem, file, size, scale) }
    }
    // ByteArray: the in-memory platform decoders behind decodeScaled are bytes-only (Skia codec),
    // and embedded art is small — the cap guards a mislabeled stream.
    val bytes = readCapped(extract(), maxImageBytes, url)
    decodeChecked(url) { decodeScaled(bytes, size, scale) }
}

private suspend fun readCapped(source: Source, maxBytes: Long, url: String): ByteArray =
    withContext(Dispatchers.IO) {
        source.use { src ->
            val buffer = Buffer()
            while (true) {
                if (buffer.size > maxBytes) {
                    throw ImageLoadException(ImageLoadException.Reason.Undecodable, "embedded art exceeds the $maxBytes-byte cap: $url")
                }
                if (src.read(buffer, 64L * 1024) == -1L) break
            }
            buffer.readByteArray()
        }
    }
