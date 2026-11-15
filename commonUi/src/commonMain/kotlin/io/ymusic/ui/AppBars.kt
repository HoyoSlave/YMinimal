package io.ymusic.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.ymusic.dynamicengine.yt.YtPageTab
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.cd_back
import io.ymusic.ui.resources.cd_clear
import io.ymusic.ui.resources.cd_filter
import io.ymusic.ui.resources.cd_menu
import io.ymusic.ui.resources.cd_more_tabs
import io.ymusic.ui.resources.cd_search
import io.ymusic.ui.resources.cd_voice_search
import io.ymusic.ui.resources.search_youtube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.exp

@Stable
internal class ScrollHost(scrolled: Boolean = false) {
    private val state = mutableStateOf(scrolled)
    val scrolled: Boolean get() = state.value

    private var owner: Any? = null
    fun claim(token: Any) { owner = token }
    fun report(token: Any, value: Boolean) { if (owner === token) state.value = value }
    fun release(token: Any) {
        if (owner === token) {
            owner = null
            state.value = false
        }
    }
}

internal val LocalScrollHost = compositionLocalOf<ScrollHost?> { null }

internal val LocalOpenNavDrawer = compositionLocalOf<(() -> Unit)?> { null }

@Composable
internal fun TrackScroll(scrolled: () -> Boolean) {
    val host = LocalScrollHost.current ?: return
    val token = remember { Any() }
    DisposableEffect(host, token) {
        host.claim(token)
        onDispose { host.release(token) }
    }
    LaunchedEffect(host, token) { snapshotFlow(scrolled).collect { host.report(token, it) } }
}

@Composable
internal fun chromeTone(elevated: Boolean): Color =
    animateColorAsState(if (elevated) DesignColors.SurfaceElevated else DesignColors.Surface).value

internal fun elevationShadowStops(alpha: Float): Array<Pair<Float, Color>> = arrayOf(
    0f to Color.Black.copy(alpha = 0.12f * alpha),
    0.4f to Color.Black.copy(alpha = 0.04f * alpha),
    1f to Color.Transparent,
)

@Composable
internal fun ElevatedBarColumn(bottomShadow: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val elevated = LocalScrollHost.current?.scrolled == true
    val shadowAlpha by animateFloatAsState(if (elevated && bottomShadow) 1f else 0f)
    val tone = chromeTone(elevated)
    Column(
        Modifier
            .zIndex(1f)
            .drawBehind {
                if (shadowAlpha > 0f) {
                    val reach = 6.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            *elevationShadowStops(shadowAlpha),
                            startY = size.height,
                            endY = size.height + reach,
                        ),
                        topLeft = Offset(0f, size.height),
                        size = Size(size.width, reach),
                    )
                }
            }
            .background(tone),
        content = content,
    )
}

@Composable
internal fun <T : Any> UnderlineTabRow(
    selected: T?,
    entries: List<T>,
    dragProgress: Float,
    onSelect: (T) -> Unit,
    label: @Composable (tab: T, active: Boolean) -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val bounds = remember(rtl) { mutableStateMapOf<T, Pair<Dp, Dp>>() }
    val activeTabFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (selected != null && selected in entries) activeTabFocus.requestFocus()
    }
    HorizontalOverflowBox(scrollState) {
        Column(
            Modifier
                .fillMaxWidth()
                .horizontalWheelScroll(scrollState)
                .horizontalScroll(scrollState)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                Modifier.listKeyboardNavigation(ListOrientation.Horizontal),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                entries.forEach { t ->
                    val active = t == selected
                    Box(
                        Modifier
                            .onGloballyPositioned { c ->
                                val parent = c.parentLayoutCoordinates
                                val left = parent?.localPositionOf(c)?.x ?: 0f
                                val x = if (rtl && parent != null) parent.size.width - left - c.size.width else left
                                bounds[t] = with(density) { x.toDp() to c.size.width.toDp() }
                            }
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            .then(if (active) Modifier.focusRequester(activeTabFocus) else Modifier)
                            .semantics {
                                this.selected = active
                                role = Role.Tab
                            }
                            .clickable { onSelect(t) },
                    ) { label(t, active) }
                }
            }
            val cur = selected?.let { bounds[it] }
            val i = entries.indexOf(selected)
            val neighbor = bounds[entries[(i + if (dragProgress > 0f) 1 else -1).coerceIn(0, entries.lastIndex)]]
            val frac = abs(dragProgress) * TabIndicatorDragTravel
            val targetX = if (cur != null && neighbor != null) lerp(cur.first, neighbor.first, frac) else cur?.first ?: 0.dp
            val targetWidth = if (cur != null && neighbor != null) lerp(cur.second, neighbor.second, frac) else cur?.second ?: 0.dp
            val indicatorX by animateDpAsState(targetX, label = "tabIndicatorX")
            val indicatorWidth by animateDpAsState(targetWidth, label = "tabIndicatorWidth")
            Box(
                Modifier
                    .offset(x = indicatorX)
                    .width(indicatorWidth)
                    .height(3.dp)
                    .background(DesignColors.Accent, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)),
            )
        }
    }
}

@Composable
internal fun TabLabel(text: String, active: Boolean) {
    Text(
        text,
        style = TextStyle(
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            color = if (active) DesignColors.Accent else DesignColors.InkMuted,
        ),
        maxLines = 1,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

@Composable
internal fun AppBarRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
internal fun RowScope.AppBarTitle(text: String, fill: Boolean = true, reserveWidthOf: List<String> = emptyList()) {
    val style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong)
    val modifier = if (fill) Modifier.weight(1f) else Modifier
    if (reserveWidthOf.isEmpty()) {
        Text(text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.CenterStart) {
            for (label in reserveWidthOf) {
                Text(label, style = style, maxLines = 1, modifier = Modifier.alpha(0f).clearAndSetSemantics {})
            }
            Text(text, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun DesignAppBar(
    title: String,
    onMenu: (() -> Unit)?,
    onSearch: (() -> Unit)?,
    titleReserveWidthOf: List<String> = emptyList(),
    inlineContent: (@Composable () -> Unit)? = null,
) {
    AppBarRow {
        if (onMenu != null) {
            ActionIcon(Icons.Default.Menu, stringResource(Res.string.cd_menu), onClick = onMenu)
        } else {
            Spacer(Modifier.width(DesignSpacing.Space8))
        }
        AppBarTitle(title, fill = false, reserveWidthOf = titleReserveWidthOf)
        if (inlineContent != null) {
            Box(Modifier.weight(1f).padding(start = 16.dp)) { inlineContent() }
        } else {
            Box(Modifier.weight(1f))
        }
        if (onSearch != null) {
            ActionIcon(Icons.Default.Search, stringResource(Res.string.cd_search), onClick = onSearch)
        }
    }
}

@Composable
internal fun SearchInputBar(
    value: TextFieldValue,
    onValue: (TextFieldValue) -> Unit,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
    autoFocus: Boolean = true,
    placeholder: String = stringResource(Res.string.search_youtube),
    onVoice: (() -> Unit)? = null,
    voiceFirst: Boolean = false,
    voiceListening: Boolean = false,
    voiceLevel: () -> Float = { 0f },
    voiceHint: String? = null,
) = ElevatedBarColumn {
    val focusRequester = remember { FocusRequester() }
    val leadingMic = voiceFirst && onVoice != null
    val inlineMic = onVoice.takeUnless { leadingMic }
    AppBarRow {
        ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.cd_back), onClick = onBack)
        if (leadingMic) {
            VoiceMicButton(
                listening = voiceListening,
                level = voiceLevel,
                onClick = onVoice,
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
        Row(
            Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(DesignColors.FieldPill)
                .padding(start = 18.dp, end = if (value.text.isEmpty() && inlineMic == null) 18.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValue,
                singleLine = true,
                textStyle = TextStyle(fontSize = 16.sp, color = DesignColors.Ink),
                cursorBrush = SolidColor(DesignColors.Accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(value.text) }),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .tvTextFieldDpad(singleLine = true)
                    .then(if (leadingMic) Modifier else Modifier.focusRequester(focusRequester)),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        if (value.text.isEmpty()) {
                            Text(
                                placeholder,
                                style = TextStyle(fontSize = 16.sp, color = DesignColors.InkSecondary),
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            if (value.text.isNotEmpty()) {
                ActionIcon(
                    Icons.Default.Close,
                    stringResource(Res.string.cd_clear),
                    tint = DesignColors.InkSecondary,
                    onClick = { onValue(TextFieldValue("")) },
                )
            } else if (inlineMic != null) {
                ActionIcon(
                    Icons.Default.Mic,
                    stringResource(Res.string.cd_voice_search),
                    tint = if (voiceListening) DesignColors.Accent else DesignColors.InkSecondary,
                    onClick = inlineMic,
                )
            }
        }
    }
    if (leadingMic && voiceHint != null) {
        Text(
            voiceHint,
            style = TextStyle(fontSize = 13.sp, color = DesignColors.InkSecondary),
            modifier = Modifier.padding(start = VoiceHintInset, end = DesignSpacing.Space16, bottom = DesignSpacing.Space8),
        )
    }
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Composable
internal fun SearchResultsBar(
    title: String,
    onBack: () -> Unit,
    showFilter: Boolean,
    onFilter: () -> Unit,
    onSearch: () -> Unit,
) = ElevatedBarColumn {
    AppBarRow {
        ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.cd_back), onClick = onBack)
        AppBarTitle(title)
        if (showFilter) {
            ActionIcon(Icons.Outlined.FilterAlt, stringResource(Res.string.cd_filter), onClick = onFilter)
        }
        ActionIcon(Icons.Default.Search, stringResource(Res.string.cd_search), onClick = onSearch)
    }
}

@Composable
internal fun DetailAppBar(
    title: String,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    bottomContent: (@Composable () -> Unit)? = null,
) = ElevatedBarColumn {
    AppBarRow {
        ActionIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.cd_back), onClick = onBack)
        AppBarTitle(title)
        ActionIcon(Icons.Default.Search, stringResource(Res.string.cd_search), onClick = onSearch)
    }
    bottomContent?.invoke()
}

@Composable
internal fun ChannelTabRow(tabs: List<YtPageTab>, activeUrl: String?, onSelect: (YtPageTab) -> Unit) {
    val selected = tabs.firstOrNull { it.url != null && it.url == activeUrl } ?: tabs.firstOrNull { it.selected }
    UnderlineTabRow(selected, tabs, dragProgress = 0f, onSelect) { tab, active ->
        TabLabel(tab.title, active)
    }
}

@Composable
internal fun HorizontalOverflowBox(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        content()
        if (scrollState.maxValue > 0 && scrollState.value > 0) {
            TabOverflowHint(
                towardStart = true,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = { scope.launch { scrollState.animateScrollBy(-TabScrollStep) } },
            )
        }
        if (scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue) {
            TabOverflowHint(
                towardStart = false,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = { scope.launch { scrollState.animateScrollBy(TabScrollStep) } },
            )
        }
    }
}

private const val TabScrollStep = 240f

internal fun Modifier.horizontalWheelScroll(scrollState: ScrollableState): Modifier =
    pointerInput(scrollState) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Scroll) {
                    val dy = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (dy != 0f && scrollState.dispatchRawDelta(dy * WheelScrollStep) != 0f) {
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        }
    }

private const val WheelScrollStep = 64f

internal fun Modifier.horizontalMouseDragScroll(scrollState: ScrollableState): Modifier = composed {
    val sign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) 1f else -1f
    pointerInput(scrollState, sign) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type != PointerType.Mouse) return@awaitEachGesture
            val drag = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                change.consume()
                scrollState.dispatchRawDelta(sign * over)
            } ?: return@awaitEachGesture
            horizontalDrag(drag.id) { change ->
                scrollState.dispatchRawDelta(sign * change.positionChange().x)
                change.consume()
            }
        }
    }
}

@Composable
internal fun <T> SwipeTabSwitcher(
    selected: T,
    entries: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    onDragProgress: (Float) -> Unit = {},
    leadingEdgeReserve: Dp = 0.dp,
    onLeadingEdgeSwipe: (() -> Unit)? = null,
    content: @Composable (T) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val rtl = layoutDirection == LayoutDirection.Rtl
    val density = LocalDensity.current
    val gestures = WindowInsets.systemGestures
    val backLeft = gestures.getLeft(density, layoutDirection).toFloat()
    val backRight = gestures.getRight(density, layoutDirection).toFloat()
    val backStart = if (rtl) backRight else backLeft
    val backEnd = if (rtl) backLeft else backRight
    val drawerZone = if (onLeadingEdgeSwipe != null) backStart + with(density) { leadingEdgeReserve.toPx() } else 0f
    val peek = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(selected, entries, rtl, backStart, backEnd, drawerZone, onLeadingEdgeSwipe) {
                val distance = SwipeSwitchDistance.toPx()
                val velocity = SwipeSwitchVelocity.toPx()
                val k = RubberPeekStiffness.toPx()

                fun reportDrag(dx: Float) {
                    onDragProgress(((if (rtl) dx else -dx) / distance).coerceIn(-1f, 1f))
                    val forward = if (rtl) dx > 0 else dx < 0
                    val hasNext = (entries.indexOf(selected) + if (forward) 1 else -1) in entries.indices
                    val stiffness = if (hasNext) k else k * 0.3f
                    scope.launch { peek.snapTo(weightedRubberPeek(dx, stiffness)) }
                }

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val x = down.position.x
                    val distFromStart = if (rtl) size.width - x else x
                    val distFromEnd = if (rtl) x else size.width - x
                    if (distFromEnd < backEnd) return@awaitEachGesture
                    if (drawerZone > 0f && distFromStart < drawerZone) {
                        var over = 0f
                        val past = awaitHorizontalTouchSlopOrCancellation(down.id) { change, o -> over = o; change.consume() }
                        if (past != null && (if (rtl) over < 0 else over > 0)) onLeadingEdgeSwipe?.invoke()
                        return@awaitEachGesture
                    }
                    if (drawerZone == 0f && distFromStart < backStart) return@awaitEachGesture
                    var dx = 0f
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    val past = awaitHorizontalTouchSlopOrCancellation(down.id) { change, over ->
                        dx += over
                        tracker.addPosition(change.uptimeMillis, change.position)
                        reportDrag(dx)
                        change.consume()
                    }
                    if (past == null) return@awaitEachGesture
                    val completed = horizontalDrag(down.id) { change ->
                        dx += change.positionChange().x
                        tracker.addPosition(change.uptimeMillis, change.position)
                        reportDrag(dx)
                        change.consume()
                    }
                    if (completed) {
                        val flung = abs(tracker.calculateVelocity().x) >= velocity
                        val advance = if (rtl) dx > 0 else dx < 0
                        val next = entries.indexOf(selected) + if (advance) 1 else -1
                        if ((abs(dx) >= distance || flung) && next in entries.indices) onSelect(entries[next])
                    }
                    onDragProgress(0f)
                    scope.launch { peek.animateTo(0f, PeekSpring) }
                }
            },
    ) {
        AnimatedContent(
            targetState = selected,
            transitionSpec = {
                val advancing = entries.indexOf(targetState) > entries.indexOf(initialState)
                val sign = if (advancing != rtl) 1 else -1
                slideInHorizontally(tween(SwipeSwitchDurationMs)) { w -> sign * w }
                    .togetherWith(slideOutHorizontally(tween(SwipeSwitchDurationMs)) { w -> -sign * w })
            },
            label = "tab",
            modifier = Modifier.graphicsLayer { translationX = peek.value },
        ) { current -> content(current) }
    }
}

internal const val TabIndicatorDragTravel = 0.2f
internal val SwipeSwitchDistance = 56.dp
internal val SwipeSwitchVelocity = 400.dp
internal const val SwipeSwitchDurationMs = 220
private val RubberPeekStiffness = 48.dp

private fun weightedRubberPeek(rawPx: Float, k: Float): Float {
    if (rawPx == 0f || k <= 0f) return 0f
    val s = if (rawPx > 0f) 1f else -1f
    return s * k * (1f - exp(-abs(rawPx) / k))
}

internal val NavDrawerEdgeReserve = 24.dp
internal val PeekSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
private val DismissDragDistance = 120.dp

@Composable
internal fun SwipeDownDismissBox(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    commitFractionOfHeight: Float? = null,
    onPull: (Boolean) -> Unit = {},
    restingShadow: Boolean = false,
    onPeek: (Float) -> Unit = {},
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val peek = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val drag = remember { DragDown(peek, scope) }
    drag.onDismiss = onDismiss
    drag.onPull = onPull
    drag.stiffnessPx = with(density) { RubberPeekStiffness.toPx() }
    val defaultCommit = with(density) { DismissDragDistance.toPx() }
    drag.commitPx = defaultCommit
    val peekY = peek.value
    SideEffect { onPeek(peekY) }
    val fullPeek = (
        if (drag.maxMovePx > 0f) drag.maxMovePx
        else weightedRubberPeek(drag.commitPx, drag.stiffnessPx)
        ).coerceAtLeast(1f)
    val topShadow = (peekY / fullPeek).coerceIn(0f, 1f)
    val shadowAlpha = if (restingShadow) 0.06f + 0.06f * topShadow else 0.14f * topShadow
    val showShadow = restingShadow || topShadow > 0.01f

    val gestures = if (enabled) {
        Modifier
            .onSizeChanged {
                drag.heightPx = it.height.toFloat()
                val frac = commitFractionOfHeight
                if (frac != null && it.height > 0) {
                    val px = it.height.toFloat() * frac
                    drag.maxMovePx = px
                    drag.commitPx = px * 0.5f
                } else {
                    drag.commitPx = defaultCommit
                    drag.maxMovePx = 0f
                }
            }
            .nestedScroll(drag.connection)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, amount ->
                        if (amount > 0f || drag.raw > 0f) {
                            change.consume()
                            drag.report(amount)
                        }
                    },
                    onDragEnd = { if (drag.raw > 0f) drag.settle() },
                    onDragCancel = { if (drag.raw > 0f) drag.settle() },
                )
            }
    } else {
        Modifier
    }
    Box(modifier.then(gestures)) {
        Box(Modifier.fillMaxWidth().graphicsLayer { translationY = peekY }) {
            if (showShadow) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset(y = (-6).dp)
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = shadowAlpha),
                            ),
                        ),
                )
            }
            content()
        }
    }
}

private class DragDown(
    private val peek: Animatable<Float, *>,
    private val scope: CoroutineScope,
) {
    var raw = 0f
    var heightPx = 0f
    var stiffnessPx = 0f
    var commitPx = 0f
    var maxMovePx = 0f
    var onDismiss: () -> Unit = {}
    var onPull: (Boolean) -> Unit = {}
    private var pulling = false

    fun report(deltaY: Float) {
        raw = (raw + deltaY).coerceAtLeast(0f)
        if (!pulling) {
            pulling = true
            onPull(true)
        }
        val cap = maxMovePx
        val target = if (cap > 0f) {
            val t = (raw / (4f * cap)).coerceIn(0f, 1f)
            val u = 1f - t
            cap * (1f - u * u * u * u)
        } else {
            weightedRubberPeek(raw, stiffnessPx)
        }
        scope.launch { peek.snapTo(target) }
    }

    fun settle() {
        val cap = maxMovePx
        val commit = raw >= commitPx || (cap > 0f && peek.value >= cap * 0.5f)
        scope.launch {
            try {
                if (commit) {
                    val end = heightPx.takeIf { it > 0f } ?: (peek.value + 400f)
                    peek.animateTo(end, tween(SwipeSwitchDurationMs))
                    onDismiss()
                } else {
                    raw = 0f
                    peek.animateTo(0f, PeekSpring)
                }
            } finally {
                pulling = false
                onPull(false)
            }
        }
    }

    val connection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (!source.isTouchDrag() || raw <= 0f || available.y >= 0f) return Offset.Zero
            val before = raw
            report(available.y)
            return Offset(0f, raw - before)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (!source.isTouchDrag() || available.y <= 0f) return Offset.Zero
            report(available.y)
            return Offset(0f, available.y)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (raw > 0f || peek.value > 0f) {
                settle()
                return available
            }
            return Velocity.Zero
        }
    }
}

private fun NestedScrollSource.isTouchDrag(): Boolean = this == NestedScrollSource.UserInput

@Composable
private fun TabOverflowHint(towardStart: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val surface = DesignColors.Surface
    val colors = listOf(surface, surface.copy(alpha = 0f))
    Box(
        modifier
            .fillMaxHeight()
            .background(Brush.horizontalGradient(if (towardStart) colors else colors.reversed()))
            .clickable(role = Role.Button, onClick = onClick)
            .a11yActionLabel(stringResource(Res.string.cd_more_tabs))
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (towardStart) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
            null,
            Modifier.size(20.dp),
            tint = DesignColors.InkSoft,
        )
    }
}

private val VoiceHintInset = DesignSpacing.Space8 + 40.dp + DesignSpacing.Space8
