package io.ymusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.app_name
import io.ymusic.ui.resources.badge_premium
import io.ymusic.ui.resources.update_check
import io.ymusic.ui.resources.ymusic_logo_square
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val BrandGreenInk: Color @Composable get() = if (DesignColors.isDark) Color(0xFF6DD58C) else Color(0xFF146C2E)
private val BrandGreenBg: Color @Composable get() = if (DesignColors.isDark) Color(0xFF0F381E) else Color(0xFFE6F4EA)
private val BrandGreenBorder: Color @Composable get() = if (DesignColors.isDark) Color(0xFF1E5631) else Color(0xFFCEEAD6)

private val DrawerSheetShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
private val DrawerPillShape = RoundedCornerShape(28.dp)

@Composable
internal fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(Res.drawable.ymusic_logo_square),
        contentDescription = null,
        modifier = modifier.size(size).clip(CircleShape),
    )
}

internal fun listedSections(showCatalogue: Boolean, guest: Boolean, showDonate: Boolean): List<AppSection> =
    AppSection.entries
        .filter { it != AppSection.Catalogue || showCatalogue }
        .filter { it != AppSection.PlayAgain || !guest }
        .filter { it != AppSection.Donate || showDonate }

@Composable
fun AppDrawer(
    section: AppSection,
    appVersion: String?,
    onSelect: (AppSection) -> Unit,
    showCatalogue: Boolean = false,
    guest: Boolean = false,
    updates: AppUpdates? = null,
    onCheckUpdate: (() -> Unit)? = null,
    premium: Boolean = false,
    showDonate: Boolean = false,
    websiteUrl: String = AppLinks(null).website,
    open: Boolean = false,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = DesignColors.SurfaceDim,
        drawerShape = DrawerSheetShape,
    ) {
        Column(Modifier.fillMaxHeight()) {
            val updateStatus = updates?.status?.collectAsState()?.value
            val updateWaiting = updateStatus is AppUpdateStatus.Offered
            DrawerHeader(appVersion, premium, onCheckUpdate, updateWaiting, websiteUrl)
            HorizontalDivider(color = DesignColors.Divider, modifier = Modifier.padding(horizontal = 16.dp))
            val listed = listedSections(showCatalogue, guest, showDonate)
            val activeItemFocus = remember { FocusRequester() }
            LaunchedEffect(open) {
                if (open && section in listed) activeItemFocus.requestFocus()
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .listKeyboardNavigation()
                    .padding(vertical = 12.dp),
            ) {
                listed
                    .groupBy { it.group }
                    .entries
                    .forEachIndexed { index, (_, sections) ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = DesignColors.Divider,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                            )
                        }
                        sections.forEach { s ->
                            val active = s == section
                            DrawerItem(
                                section = s,
                                active = active,
                                focusRequester = if (active) activeItemFocus else null,
                            ) { onSelect(s) }
                        }
                    }
            }
        }
    }
}

@Composable
private fun DrawerHeader(
    appVersion: String?,
    premium: Boolean,
    onCheckUpdate: (() -> Unit)?,
    updateWaiting: Boolean,
    websiteUrl: String,
) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppLogo(48.dp)
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.app_name),
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.InkStrong),
                    )
                    if (premium) PremiumBadge()
                }
                appVersion?.let {
                    Text(
                        it,
                        style = TextStyle(fontSize = 12.sp, color = DesignColors.InkSecondary),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
        DrawerLinks(onCheckUpdate, updateWaiting, websiteUrl)
    }
}

@Composable
internal fun PremiumBadge() {
    Text(
        stringResource(Res.string.badge_premium),
        style = TextStyle(
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = BrandGreenInk,
        ),
        modifier = Modifier
            .background(BrandGreenBg, RoundedCornerShape(999.dp))
            .border(1.dp, BrandGreenBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
internal fun DrawerLinks(onCheckUpdate: (() -> Unit)?, updateWaiting: Boolean, websiteUrl: String) {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(top = DesignSpacing.Space8)) {
        if (onCheckUpdate != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeaderLink(stringResource(Res.string.update_check), highlighted = updateWaiting, onClick = onCheckUpdate)
                if (updateWaiting) {
                    Box(Modifier.size(DesignSpacing.Space8).background(DesignColors.Accent, CircleShape))
                }
            }
        }
        HeaderLink("ymusic.io") { uriHandler.openUri(websiteUrl) }
    }
}

@Composable
private fun HeaderLink(text: String, highlighted: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (highlighted) DesignColors.Accent else DesignColors.InkSecondary,
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(DesignSpacing.Space8))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
internal fun DrawerItem(
    section: AppSection,
    active: Boolean,
    focusRequester: FocusRequester? = null,
    iconStart: Dp = DrawerIconStart,
    onClick: () -> Unit,
) = DrawerRow(section.icon, stringResource(section.label), active, focusRequester, iconStart, onClick)

@Composable
private fun DrawerRow(
    icon: ImageVector,
    label: String,
    active: Boolean,
    focusRequester: FocusRequester?,
    iconStart: Dp,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val focusMod = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(DrawerPillShape)
            .then(focusMod)
            .clickable(onClick = onClick)
            .semantics { selected = active }
            .background(if (active) scheme.secondaryContainer else Color.Transparent),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(start = (iconStart - 12.dp).coerceAtLeast(16.dp), end = 24.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                null,
                Modifier.size(DrawerIconSize),
                tint = if (active) scheme.onSecondaryContainer else DesignColors.InkMuted,
            )
            Text(
                label,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) scheme.onSecondaryContainer else DesignColors.InkStrong,
                ),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal val DrawerIconSize = 24.dp
private val DrawerIconStart = 28.dp
