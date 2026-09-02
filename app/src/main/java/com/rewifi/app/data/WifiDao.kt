package com.rewifi.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiDao {
    @Query("SELECT * FROM wifi_entries ORDER BY ssid COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WifiEntry>>

    @Query("SELECT * FROM wifi_entries")
    suspend fun all(): List<WifiEntry>

    @Query("SELECT COUNT(*) FROM wifi_entries")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM wifi_entries WHERE ssid = :ssid")
    suspend fun countBySsid(ssid: String): Int

    @Query("SELECT * FROM wifi_entries WHERE ssid = :ssid LIMIT 1")
    suspend fun bySsid(ssid: String): WifiEntry?

    @Query("SELECT * FROM wifi_entries WHERE LOWER(TRIM(ssid)) = LOWER(TRIM(:ssid))")
    suspend fun findBySsidIgnoreCase(ssid: String): List<WifiEntry>

    @Query("SELECT * FROM wifi_entries WHERE id = :id")
    suspend fun byId(id: Long): WifiEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WifiEntry): Long

    @Update
    suspend fun update(entry: WifiEntry)

    @Query("UPDATE wifi_entries SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE wifi_entries SET lastConnectedAt = :timestamp, connectionCount = connectionCount + 1 WHERE id = :id")
    suspend fun recordConnection(id: Long, timestamp: Long)

    @Query("UPDATE wifi_entries SET lastConnectedAt = :timestamp, connectionCount = connectionCount + 1 WHERE LOWER(TRIM(ssid)) = LOWER(TRIM(:ssid))")
    suspend fun recordConnectionBySsid(ssid: String, timestamp: Long)

    @Query("UPDATE wifi_entries SET category = 'Other' WHERE category = :oldCategory")
    suspend fun reassignCategoryToOther(oldCategory: String)

    @Query("UPDATE wifi_entries SET category = :newCategory WHERE category = :oldCategory")
    suspend fun renameCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE wifi_entries SET isFavorite = :isFavorite WHERE id IN (:ids)")
    suspend fun setFavoriteBulk(ids: List<Long>, isFavorite: Boolean)

    @Query("UPDATE wifi_entries SET category = :category WHERE id IN (:ids)")
    suspend fun setCategoryBulk(ids: List<Long>, category: String)

    @Query("DELETE FROM wifi_entries WHERE id IN (:ids)")
    suspend fun deleteBulk(ids: List<Long>)

    @Delete
    suspend fun delete(entry: WifiEntry)
}
