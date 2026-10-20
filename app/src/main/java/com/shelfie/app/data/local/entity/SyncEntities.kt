package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

fun newSyncId(): String = UUID.randomUUID().toString()

/**
 * Records changed locally and not yet pushed to the server, filled by SQLite triggers on the
 * synced tables (see [com.shelfie.app.data.local.SyncTriggers]), so no write path can forget it.
 * One row per record: a later change replaces the earlier one. A record that no longer exists
 * locally when pushed was deleted.
 */
@Entity(tableName = SyncOutboxEntity.TABLE_NAME, primaryKeys = ["collection", "sync_id"])
data class SyncOutboxEntity(
    @ColumnInfo(name = "collection")
    val collection: String,

    @ColumnInfo(name = "sync_id")
    val syncId: String,

    /** Epoch milliseconds of the change; the server resolves conflicts by it (last write wins). */
    @ColumnInfo(name = "changed_at")
    val changedAt: Long,
) {
    companion object {
        const val TABLE_NAME = "sync_outbox"
    }
}

/**
 * Holds a row only while remote changes are being written, which the outbox triggers check so
 * that applying the server's records doesn't queue them to be pushed straight back.
 */
@Entity(tableName = SyncApplyingEntity.TABLE_NAME)
data class SyncApplyingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 0,
) {
    companion object {
        const val TABLE_NAME = "sync_applying"
    }
}

/** Collection names shared with the backend (backend/src/sync.ts). */
object SyncCollections {
    const val PANTRY = "pantry"
    const val SHOPPING = "shopping"
    const val RECIPES = "recipes"
}
