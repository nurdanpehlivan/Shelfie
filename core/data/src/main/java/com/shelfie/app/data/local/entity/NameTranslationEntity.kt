package com.shelfie.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * How a pantry or shopping item name reads in another app language, e.g. "Naneli şeker" in
 * English is "Mint candy". Local cache only, never synced: each phone translates into its own
 * language, so a household can share one pantry across languages.
 */
@Entity(
    tableName = NameTranslationEntity.TABLE_NAME,
    primaryKeys = [NameTranslationEntity.COLUMN_SOURCE, NameTranslationEntity.COLUMN_LANGUAGE],
)
data class NameTranslationEntity(
    /** The name as stored on the item. */
    @ColumnInfo(name = COLUMN_SOURCE)
    val source: String,

    /** ISO 639 language code, e.g. "en". */
    @ColumnInfo(name = COLUMN_LANGUAGE)
    val language: String,

    @ColumnInfo(name = COLUMN_TRANSLATED)
    val translated: String,
) {
    companion object {
        const val TABLE_NAME = "name_translations"

        const val COLUMN_SOURCE = "source"
        const val COLUMN_LANGUAGE = "language"
        const val COLUMN_TRANSLATED = "translated"
    }
}
