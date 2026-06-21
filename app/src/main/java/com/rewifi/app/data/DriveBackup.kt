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
        "879087804730-lkmsma0ch4p81e90ogpv3n5m2j770s1h.apps.googleusercontent.com"

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
}
