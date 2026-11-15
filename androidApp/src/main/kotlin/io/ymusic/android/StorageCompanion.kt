package io.ymusic.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings

internal object StorageCompanion {
    private const val ACTION_PICK_FOLDER = "io.ymusic.android.saf.action.PICK_FOLDER"

    fun pickFolderIntent(packageName: String): Intent = Intent(ACTION_PICK_FOLDER).setPackage(packageName)

    fun installedVersion(context: Context, packageName: String): Long? = try {
        context.packageManager.getPackageInfo(packageName, 0).longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun isSignedLikeUs(context: Context, packageName: String): Boolean =
        context.packageManager.checkSignatures(context.packageName, packageName) == PackageManager.SIGNATURE_MATCH

    fun canGrantAllFilesAccess(context: Context, packageName: String): Boolean = listOf(
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")),
        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
    ).any { it.resolveActivity(context.packageManager) != null }
}
