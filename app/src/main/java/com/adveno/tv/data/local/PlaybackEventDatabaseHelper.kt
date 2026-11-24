package com.adveno.tv.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PlaybackEventDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                $COLUMN_EVENT_ID TEXT PRIMARY KEY,
                $COLUMN_PLAYLIST_ID TEXT NOT NULL,
                $COLUMN_PLAYLIST_ITEM_ID TEXT NOT NULL,
                $COLUMN_MEDIA_ASSET_ID TEXT NOT NULL,
                $COLUMN_PLAYLIST_VERSION INTEGER NOT NULL,
                $COLUMN_MEDIA_CHECKSUM TEXT NOT NULL,
                $COLUMN_PLAYED_AT TEXT NOT NULL,
                $COLUMN_PLAYED_COMPLETE_AT TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migration strategy for future schema updates
    }

    companion object {
        const val DATABASE_NAME = "adveno_playback_events.db"
        const val DATABASE_VERSION = 1

        const val TABLE_NAME = "playback_events"
        const val COLUMN_EVENT_ID = "event_id"
        const val COLUMN_PLAYLIST_ID = "playlist_id"
        const val COLUMN_PLAYLIST_ITEM_ID = "playlist_item_id"
        const val COLUMN_MEDIA_ASSET_ID = "media_asset_id"
        const val COLUMN_PLAYLIST_VERSION = "playlist_version"
        const val COLUMN_MEDIA_CHECKSUM = "media_checksum"
        const val COLUMN_PLAYED_AT = "played_at"
        const val COLUMN_PLAYED_COMPLETE_AT = "played_complete_at"
    }
}
