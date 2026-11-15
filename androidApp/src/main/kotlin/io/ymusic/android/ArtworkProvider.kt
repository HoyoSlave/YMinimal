package io.ymusic.android

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.ymusic.ui.NOW_PLAYING_ART_PX
import io.ymusic.ui.artImageRequest
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.ByteString.Companion.decodeHex
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import okio.Path
import java.io.FileNotFoundException
import java.security.MessageDigest
import java.security.SecureRandom

class ArtworkProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("artwork is read-only")
        val context = requireNotNull(context)
        val target = uri.getQueryParameter(ARTWORK_URI_PARAM)
            ?.takeIf { verifySignature(context, it, uri.getQueryParameter(ARTWORK_SIG_PARAM)) }
            ?.takeIf { isAllowedArtworkTarget(it) }
            ?: throw FileNotFoundException("refused artwork target: $uri")
        var firstMiss: FileNotFoundException? = null
        repeat(2) { attempt ->
            val path = resolve(target)
            try {
                return ParcelFileDescriptor.open(path.toFile(), ParcelFileDescriptor.MODE_READ_ONLY)
            } catch (e: FileNotFoundException) {
                if (attempt == 1) { // evicted twice in a row: give up
                    firstMiss?.let(e::addSuppressed)
                    throw e
                }
                firstMiss = e
            }
        }
        throw FileNotFoundException(target) // unreachable
    }

    private fun resolve(target: String): Path {
        val app = requireNotNull(context).applicationContext as YMusicApplication
        try {
            return runBlocking {
                app.services.await().imageLoader.loadFile(artImageRequest(target, ARTWORK_MAX_PX, ARTWORK_MAX_PX))
            } ?: throw FileNotFoundException("no scaled disk cache to serve artwork from")
        } catch (e: FileNotFoundException) {
            throw e
        } catch (e: Exception) {
            // FileNotFoundException is the one exception a ContentProvider open may throw; keep the
            // real cause on its chain instead of flattening it into the message.
            throw FileNotFoundException("artwork load failed for $target").apply { initCause(e) }
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("artwork is read-only")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("artwork is read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("artwork is read-only")
}

fun artworkAuthority(context: Context): String = "${context.packageName}.artwork"

fun artworkContentUri(context: Context, url: String): Uri =
    Uri.Builder()
        .scheme("content")
        .authority(artworkAuthority(context))
        .path("art")
        .appendQueryParameter(ARTWORK_URI_PARAM, url)
        .appendQueryParameter(ARTWORK_SIG_PARAM, artworkSignature(context, url))
        .build()

internal fun artworkSignature(context: Context, target: String): String =
    target.encodeUtf8().hmacSha256(artworkSalt(context)).hex()

internal fun verifySignature(context: Context, target: String, sig: String?): Boolean {
    if (sig == null) return false
    val expected = artworkSignature(context, target)
    return MessageDigest.isEqual(sig.lowercase().encodeToByteArray(), expected.encodeToByteArray())
}

private fun artworkSalt(context: Context): ByteString {
    cachedSalt?.let { return it }
    synchronized(saltLock) {
        cachedSalt?.let { return it }
        val prefs = context.getSharedPreferences("artwork_provider", Context.MODE_PRIVATE)
        val salt = prefs.getString(SALT_KEY, null)?.decodeHex()
            ?: ByteArray(32).also { SecureRandom().nextBytes(it) }.toByteString()
                .also { prefs.edit().putString(SALT_KEY, it.hex()).apply() }
        cachedSalt = salt
        return salt
    }
}

@Volatile private var cachedSalt: ByteString? = null
private val saltLock = Any()
private const val SALT_KEY = "salt"

internal fun isAllowedArtworkTarget(target: String): Boolean {
    val uri = Uri.parse(target)
    return when (uri.scheme) {
        "art" -> uri.authority == "pic" && uri.lastPathSegment?.toLongOrNull() != null
        "https" -> {
            val host = uri.host ?: return false
            ALLOWED_IMAGE_HOSTS.any { host == it || host.endsWith(".$it") }
        }
        else -> false
    }
}

internal const val ARTWORK_URI_PARAM = "uri"
internal const val ARTWORK_SIG_PARAM = "sig"

internal const val ARTWORK_MAX_PX = NOW_PLAYING_ART_PX

private val ALLOWED_IMAGE_HOSTS = listOf("ytimg.com", "ggpht.com", "googleusercontent.com")
