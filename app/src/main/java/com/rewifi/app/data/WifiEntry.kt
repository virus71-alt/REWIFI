package com.rewifi.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_entries")
data class WifiEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ssid: String,
    /** Stored as Base64(iv||ciphertext); never plaintext. */
    val passwordEnc: String,
    /** Optional free-text note, e.g. "cafe near the park, ask waiter". */
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
