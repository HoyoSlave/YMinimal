package io.ymusic.ui

import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.StringResource

interface AppUpdates {
    val status: StateFlow<AppUpdateStatus>

    val blockedReason: StringResource?

    fun download()

    fun installAndRestart()

    fun allowInstall() {}

    suspend fun checkNow(): UpdateCheck
}

enum class UpdateCheck { UpToDate, Found, Failed }

sealed interface AppUpdateStatus {
    data object None : AppUpdateStatus

    data class Offered(
        val versionCode: Int,
        val versionName: String,
        val notes: String,
        val changelogUrl: String?,
        val installInApp: Boolean,
        val downloadPage: String?,
        val download: UpdateDownload,
    ) : AppUpdateStatus
}

sealed interface UpdateDownload {
    data object Idle : UpdateDownload
    data class Running(val done: Long, val total: Long) : UpdateDownload
    data object Ready : UpdateDownload

    data object Installing : UpdateDownload
    data class Failed(val code: String) : UpdateDownload

    data class InstallFailed(val code: String) : UpdateDownload

    data class NeedsInstallPermission(val offerBrowser: Boolean) : UpdateDownload
}
