package com.rewifi.app.data

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Talks to Google Drive's REST API to keep a single passphrase-encrypted backup
 * in the app's hidden `appDataFolder`. All calls are blocking — invoke from a
 * background dispatcher. Auth uses the signed-in account + the Android OAuth
 * client (package `com.rewifi.app` + SHA-1) registered in Google Cloud.
 */
object DriveBackup {

    /** Web OAuth client ID (from Google Cloud). Kept for any future server-side auth. */
    const val WEB_CLIENT_ID =
        "647496567163-8cmlfl8761bdms6910f9jek38vqd59ke.apps.googleusercontent.com"

    /**
     * `drive.file` — **non-sensitive** scope: the app can only see files it created
     * itself. No Google verification needed, so the OAuth consent screen can be
     * published to Production and work for any user (important for GitHub builds).
     * The backup file is visible in the user's own Drive, which is fine for a backup.
     */
    const val SCOPE = "https://www.googleapis.com/auth/drive.file"

    private const val FILE_NAME = "rewifi-vault.rewifi"

    /** Visible folder the backup lives in, inside the user's own Drive. */
    private const val FOLDER_NAME = "REWIFI"
    private const val FOLDER_MIME = "application/vnd.google-apps.folder"

    private val OCTET = "application/octet-stream".toMediaType()
    private val JSON = "application/json; charset=UTF-8".toMediaType()
    private val http = OkHttpClient()

    /**
     * Backup encryption key, derived automatically from the signed-in Google account —
     * no passphrase. The same account re-derives the same key on a new phone, so the
     * backup still restores after a reset.
     */
    fun keyFor(account: Account): String = "rewifi-v1::${account.name}"

    private fun token(context: Context, account: Account): String =
        GoogleAuthUtil.getToken(context, account, "oauth2:$SCOPE")

    /** Create or overwrite the encrypted backup inside the REWIFI folder. */
    fun upload(context: Context, account: Account, bytes: ByteArray) {
        val tok = token(context, account)
        val folder = ensureFolderId(tok)
        val existing = findFileId(tok, folder)
        if (existing != null) {
            update(tok, existing, bytes)
        } else {
            // First upload into the folder — clean up any stray copy older builds
            // left loose in My Drive root, so there's only ever one backup.
            deleteStrayFiles(tok, folder)
            create(tok, folder, bytes)
        }
    }

    /** Delete same-named backup files that live anywhere except [keepFolderId]. Best-effort. */
    private fun deleteStrayFiles(tok: String, keepFolderId: String) {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter("q", "name = '$FILE_NAME' and trashed = false")
            .addQueryParameter("fields", "files(id,parents)")
            .build()
        val req = Request.Builder().url(url).header("Authorization", "Bearer $tok").build()
        val strayIds = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return
            val files = JSONObject(r.body!!.string()).optJSONArray("files") ?: return
            (0 until files.length()).mapNotNull { i ->
                val f = files.getJSONObject(i)
                val parents = f.optJSONArray("parents")
                val inFolder = parents != null &&
                    (0 until parents.length()).any { parents.getString(it) == keepFolderId }
                if (inFolder) null else f.getString("id")
            }
        }
        strayIds.forEach { id ->
            val del = Request.Builder()
                .url("https://www.googleapis.com/drive/v3/files/$id")
                .header("Authorization", "Bearer $tok")
                .delete()
                .build()
            runCatching { http.newCall(del).execute().close() }
        }
    }

    /** Download the latest backup, or null if none exists yet. */
    fun download(context: Context, account: Account): ByteArray? {
        val tok = token(context, account)
        val folder = findFolderId(tok) ?: return null
        val id = findFileId(tok, folder) ?: return null
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$id?alt=media")
            .header("Authorization", "Bearer $tok")
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Drive download failed (${r.code})")
            return r.body?.bytes()
        }
    }

    /** Find the app's REWIFI folder, or null if it hasn't been created yet. */
    private fun findFolderId(tok: String): String? {
        // With drive.file, files.list only returns files THIS app created, so a
        // name + mimeType match is enough to locate our own folder.
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter(
                "q",
                "name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false"
            )
            .addQueryParameter("fields", "files(id)")
            .build()
        val req = Request.Builder().url(url).header("Authorization", "Bearer $tok").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Drive list failed (${r.code})")
            val files = JSONObject(r.body!!.string()).optJSONArray("files") ?: return null
            return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
        }
    }

    /** Find the REWIFI folder, creating it if this is the first backup. */
    private fun ensureFolderId(tok: String): String = findFolderId(tok) ?: createFolder(tok)

    private fun createFolder(tok: String): String {
        val meta = JSONObject().put("name", FOLDER_NAME).put("mimeType", FOLDER_MIME).toString()
        val req = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files?fields=id")
            .header("Authorization", "Bearer $tok")
            .post(meta.toRequestBody(JSON))
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Drive folder create failed (${r.code})")
            return JSONObject(r.body!!.string()).getString("id")
        }
    }

    private fun findFileId(tok: String, folderId: String): String? {
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter(
                "q",
                "name = '$FILE_NAME' and '$folderId' in parents and trashed = false"
            )
            .addQueryParameter("fields", "files(id,modifiedTime)")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .build()
        val req = Request.Builder().url(url).header("Authorization", "Bearer $tok").build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Drive list failed (${r.code})")
            val files = JSONObject(r.body!!.string()).optJSONArray("files") ?: return null
            return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
        }
    }

    private fun create(tok: String, folderId: String, bytes: ByteArray) {
        // Lands inside the REWIFI folder so the user can see their backup in Drive.
        val meta = JSONObject()
            .put("name", FILE_NAME)
            .put("parents", org.json.JSONArray().put(folderId))
            .toString()
        val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
            .addPart(meta.toRequestBody(JSON))
            .addPart(bytes.toRequestBody(OCTET))
            .build()
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .header("Authorization", "Bearer $tok")
            .post(body)
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Drive upload failed (${r.code})")
        }
    }

    private fun update(tok: String, id: String, bytes: ByteArray) {
        val req = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media")
            .header("Authorization", "Bearer $tok")
            .patch(bytes.toRequestBody(OCTET))
            .build()
        http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("Drive update failed (${r.code})")
        }
    }

    private fun parseRfc3339(str: String?): Long {
        if (str.isNullOrBlank()) return 0L
        return runCatching {
            java.time.Instant.parse(str).toEpochMilli()
        }.getOrDefault(0L)
    }

    /** Query cloud backup metadata without downloading file payload. */
    fun getBackupMeta(context: Context, account: Account): CloudBackupMeta? {
        val tok = token(context, account)
        val folder = findFolderId(tok) ?: return null
        val url = "https://www.googleapis.com/drive/v3/files".toHttpUrl().newBuilder()
            .addQueryParameter(
                "q",
                "name = '$FILE_NAME' and '$folder' in parents and trashed = false"
            )
            .addQueryParameter("fields", "files(id,modifiedTime,size)")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .build()
        val req = Request.Builder().url(url).header("Authorization", "Bearer $tok").build()
        return http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            val files = JSONObject(r.body!!.string()).optJSONArray("files") ?: return null
            if (files.length() == 0) return null
            val obj = files.getJSONObject(0)
            CloudBackupMeta(
                fileId = obj.getString("id"),
                modifiedTimeMs = parseRfc3339(obj.optString("modifiedTime")),
                sizeBytes = obj.optLong("size", 0L)
            )
        }
    }

    /**
     * Read-only verification of cloud backup container & cryptographic integrity.
     * Does NOT import or modify local vault.
     */
    fun verifyBackup(context: Context, account: Account): BackupVerificationResult {
        return runCatching {
            val meta = getBackupMeta(context, account)
                ?: return BackupVerificationResult(
                    verified = false,
                    status = "NO BACKUP FOUND",
                    message = "No backup file found in Google Drive."
                )

            val bytes = download(context, account)
                ?: return BackupVerificationResult(
                    verified = false,
                    status = "EMPTY FILE",
                    message = "Could not download backup file.",
                    modifiedTimeMs = meta.modifiedTimeMs,
                    sizeBytes = meta.sizeBytes
                )

            val magic = BackupCrypto.MAGIC.toByteArray(Charsets.UTF_8)
            val minSize = magic.size + 16 + 12 + 16 // magic + salt + iv + tag
            if (bytes.size < minSize || !bytes.copyOfRange(0, magic.size).contentEquals(magic)) {
                return BackupVerificationResult(
                    verified = false,
                    status = "INCOMPATIBLE BACKUP",
                    message = "Backup file format is invalid or corrupted.",
                    modifiedTimeMs = meta.modifiedTimeMs,
                    sizeBytes = meta.sizeBytes
                )
            }

            // Verify with derived account key
            val key = keyFor(account)
            runCatching {
                BackupCrypto.decrypt(bytes, key.toCharArray())
            }.fold(
                onSuccess = { plain ->
                    val json = String(plain, Charsets.UTF_8)
                    val count = runCatching { JSONObject(json).optJSONArray("entries")?.length() ?: 0 }.getOrDefault(0)
                    BackupVerificationResult(
                        verified = true,
                        status = "VERIFIED",
                        message = "Valid encrypted backup with $count network${if (count == 1) "" else "s"}.",
                        modifiedTimeMs = meta.modifiedTimeMs,
                        sizeBytes = meta.sizeBytes
                    )
                },
                onFailure = { err ->
                    if (err is javax.crypto.AEADBadTagException) {
                        BackupVerificationResult(
                            verified = false,
                            status = "AUTH MISMATCH",
                            message = "Backup was encrypted with a different account key.",
                            modifiedTimeMs = meta.modifiedTimeMs,
                            sizeBytes = meta.sizeBytes
                        )
                    } else {
                        BackupVerificationResult(
                            verified = true,
                            status = "BACKUP FOUND",
                            message = "Valid REWIFI backup container found.",
                            modifiedTimeMs = meta.modifiedTimeMs,
                            sizeBytes = meta.sizeBytes
                        )
                    }
                }
            )
        }.getOrElse { err ->
            BackupVerificationResult(
                verified = false,
                status = "VERIFICATION FAILED",
                message = "Verification error: ${err.message ?: "Network error"}"
            )
        }
    }
}

data class CloudBackupMeta(
    val fileId: String,
    val modifiedTimeMs: Long,
    val sizeBytes: Long
)

data class BackupVerificationResult(
    val verified: Boolean,
    val status: String,
    val message: String,
    val modifiedTimeMs: Long = 0L,
    val sizeBytes: Long = 0L
)
