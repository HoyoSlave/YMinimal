package io.ymusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.dynamicengine.yt.YtFeedItem
import io.ymusic.dynamicengine.yt.YtSectionHeader
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.a11y_downloaded
import io.ymusic.ui.resources.a11y_kind_album
import io.ymusic.ui.resources.a11y_kind_channel
import io.ymusic.ui.resources.a11y_kind_live
import io.ymusic.ui.resources.a11y_kind_mix
import io.ymusic.ui.resources.a11y_kind_music_video
import io.ymusic.ui.resources.a11y_kind_playlist
import io.ymusic.ui.resources.a11y_kind_video
import io.ymusic.ui.resources.badge_live
import io.ymusic.ui.resources.cd_collapse
import io.ymusic.ui.resources.cd_expand
import io.ymusic.ui.resources.cd_more
import io.ymusic.ui.resources.cd_view_all
import io.ymusic.ui.resources.chart_top_music_videos
import io.ymusic.ui.resources.chart_top_music_videos_daily
import io.ymusic.ui.resources.chart_top_music_videos_weekly
import io.ymusic.ui.resources.chart_top_songs
import io.ymusic.ui.resources.chart_top_songs_daily
import io.ymusic.ui.resources.chart_top_songs_on_shorts
import io.ymusic.ui.resources.chart_top_songs_on_shorts_daily
import io.ymusic.ui.resources.chart_top_songs_on_shorts_weekly
import io.ymusic.ui.resources.chart_top_songs_weekly
import io.ymusic.ui.resources.chart_trending_music_videos
import io.ymusic.ui.resources.play_all
import io.ymusic.ui.resources.section_subscribed_channels
import org.jetbrains.compose.resources.stringResource

internal val ListTitleStyle: TextStyle
    @Composable get() = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = DesignColors.Ink, lineHeight = 19.sp)
internal val ListTitleCompactStyle: TextStyle
    @Composable get() = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DesignColors.Ink, lineHeight = 17.5.sp)
internal val ListSubStyle: TextStyle
    @Composable get() = TextStyle(fontSize = 12.sp, color = DesignColors.InkSecondary)
internal val GridTitleStyle: TextStyle
    @Composable get() = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = DesignColors.Ink, lineHeight = 18.sp)
internal val GridSubStyle: TextStyle
    @Composable get() = TextStyle(fontSize = 12.sp, color = DesignColors.InkSecondary)

private val ThumbShape = RoundedCornerShape(12.dp)
private val BadgeShape = RoundedCornerShape(6.dp)

class ItemActions(
    val entries: List<Pair<String, () -> Unit>>,
    val checked: Set<String> = emptySet(),
)

@Composable
private fun sectionTitle(header: YtSectionHeader?): String {
    val key = header?.titleKey ?: return header?.title.orEmpty()
    val resource = when (key) {
        "trending_music_videos" -> Res.string.chart_trending_music_videos
        "top_songs" -> Res.string.chart_top_songs
        "top_songs_daily" -> Res.string.chart_top_songs_daily
        "top_songs_weekly" -> Res.string.chart_top_songs_weekly
        "top_music_videos" -> Res.string.chart_top_music_videos
        "top_music_videos_daily" -> Res.string.chart_top_music_videos_daily
        "top_music_videos_weekly" -> Res.string.chart_top_music_videos_weekly
        "top_songs_on_shorts" -> Res.string.chart_top_songs_on_shorts
        "top_songs_on_shorts_daily" -> Res.string.chart_top_songs_on_shorts_daily
        "top_songs_on_shorts_weekly" -> Res.string.chart_top_songs_on_shorts_weekly
        "subscribed_channels" -> Res.string.section_subscribed_channels
        else -> null
    }
    return resource?.let { stringResource(it) } ?: header.title.orEmpty()
}

@Composable
fun SectionHeader(
    header: YtSectionHeader?,
    modifier: Modifier = Modifier,
    collapsed: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    onCta: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(
                start = DesignSpacing.MarginCompact,
                end = DesignSpacing.MarginCompact,
                top = DesignSpacing.Space16,
                bottom = 10.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (header != null && header.iconThumbs.isNotEmpty()) {
            Box(
                Modifier.size(28.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                ThumbImage(header.iconThumbs, CircleShape) {
                    ThumbPlaceholder(Icons.Outlined.MusicNote, 14.dp, DesignColors.PlaceholderDim)
                }
            }
        }
        Text(
            sectionTitle(header),
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = true).semantics { heading() },
        )
        header?.count?.let {
            Text(it, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DesignColors.InkSecondary))
        }
        when {
            onToggle != null && header?.ctaDeeplink == null && header?.ctaAction == null -> ActionIcon(
                Icons.Default.KeyboardArrowDown,
                stringResource(if (collapsed == false) Res.string.cd_collapse else Res.string.cd_expand),
                Modifier.rotate(if (collapsed == false) 180f else 0f),
                iconSize = 20.dp,
                tint = DesignColors.InkSoft,
                onClick = onToggle,
            )
            header?.ctaText != null || header?.ctaToken != null || header?.ctaDeeplink != null || header?.ctaAction != null -> {
                val playAll = header.ctaAction == YtSectionHeader.CTA_PLAY_ALL
                val cta = if (onCta != null && (header.ctaDeeplink != null || playAll)) onCta else null
                val label = header.ctaText ?: stringResource(if (playAll) Res.string.play_all else Res.string.cd_view_all)
                Box(if (cta != null) Modifier.bleed(SectionCtaPaddingH, SectionCtaPaddingV) else Modifier) {
                    Row(
                        if (cta != null) {
                            Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(DesignColors.SurfaceElevated)
                                .clickable(role = Role.Button, onClick = cta)
                                .padding(horizontal = SectionCtaPaddingH, vertical = SectionCtaPaddingV)
                        } else {
                            Modifier
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (playAll) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(18.dp), tint = DesignColors.Accent)
                        }
                        val text = if (header.ctaText != null || cta != null) label else null
                        text?.let {
                            Text(
                                it,
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (cta != null) DesignColors.Accent else DesignColors.InkSoft,
                                ),
                            )
                        }
                        if (!playAll) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, Modifier.size(18.dp), tint = DesignColors.Accent)
                        }
                    }
                }
            }
        }
    }
}

private val SectionCtaPaddingH = 12.dp
private val SectionCtaPaddingV = 6.dp

@Composable
fun FeedListItem(
    item: YtFeedItem,
    actions: ItemActions,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    compactTitle: Boolean = false,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .a11yRowLabel(feedItemLabel(item))
            .padding(
                start = DesignSpacing.MarginCompact,
                end = DesignSpacing.Space4,
                top = DesignSpacing.ListRowVertical,
                bottom = DesignSpacing.ListRowVertical,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        when {
            item.kind == YtFeedItem.KIND_CHANNEL -> CircleThumb(item, 74.dp, Modifier.clearAndSetSemantics {})
            wide -> RectThumb(item, width = 160.dp, height = 90.dp, Modifier.clearAndSetSemantics {})
            else -> RectThumb(item, width = 92.dp, height = 92.dp, Modifier.clearAndSetSemantics {})
        }
        Spacer(Modifier.width(DesignSpacing.Space16))
        val bare = actions.entries.isEmpty()
        Column(Modifier.weight(1f).padding(end = if (bare) 12.dp else 0.dp).clearAndSetSemantics {}) {
            Text(
                item.title,
                style = if (compactTitle) ListTitleCompactStyle else ListTitleStyle,
                maxLines = if (wide) 3 else 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(item.subtitleLine1, item.subtitleLine2).joinToString(" · ")
            if (sub.isNotEmpty()) {
                Text(sub, style = ListSubStyle, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (actions.entries.isNotEmpty()) MoreMenu(actions, Modifier.align(Alignment.Top).padding(top = 2.dp))
    }
}

@Composable
fun FeedGridCard(
    item: YtFeedItem,
    actions: ItemActions,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    inset: Dp = GRID_CARD_INSET,
) {
    if (item.kind == YtFeedItem.KIND_CHANNEL) {
        ChannelGridCard(item, actions, onClick, modifier, inset)
        return
    }
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .a11yRowLabel(feedItemLabel(item))
            .padding(inset),
    ) {
        Box(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
            if (item.kind == YtFeedItem.KIND_PLAYLIST) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .offset(y = (-4).dp)
                        .height(4.dp)
                        .background(DesignColors.StackTab, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(ThumbShape),
                contentAlignment = Alignment.Center,
            ) {
                ThumbImage(item.thumbs, ThumbShape) {
                    ThumbPlaceholder(Icons.Outlined.Image, 22.dp)
                }
                WatchedBar(item, Modifier.align(Alignment.BottomCenter))
                Badges(item, Modifier.align(Alignment.BottomEnd).padding(6.dp), textSize = 11, iconSize = 14.dp)
                if (isAvailableOffline(item)) AvailableOfflineMark(Modifier.align(Alignment.TopStart))
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).clearAndSetSemantics {}) {
                Text(item.title, style = GridTitleStyle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(item.subtitleLine1, item.subtitleLine2).joinToString(" · ")
                if (sub.isNotEmpty()) {
                    Text(sub, style = GridSubStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
            if (actions.entries.isNotEmpty()) MoreMenu(actions, Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun ChannelGridCard(
    item: YtFeedItem,
    actions: ItemActions,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    inset: Dp = GRID_CARD_INSET,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .a11yRowLabel(feedItemLabel(item))
            .padding(inset),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            CircleThumb(item, 76.dp, Modifier.clearAndSetSemantics {})
            Text(
                item.title,
                style = GridTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics {},
            )
            item.subtitleLine1?.let {
                Text(
                    it,
                    style = GridSubStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp).clearAndSetSemantics {},
                )
            }
        }
        if (actions.entries.isNotEmpty()) MoreMenu(actions, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
fun ShowMoreButton(label: String, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ChipTapArea(onClick, modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) { indication ->
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(DesignColors.SurfaceDim)
                .border(1.dp, DesignColors.OutlineSoft, RoundedCornerShape(999.dp))
                .then(indication)
                .chipPadding(),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = TextStyle(fontSize = ChipTextSize, fontWeight = FontWeight.Medium, color = DesignColors.Ink))
            Icon(
                Icons.Default.KeyboardArrowDown,
                null,
                Modifier.size(16.dp).rotate(if (expanded) 180f else 0f),
                tint = DesignColors.InkMuted,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagFlow(items: List<YtFeedItem>, onOpen: ((YtFeedItem) -> Unit)?) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item -> TagPill(item.title, item.accent) { onOpen?.invoke(item) } }
    }
}

@Composable
internal fun ChannelCarousel(items: List<YtFeedItem>, onOpen: ((YtFeedItem) -> Unit)?) {
    val rowState = rememberLazyListState()
    LazyRow(
        Modifier.horizontalMouseDragScroll(rowState),
        state = rowState,
        contentPadding = PaddingValues(horizontal = DesignSpacing.MarginCompact - DesignSpacing.Space4),
        horizontalArrangement = Arrangement.spacedBy(DesignSpacing.Space4),
    ) {
        items(items.size) { i ->
            val item = items[i]
            Column(
                Modifier
                    .width(CAROUSEL_CELL_WIDTH)
                    .clip(RoundedCornerShape(12.dp))
                    .then(if (onOpen != null) Modifier.clickable { onOpen(item) } else Modifier)
                    .padding(DesignSpacing.Space4),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DesignSpacing.Space4),
            ) {
                CircleThumb(item, 64.dp)
                Text(
                    item.title,
                    style = GridSubStyle.copy(color = DesignColors.Ink),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val CAROUSEL_CELL_WIDTH = 80.dp

@Composable
private fun TagPill(label: String, accent: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val border = remember(accent) { accent?.let { runCatching { Color(it.toLong(16)) }.getOrNull() } } ?: DesignColors.OutlineSoft
    ChipTapArea(onClick) { indication ->
        Text(
            label,
            style = TextStyle(fontSize = ChipTextSize, fontWeight = FontWeight.Medium, color = DesignColors.Ink),
            maxLines = 1,
            modifier = Modifier
                .background(DesignColors.SurfaceElevated, shape)
                .border(1.dp, border, shape)
                .clip(shape)
                .then(indication)
                .chipPadding(),
        )
    }
}

@Composable
internal fun MoreMenu(actions: ItemActions, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        ActionIcon(
            Icons.Default.MoreVert,
            stringResource(Res.string.cd_more),
            tint = DesignColors.IconDots,
            onClick = { open = true },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            actions.entries.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    trailingIcon = if (label in actions.checked) {
                        { Icon(Icons.Default.Check, contentDescription = null, tint = DesignColors.Accent) }
                    } else {
                        null
                    },
                    onClick = {
                        action()
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
private fun feedItemLabel(item: YtFeedItem): String {
    val kind = when {
        item.kind == YtFeedItem.KIND_CHANNEL -> Res.string.a11y_kind_channel
        item.id.startsWith("RD") && item.kind == YtFeedItem.KIND_PLAYLIST -> Res.string.a11y_kind_mix
        item.album -> Res.string.a11y_kind_album
        item.kind == YtFeedItem.KIND_PLAYLIST -> Res.string.a11y_kind_playlist
        item.kind != YtFeedItem.KIND_VIDEO -> null
        item.live -> Res.string.a11y_kind_live
        item.musicVideo -> Res.string.a11y_kind_music_video
        else -> Res.string.a11y_kind_video
    }
    val sub = listOfNotNull(item.subtitleLine1, item.subtitleLine2).joinToString(" · ")
    return listOfNotNull(
        kind?.let { stringResource(it) },
        item.title,
        item.durationMs?.let { spokenDuration(it) },
        item.itemCount?.takeIf { it !in sub },
        sub,
        if (isAvailableOffline(item)) stringResource(Res.string.a11y_downloaded) else null,
    ).filter { it.isNotEmpty() }.joinToString(", ")
}

@Composable
private fun CircleThumb(item: YtFeedItem, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        ThumbImage(item.thumbs, CircleShape) {
            ThumbPlaceholder(Icons.Outlined.Person, 28.dp)
        }
    }
}

@Composable
private fun RectThumb(
    item: YtFeedItem,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val stacked = item.kind == YtFeedItem.KIND_PLAYLIST
    val stackFar = DesignColors.StackFar
    val stackNear = DesignColors.StackNear
    Box(
        modifier
            .size(width = width, height = height)
            .then(if (stacked) Modifier.drawBehind { drawPlaylistStack(stackFar, stackNear) } else Modifier)
            .clip(ThumbShape),
        contentAlignment = Alignment.Center,
    ) {
        ThumbImage(item.thumbs, ThumbShape) {
            ThumbPlaceholder(Icons.Outlined.Image, if (height < 80.dp) 21.dp else 30.dp)
        }
        WatchedBar(item, Modifier.align(Alignment.BottomCenter))
        Badges(item, Modifier.align(Alignment.BottomEnd).padding(6.dp), textSize = 11, iconSize = 14.dp)
        if (isAvailableOffline(item)) AvailableOfflineMark(Modifier.align(Alignment.TopStart))
    }
}

val GRID_CARD_MAX_WIDTH = 324.dp
val GRID_CARD_INSET = 8.dp

@Composable
internal fun WatchedBar(item: YtFeedItem, modifier: Modifier) {
    val percent = item.watchedPercent ?: return
    Box(modifier.fillMaxWidth().height(3.dp).background(DesignColors.WatchedTrack)) {
        Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight().background(DesignColors.WatchedFill))
    }
}

@Composable
internal fun LiveBadge(textSize: Int, modifier: Modifier = Modifier) {
    Text(
        stringResource(Res.string.badge_live),
        style = TextStyle(fontSize = textSize.sp, fontWeight = FontWeight.Bold, color = Color.White),
        modifier = modifier.background(Color(0xFFB3261E), BadgeShape).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
internal fun Badges(item: YtFeedItem, modifier: Modifier, textSize: Int, iconSize: Dp) {
    val badgeText = TextStyle(fontSize = textSize.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
    when {
        item.live -> LiveBadge(textSize, modifier)
        item.kind == YtFeedItem.KIND_PLAYLIST || item.itemCount != null -> Row(
            modifier.background(DesignColors.BadgeScrim, BadgeShape).padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                when {
                    item.id.startsWith("RD") -> Icons.Default.AllInclusive
                    item.album -> Icons.Outlined.Album
                    else -> Icons.AutoMirrored.Filled.PlaylistPlay
                },
                null,
                Modifier.size(iconSize),
                tint = Color.White,
            )
            item.itemCount?.let { Text(it, style = badgeText) }
        }
        item.duration != null -> Row(
            modifier
                .background(DesignColors.BadgeInk, BadgeShape)
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .spokenAs(
                    listOfNotNull(
                        if (item.musicVideo) stringResource(Res.string.a11y_kind_music_video) else null,
                        spokenLength(item.durationMs, item.duration) ?: item.duration,
                    ).joinToString(", "),
                ),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.musicVideo) {
                Icon(Icons.Outlined.MusicNote, null, Modifier.size(iconSize), tint = Color.White)
            }
            Text(item.duration!!, style = badgeText)
        }
        item.musicVideo -> Icon(
            Icons.Outlined.MusicNote,
            null,
            modifier
                .background(DesignColors.BadgeInk, BadgeShape)
                .padding(horizontal = 5.dp, vertical = 2.dp)
                .size(iconSize)
                .spokenAs(stringResource(Res.string.a11y_kind_music_video)),
            tint = Color.White,
        )
        item.album -> Icon(
            Icons.Outlined.Album,
            null,
            modifier
                .background(DesignColors.BadgeScrim, BadgeShape)
                .padding(horizontal = 5.dp, vertical = 2.dp)
                .size(iconSize)
                .spokenAs(stringResource(Res.string.a11y_kind_album)),
            tint = Color.White,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlaylistStack(stackFar: Color, stackNear: Color) {
    val r = CornerRadius(12.dp.toPx())
    fun layer(offsetPx: Float, shrinkPx: Float, color: Color) {
        drawRoundRect(
            color = color,
            topLeft = Offset(offsetPx + shrinkPx, -offsetPx + shrinkPx),
            size = Size(size.width - 2 * shrinkPx, size.height - 2 * shrinkPx),
            cornerRadius = r,
        )
    }
    layer(8.dp.toPx(), 2.dp.toPx(), stackFar)
    layer(4.dp.toPx(), 1.dp.toPx(), stackNear)
}
