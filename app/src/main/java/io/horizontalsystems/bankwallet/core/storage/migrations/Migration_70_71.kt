package io.horizontalsystems.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_70_71 : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `OxyraNodeRecord` (
                `url` TEXT NOT NULL,
                `username` TEXT,
                `password` TEXT,
                `trusted` INTEGER NOT NULL,
                PRIMARY KEY(`url`)
            )
        """.trimIndent())
    }
}
