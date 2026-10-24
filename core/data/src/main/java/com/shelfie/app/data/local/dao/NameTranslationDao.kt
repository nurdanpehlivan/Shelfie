package com.shelfie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shelfie.app.data.local.entity.NameTranslationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NameTranslationDao {

    @Query("SELECT * FROM name_translations WHERE language = :language")
    fun observeLanguage(language: String): Flow<List<NameTranslationEntity>>

    @Query("SELECT translated FROM name_translations WHERE source = :source AND language = :language")
    suspend fun getTranslation(source: String, language: String): String?

    /** Fresh translations replace old ones. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(translations: List<NameTranslationEntity>)

    /** Keeps an existing translation; used to note that a typed name is already in the language. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(translations: List<NameTranslationEntity>)
}
