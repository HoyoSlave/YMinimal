package io.ymusic.ui

internal sealed interface Deeplink {
    val url: String

    data class Playlist(override val url: String) : Deeplink

    data class Channel(override val url: String) : Deeplink

    data class Feed(override val url: String) : Deeplink
}

internal fun parseDeeplink(link: String): Deeplink? = when {
    link.startsWith("/playlist?") -> Deeplink.Playlist(link)
    link.startsWith("/channel/") && link.length > "/channel/".length -> Deeplink.Channel(link)
    link.startsWith("/feed/") && link.length > "/feed/".length -> Deeplink.Feed(link)
    else -> null
}
