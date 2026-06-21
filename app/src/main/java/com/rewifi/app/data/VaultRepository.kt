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
    val createdAt: Long
)

class VaultRepository(private val dao: WifiDao) {

    val creds: Flow<List<WifiCred>> = dao.observeAll().map { list ->
        list.map { it.toCred() }
    }

    /** Number of saved networks — used to refuse uploading an empty vault over a real backup. */
    suspend fun count(): Int = dao.count()

    suspend fun upsert(id: Long, ssid: String, password: String, note: String?) {
        val enc = Crypto.encrypt(password)
        val cleanNote = note?.trim()?.ifBlank { null }
        if (id == 0L) {
            dao.insert(WifiEntry(ssid = ssid.trim(), passwordEnc = enc, note = cleanNote))
        } else {
            val existing = dao.byId(id) ?: return
            dao.update(existing.copy(ssid = ssid.trim(), passwordEnc = enc, note = cleanNote))
        }
    }

    suspend fun delete(id: Long) {
        dao.byId(id)?.let { dao.delete(it) }
    }

    /** Insert a scanned network, skipping it if that SSID is already saved. Returns true if added. */
    suspend fun addIfNew(ssid: String, password: String): Boolean {
        val clean = ssid.trim()
        if (clean.isEmpty() || dao.countBySsid(clean) > 0) return false
        dao.insert(WifiEntry(ssid = clean, passwordEnc = Crypto.encrypt(password), note = null))
        return true
    }

    /** Decrypt the whole vault and re-pack as a plaintext JSON snapshot. */
    private suspend fun snapshotJson(): String {
        val items = JSONArray()
        dao.all().forEach { e ->
            val pw = runCatching { Crypto.decrypt(e.passwordEnc) }.getOrDefault("")
            items.put(JSONObject().put("ssid", e.ssid).put("pw", pw).put("note", e.note ?: ""))
        }
        return JSONObject().put("v", 1).put("items", items).toString()
    }

    /** Decrypt the whole vault, re-pack as JSON, then passphrase-encrypt for portability. */
    suspend fun exportEncrypted(passphrase: String): ByteArray =
        BackupCrypto.encrypt(snapshotJson().toByteArray(Charsets.UTF_8), passphrase.toCharArray())

    /**
     * Device-bound (hardware Keystore) encrypted snapshot for *silent* local
     * auto-backup — no passphrase prompt. Stays on-device; to survive a reinstall
     * or factory reset use the passphrase export / Drive sync instead.
     */
    suspend fun autoBackupBlob(): ByteArray =
        Crypto.encrypt(snapshotJson()).toByteArray(Charsets.UTF_8)

    /**
     * Decrypt a backup and merge it in: new SSIDs are added, and existing ones are
     * updated when their password or note changed (instead of being skipped). Returns
     * the number of entries added or updated.
     */
    suspend fun importEncrypted(blob: ByteArray, passphrase: String): Int {
        val json = String(BackupCrypto.decrypt(blob, passphrase.toCharArray()), Charsets.UTF_8)
        val items = JSONObject(json).getJSONArray("items")
        var changed = 0
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val ssid = o.getString("ssid").trim()
            if (ssid.isEmpty()) continue
            val pw = o.getString("pw")
            val note = o.optString("note", "").trim().ifBlank { null }
            val existing = dao.bySsid(ssid)
            if (existing == null) {
                dao.insert(WifiEntry(ssid = ssid, passwordEnc = Crypto.encrypt(pw), note = note))
                changed++
            } else {
                val curPw = runCatching { Crypto.decrypt(existing.passwordEnc) }.getOrDefault("")
                if (curPw != pw || existing.note != note) {
                    dao.update(existing.copy(passwordEnc = Crypto.encrypt(pw), note = note))
                    changed++
                }
            }
        }
        return changed
    }

    private fun WifiEntry.toCred(): WifiCred =
        WifiCred(id, ssid, runCatching { Crypto.decrypt(passwordEnc) }.getOrDefault("••••"), note, createdAt)
}
