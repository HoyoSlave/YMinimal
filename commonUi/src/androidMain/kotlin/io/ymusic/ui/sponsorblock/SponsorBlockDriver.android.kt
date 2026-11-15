package io.ymusic.ui.sponsorblock

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun sponsorBlockAndroidDriver(context: Context, name: String? = "sponsor-block.db"): SqlDriver =
    AndroidSqliteDriver(schema = SponsorBlockSchema, context = context, name = name)
