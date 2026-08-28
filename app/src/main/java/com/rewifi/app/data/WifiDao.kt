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

    @Query("SELECT * FROM wifi_entries WHERE id = :id")
    suspend fun byId(id: Long): WifiEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WifiEntry): Long

    @Update
    suspend fun update(entry: WifiEntry)

    @Delete
    suspend fun delete(entry: WifiEntry)
}
