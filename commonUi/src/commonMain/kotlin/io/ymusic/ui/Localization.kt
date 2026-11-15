package io.ymusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.LayoutDirection
import io.ymusic.settings.AppSettings
import io.ymusic.settings.SettingsStore
import io.ymusic.settings.ensureUiLanguage
import io.ymusic.settings.flow
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.startup_failed
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.ResourceEnvironment
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import org.jetbrains.compose.resources.rememberResourceEnvironment
import org.jetbrains.compose.resources.stringResource
import kotlin.concurrent.Volatile

internal sealed interface UiText {
    class Plain(val text: String) : UiText
    class Resource(val res: StringResource, vararg val args: Any) : UiText
}

@Composable
internal fun UiText.resolve(): String = when (this) {
    is UiText.Plain -> text
    is UiText.Resource -> stringResource(res, *args)
}

internal suspend fun UiText.load(): String = when (this) {
    is UiText.Plain -> text
    is UiText.Resource -> getString(res, *args)
}

@Volatile
internal var restoreUiLocale: ((noRules: NoSuchElementException, alreadyRestored: Boolean) -> Unit)? = null

private var uiLocaleRestores by mutableIntStateOf(0)

@Volatile
private var restoredOver: ResourceEnvironment? = null

@Volatile
private var restoredAt = 0

@Volatile
private var reportedStuckOn: ResourceEnvironment? = null

private fun onNoPluralRules(environment: ResourceEnvironment, noRules: NoSuchElementException, restoresSeen: Int) {
    when {
        environment == reportedStuckOn -> return
        environment != restoredOver -> {
            restoredOver = environment
            restoreUiLocale?.invoke(noRules, false)
            restoredAt = ++uiLocaleRestores
        }
        restoresSeen >= restoredAt -> {
            reportedStuckOn = environment
            restoreUiLocale?.invoke(noRules, true)
        }
    }
}

@Composable
internal fun pluralText(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
    val restores = uiLocaleRestores
    val environment = rememberResourceEnvironment()
    val args = formatArgs.map { it.toString() }
    val plural = remember(resource, quantity, args, environment, restores) {
        runBlocking { pluralOrCount(environment, resource, quantity, args) }
    }
    plural.noRules?.let { noRules -> SideEffect { onNoPluralRules(environment, noRules, restores) } }
    return plural.text
}

internal suspend fun loadPluralText(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String {
    val args = formatArgs.map { it.toString() }
    val restores = uiLocaleRestores
    val environment = getSystemResourceEnvironment()
    val plural = pluralOrCount(environment, resource, quantity, args)
    val noRules = plural.noRules ?: return plural.text
    onNoPluralRules(environment, noRules, restores)
    val restored = getSystemResourceEnvironment()
    val retried = pluralOrCount(restored, resource, quantity, args)
    retried.noRules?.let { onNoPluralRules(restored, it, uiLocaleRestores) }
    return retried.text
}

private class PluralOrCount(val text: String, val noRules: NoSuchElementException? = null)

private suspend fun pluralOrCount(
    environment: ResourceEnvironment,
    resource: PluralStringResource,
    quantity: Int,
    args: List<String>,
): PluralOrCount = try {
    PluralOrCount(getPluralString(environment, resource, quantity, *args.toTypedArray()))
} catch (noRules: NoSuchElementException) {
    PluralOrCount(quantity.toString(), noRules)
}

class UiLanguage(val tag: String, val nativeName: String)

val UiLanguages: List<UiLanguage> = listOf(
    UiLanguage("en", "English"),
    UiLanguage("af", "Afrikaans"),
    UiLanguage("az", "Azərbaycan"),
    UiLanguage("id", "Bahasa Indonesia"),
    UiLanguage("ms", "Bahasa Melayu"),
    UiLanguage("ca", "Català"),
    UiLanguage("cs", "Čeština"),
    UiLanguage("de", "Deutsch"),
    UiLanguage("et", "Eesti"),
    UiLanguage("es", "Español"),
    UiLanguage("eu", "Euskara"),
    UiLanguage("fil", "Filipino"),
    UiLanguage("fr", "Français"),
    UiLanguage("hr", "Hrvatski"),
    UiLanguage("sw", "Kiswahili"),
    UiLanguage("hu", "Magyar"),
    UiLanguage("nl", "Nederlands"),
    UiLanguage("pl", "Polski"),
    UiLanguage("pt", "Português"),
    UiLanguage("ro", "Română"),
    UiLanguage("sk", "Slovenčina"),
    UiLanguage("so", "Soomaali"),
    UiLanguage("it", "Italiano"),
    UiLanguage("vi", "Tiếng Việt"),
    UiLanguage("tr", "Türkçe"),
    UiLanguage("el", "Ελληνικά"),
    UiLanguage("bg", "Български"),
    UiLanguage("mk", "Македонски"),
    UiLanguage("ru", "Русский"),
    UiLanguage("sr", "Српски"),
    UiLanguage("uk", "Українська"),
    UiLanguage("hy", "Հայերեն"),
    UiLanguage("iw", "עברית"),
    UiLanguage("ar", "العربية"),
    UiLanguage("fa", "فارسی"),
    UiLanguage("ne", "नेपाली"),
    UiLanguage("mr", "मराठी"),
    UiLanguage("hi", "हिन्दी"),
    UiLanguage("bn", "বাংলা"),
    UiLanguage("gu", "ગુજરાતી"),
    UiLanguage("ta", "தமிழ்"),
    UiLanguage("kn", "ಕನ್ನಡ"),
    UiLanguage("ml", "മലയാളം"),
    UiLanguage("si", "සිංහල"),
    UiLanguage("th", "ไทย"),
    UiLanguage("ko", "한국어"),
    UiLanguage("ja", "日本語"),
    UiLanguage("zh-CN", "简体中文"),
    UiLanguage("zh-TW", "繁體中文"),
)

internal expect fun applyUiLocale(tag: String?)

internal expect fun formatTimeOfDay(epochMs: Long): String

internal expect fun systemLanguageTag(): String

internal expect fun systemLocaleTag(): String

internal fun defaultUiLanguageTag(): String = uiLanguageFor(systemLocaleTag())

internal fun uiLanguageTag(settings: SettingsStore): String = settings.flow(AppSettings.uiLanguage).value.ifBlank { defaultUiLanguageTag() }

internal fun uiLanguageFor(localeTag: String): String {
    val parts = localeTag.replace('_', '-').split('-').filter { it.isNotEmpty() }
    val language = when (val primary = parts.firstOrNull()?.lowercase().orEmpty()) {
        "he" -> "iw"
        "in" -> "id"
        "tl" -> "fil"
        "zh" -> {
            val traditional = parts.drop(1).any { it.equals("Hant", ignoreCase = true) || it.uppercase() in TraditionalChineseRegions }
            if (traditional) "zh-TW" else "zh-CN"
        }
        else -> primary
    }
    return UiLanguages.firstOrNull { it.tag == language }?.tag ?: "en"
}

private val TraditionalChineseRegions = setOf("TW", "HK", "MO")

suspend fun ensureAppLanguage(settings: SettingsStore): String =
    settings.ensureUiLanguage(::defaultUiLanguageTag)

private val RTL_LANGUAGES = setOf("ar", "fa", "he", "iw", "ur", "ug", "ps", "sd", "yi", "dv", "ckb")

internal fun isRtlLanguage(languageTag: String): Boolean =
    languageTag.substringBefore('-').substringBefore('_').lowercase() in RTL_LANGUAGES

@Composable
fun startupFailedMessage(detail: String): String = stringResource(Res.string.startup_failed, detail)

@Composable
fun LanguageDirection(content: @Composable () -> Unit) {
    val direction = if (isRtlLanguage(Locale.current.language)) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides direction, content = content)
}
