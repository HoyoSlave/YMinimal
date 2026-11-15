package io.ymusic.ui.playhistory

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun playHistoryAndroidDriver(context: Context, name: String? = "play-history.db"): SqlDriver =
    AndroidSqliteDriver(schema = PlayHistorySchema, context = context, name = name)
