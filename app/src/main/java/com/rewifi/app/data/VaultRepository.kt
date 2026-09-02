package com.rewifi.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** Plain (decrypted) view of an entry for the UI layer. */
data class WifiCred(
    val id: Long,
    val ssid: String,
    val password: String,
    val note: String?,
    val createdAt: Long,
    val isFavorite: Boolean = false,
    val category: String = "Other",
    val lastConnectedAt: Long? = null,
    val connectionCount: Int = 0
)

class VaultRepository(private val dao: WifiDao) {

    val creds: Flow<List<WifiCred>> = dao.observeAll().map { list ->
        list.map { it.toCred() }
    }

    /** Number of saved networks — used to refuse uploading an empty vault over a real backup. */
    suspend fun count(): Int = dao.count()

    suspend fun upsert(id: Long, ssid: String, password: String, note: String?, category: String = "Other") {
        val enc = Crypto.encrypt(password)
        val cleanNote = note?.trim()?.ifBlank { null }
        val cleanCategory = category.trim().ifBlank { "Other" }
        if (id == 0L) {
            dao.insert(WifiEntry(ssid = ssid.trim(), passwordEnc = enc, note = cleanNote, isFavorite = false, category = cleanCategory))
        } else {
            val existing = dao.byId(id) ?: return
            dao.update(existing.copy(ssid = ssid.trim(), passwordEnc = enc, note = cleanNote, category = cleanCategory))
        }
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        dao.setFavorite(id, isFavorite)
    }

    suspend fun reassignCategoryToOther(category: String) {
        dao.reassignCategoryToOther(category)
    }

    suspend fun renameCategory(oldCategory: String, newCategory: String) {
        dao.renameCategory(oldCategory, newCategory)
    }

    suspend fun delete(id: Long) {
        dao.byId(id)?.let { dao.delete(it) }
    }

    /** Insert a scanned network, skipping it if that SSID is already saved. Returns true if added. */
    suspend fun addIfNew(ssid: String, password: String): Boolean {
        val clean = ssid.trim()
        if (clean.isEmpty() || dao.countBySsid(clean) > 0) return false
        dao.insert(WifiEntry(ssid = clean, passwordEnc = Crypto.encrypt(password), note = null, isFavorite = false, category = "Other"))
        return true
    }

    suspend fun findDuplicates(ssid: String, excludeId: Long = 0L): List<WifiCred> {
        val clean = ssid.trim()
        if (clean.isEmpty()) return emptyList()
        return dao.findBySsidIgnoreCase(clean)
            .filter { it.id != excludeId }
            .map { it.toCred() }
    }

    suspend fun recordConnection(id: Long, timestamp: Long) {
        dao.recordConnection(id, timestamp)
    }

    suspend fun recordConnectionForSsid(ssid: String, timestamp: Long) {
        dao.recordConnectionBySsid(ssid.trim(), timestamp)
    }

    suspend fun updateExisting(
        targetId: Long,
        newPassword: String,
        newNote: String?,
        newCategory: String? = null
    ): Boolean {
        val existing = dao.byId(targetId) ?: return false
        val updatedNote = if (!newNote.isNullOrBlank()) newNote.trim() else existing.note
        val updatedCategory = if (!newCategory.isNullOrBlank() && newCategory != "Other") newCategory else existing.category
        val updatedEntry = existing.copy(
            passwordEnc = Crypto.encrypt(newPassword),
            note = updatedNote,
            category = updatedCategory
        )
        dao.update(updatedEntry)
        return true
    }

    /** Decrypt the whole vault and re-pack as a plaintext JSON snapshot. */
    private suspend fun snapshotJson(customCategories: List<String> = emptyList()): String {
        val items = JSONArray()
        dao.all().forEach { e ->
            val pw = runCatching { Crypto.decrypt(e.passwordEnc) }.getOrDefault("")
            items.put(
                JSONObject()
                    .put("ssid", e.ssid)
                    .put("pw", pw)
                    .put("note", e.note ?: "")
                    .put("fav", e.isFavorite)
                    .put("cat", e.category)
                    .put("last_conn", e.lastConnectedAt ?: 0L)
                    .put("conn_cnt", e.connectionCount)
            )
        }
        val customCats = JSONArray()
        customCategories.forEach { customCats.put(it) }
        return JSONObject()
            .put("v", 1)
            .put("items", items)
            .put("custom_cats", customCats)
            .toString()
    }

    /** Decrypt the whole vault, re-pack as JSON, then passphrase-encrypt for portability. */
    suspend fun exportEncrypted(passphrase: String, customCategories: List<String> = emptyList()): ByteArray =
        BackupCrypto.encrypt(snapshotJson(customCategories).toByteArray(Charsets.UTF_8), passphrase.toCharArray())

    /**
     * Device-bound (hardware Keystore) encrypted snapshot for *silent* local
     * auto-backup — no passphrase prompt. Stays on-device; to survive a reinstall
     * or factory reset use the passphrase export / Drive sync instead.
     */
    suspend fun autoBackupBlob(customCategories: List<String> = emptyList()): ByteArray =
        Crypto.encrypt(snapshotJson(customCategories)).toByteArray(Charsets.UTF_8)

    /**
     * Decrypt a backup and merge it in: new SSIDs are added, and existing ones are
     * updated when their password, note, favorite, category, or usage metadata changed.
     * Backwards-compatible: older backups without "fav" default to false, "cat" defaults to "Other",
     * "last_conn" defaults to null, and "conn_cnt" defaults to 0.
     * Returns the number of entries added or updated.
     */
    suspend fun importEncrypted(
        blob: ByteArray,
        passphrase: String,
        onImportCustomCategories: (List<String>) -> Unit = {}
    ): Int {
        val json = String(BackupCrypto.decrypt(blob, passphrase.toCharArray()), Charsets.UTF_8)
        val root = JSONObject(json)
        val items = root.getJSONArray("items")
        val customCatsArr = root.optJSONArray("custom_cats")
        if (customCatsArr != null) {
            val customCats = mutableListOf<String>()
            for (i in 0 until customCatsArr.length()) {
                val cat = customCatsArr.optString(i, "").trim()
                if (cat.isNotEmpty()) customCats.add(cat)
            }
            onImportCustomCategories(customCats)
        }
        var changed = 0
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val ssid = o.getString("ssid").trim()
            if (ssid.isEmpty()) continue
            val pw = o.getString("pw")
            val note = o.optString("note", "").trim().ifBlank { null }
            val fav = o.optBoolean("fav", false)
            val cat = o.optString("cat", "Other").trim().ifBlank { "Other" }
            val lastConnRaw = o.optLong("last_conn", 0L)
            val lastConn = if (lastConnRaw > 0L) lastConnRaw else null
            val connCnt = o.optInt("conn_cnt", 0)

            val existing = dao.bySsid(ssid)
            if (existing == null) {
                dao.insert(
                    WifiEntry(
                        ssid = ssid,
                        passwordEnc = Crypto.encrypt(pw),
                        note = note,
                        isFavorite = fav,
                        category = cat,
                        lastConnectedAt = lastConn,
                        connectionCount = connCnt
                    )
                )
                changed++
            } else {
                val curPw = runCatching { Crypto.decrypt(existing.passwordEnc) }.getOrDefault("")
                val shouldUpdateLastConn = lastConn != null && (existing.lastConnectedAt == null || lastConn > existing.lastConnectedAt)
                val newLastConn = if (shouldUpdateLastConn) lastConn else existing.lastConnectedAt
                val newConnCnt = maxOf(existing.connectionCount, connCnt)

                if (curPw != pw || existing.note != note || existing.isFavorite != fav || existing.category != cat ||
                    existing.lastConnectedAt != newLastConn || existing.connectionCount != newConnCnt
                ) {
                    dao.update(
                        existing.copy(
                            passwordEnc = Crypto.encrypt(pw),
                            note = note,
                            isFavorite = fav,
                            category = cat,
                            lastConnectedAt = newLastConn,
                            connectionCount = newConnCnt
                        )
                    )
                    changed++
                }
            }
        }
        return changed
    }

    private fun WifiEntry.toCred(): WifiCred =
        WifiCred(id, ssid, runCatching { Crypto.decrypt(passwordEnc) }.getOrDefault("••••"), note, createdAt, isFavorite, category, lastConnectedAt, connectionCount)
}
