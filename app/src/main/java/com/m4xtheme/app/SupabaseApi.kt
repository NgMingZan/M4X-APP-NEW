package com.m4xtheme.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    val url: String get() = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    val key: String get() = BuildConfig.SUPABASE_ANON_KEY.trim()
    val configured: Boolean get() = url.startsWith("https://") && key.length > 20
}

data class Session(val token: String, val userId: String, val email: String)
data class Profile(val id: String, val username: String, val displayName: String, val role: String, val points: Long)
data class ThemeItem(
    val id: String,
    val ownerId: String,
    val title: String,
    val description: String,
    val category: String,
    val osVersion: String,
    val fileUrl: String,
    val previewUrl: String,
    val status: String,
    val rejectReason: String,
    val downloads: Long,
    val rating: Double,
    val createdAt: String
)
data class RemoteConfig(
    val minVersionCode: Int = 0,
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val updateUrl: String = "",
    val updateMessage: String = "",
    val forceUpdate: Boolean = false,
    val homeBannerTitle: String = "M4X Theme",
    val homeBannerSubtitle: String = "Kho giao diện HyperOS & MIUI"
)

class SupabaseApi(private val context: Context) {
    private val jsonType = "application/json; charset=utf-8".toMediaType()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()
    private val prefs = context.getSharedPreferences("m4x_v2_session", Context.MODE_PRIVATE)

    fun restoreSession(): Session? {
        val token = prefs.getString("token", null) ?: return null
        val uid = prefs.getString("uid", null) ?: return null
        return Session(token, uid, prefs.getString("email", "").orEmpty())
    }

    fun saveSession(session: Session) {
        prefs.edit().putString("token", session.token).putString("uid", session.userId)
            .putString("email", session.email).apply()
    }

    fun signOut() = prefs.edit().clear().apply()

    suspend fun signUp(email: String, password: String, username: String, displayName: String): Result<Session> = io {
        require(email.isNotBlank() && password.length >= 6) { "Email hoặc mật khẩu chưa hợp lệ" }
        require(username.matches(Regex("[A-Za-z0-9_]{3,24}"))) { "Tên đăng nhập gồm 3–24 chữ, số hoặc dấu gạch dưới" }
        val body = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("data", JSONObject().apply {
                put("username", username.trim().lowercase())
                put("display_name", displayName.trim())
            })
        }
        auth("/auth/v1/signup", body)
    }

    suspend fun signIn(email: String, password: String): Result<Session> = io {
        auth("/auth/v1/token?grant_type=password", JSONObject().put("email", email.trim()).put("password", password))
    }

    suspend fun profile(session: Session): Result<Profile> = io {
        val a = get("/rest/v1/profiles?id=eq.${session.userId}&select=id,username,display_name,role,points", session)
        if (a.length() == 0) throw IOException("Không tìm thấy hồ sơ. Hãy kiểm tra trigger profiles trong Supabase")
        val o = a.getJSONObject(0)
        Profile(o.getString("id"), o.optString("username"), o.optString("display_name"), o.optString("role", "user"), o.optLong("points"))
    }

    suspend fun approvedThemes(session: Session): Result<List<ThemeItem>> = io {
        parseThemes(get("/rest/v1/themes?status=eq.approved&select=*&order=created_at.desc", session))
    }

    suspend fun myThemes(session: Session): Result<List<ThemeItem>> = io {
        parseThemes(get("/rest/v1/themes?owner_id=eq.${session.userId}&select=*&order=created_at.desc", session))
    }

    suspend fun pendingThemes(session: Session): Result<List<ThemeItem>> = io {
        parseThemes(get("/rest/v1/themes?status=eq.pending&select=*&order=created_at.asc", session))
    }

    suspend fun users(session: Session): Result<List<Profile>> = io {
        val a = get("/rest/v1/profiles?select=id,username,display_name,role,points&order=created_at.desc", session)
        List(a.length()) { i -> a.getJSONObject(i).let { o ->
            Profile(o.getString("id"), o.optString("username"), o.optString("display_name"), o.optString("role", "user"), o.optLong("points"))
        } }
    }

    suspend fun uploadTheme(
        session: Session,
        fileUri: Uri,
        title: String,
        description: String,
        category: String,
        osVersion: String
    ): Result<Unit> = io {
        require(title.isNotBlank()) { "Chưa nhập tên theme" }
        val name = fileName(context.contentResolver, fileUri)
        require(name.endsWith(".mtz", true) || name.endsWith(".zip", true)) { "Chỉ nhận file .mtz hoặc .zip" }
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val path = "${session.userId}/${UUID.randomUUID()}_$safeName"
        uploadFile(session, fileUri, path)
        val publicUrl = "${SupabaseConfig.url}/storage/v1/object/public/themes/$path"
        val row = JSONObject().apply {
            put("owner_id", session.userId)
            put("title", title.trim())
            put("description", description.trim())
            put("category", category.trim())
            put("os_version", osVersion.trim())
            put("file_url", publicUrl)
            put("preview_url", "")
            put("status", "pending")
        }
        post("/rest/v1/themes", JSONArray().put(row).toString(), session)
    }

    suspend fun reviewTheme(session: Session, id: String, approved: Boolean, reason: String = ""): Result<Unit> = io {
        val json = JSONObject().put("status", if (approved) "approved" else "rejected")
            .put("reject_reason", if (approved) "" else reason.ifBlank { "Theme chưa đạt yêu cầu" })
        patch("/rest/v1/themes?id=eq.$id", json.toString(), session)
    }

    suspend fun deleteTheme(session: Session, id: String): Result<Unit> = io {
        execute(base("${SupabaseConfig.url}/rest/v1/themes?id=eq.$id", session).delete().build())
    }

    suspend fun setRole(session: Session, userId: String, role: String): Result<Unit> = io {
        require(role in setOf("user", "creator", "admin", "banned"))
        val body = JSONObject().put("target_user_id", userId).put("new_role", role)
        execute(base("${SupabaseConfig.url}/rest/v1/rpc/set_user_role", session).post(body.toString().toRequestBody(jsonType)).build())
    }

    suspend fun incrementDownload(session: Session, themeId: String): Result<Unit> = io {
        val body = JSONObject().put("theme_id", themeId)
        execute(base("${SupabaseConfig.url}/rest/v1/rpc/increment_theme_download", session).post(body.toString().toRequestBody(jsonType)).build())
    }

    suspend fun remoteConfig(session: Session): Result<RemoteConfig> = io {
        val a = get("/rest/v1/app_config?id=eq.main&select=*", session)
        if (a.length() == 0) return@io RemoteConfig()
        val o = a.getJSONObject(0)
        RemoteConfig(
            minVersionCode = o.optInt("min_version_code"),
            latestVersionCode = o.optInt("latest_version_code"),
            latestVersionName = o.optString("latest_version_name"),
            updateUrl = o.optString("update_url"),
            updateMessage = o.optString("update_message"),
            forceUpdate = o.optBoolean("force_update"),
            homeBannerTitle = o.optString("home_banner_title", "M4X Theme"),
            homeBannerSubtitle = o.optString("home_banner_subtitle", "Kho giao diện HyperOS & MIUI")
        )
    }

    private fun auth(path: String, body: JSONObject): Session {
        checkConfig()
        val req = Request.Builder().url(SupabaseConfig.url + path)
            .header("apikey", SupabaseConfig.key).header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Xác thực thất bại (${res.code})"))
            val json = JSONObject(text)
            val token = json.optString("access_token")
            if (token.isBlank()) throw IOException("Hãy xác nhận email rồi đăng nhập")
            val user = json.getJSONObject("user")
            return Session(token, user.getString("id"), user.optString("email"))
        }
    }

    private fun uploadFile(session: Session, uri: Uri, path: String) {
        checkConfig()
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val body = object : RequestBody() {
            override fun contentType() = mime.toMediaType()
            override fun contentLength() = fileSize(resolver, uri)
            override fun writeTo(sink: BufferedSink) {
                resolver.openInputStream(uri)?.use { sink.writeAll(it.source()) }
                    ?: throw IOException("Không mở được file đã chọn")
            }
        }
        execute(base("${SupabaseConfig.url}/storage/v1/object/themes/$path", session)
            .header("x-upsert", "false").post(body).build())
    }

    private fun get(path: String, session: Session): JSONArray {
        val req = base(SupabaseConfig.url + path, session).get().build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không tải được dữ liệu (${res.code})"))
            return JSONArray(text)
        }
    }

    private fun post(path: String, body: String, session: Session) = execute(
        base(SupabaseConfig.url + path, session).header("Prefer", "return=minimal")
            .post(body.toRequestBody(jsonType)).build()
    )

    private fun patch(path: String, body: String, session: Session) = execute(
        base(SupabaseConfig.url + path, session).header("Prefer", "return=minimal")
            .patch(body.toRequestBody(jsonType)).build()
    )

    private fun base(url: String, session: Session) = Request.Builder().url(url)
        .header("apikey", SupabaseConfig.key)
        .header("Authorization", "Bearer ${session.token}")
        .header("Content-Type", "application/json")

    private fun execute(req: Request) {
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Yêu cầu thất bại (${res.code})"))
        }
    }

    private fun parseThemes(a: JSONArray) = List(a.length()) { i ->
        a.getJSONObject(i).let { o ->
            ThemeItem(
                id = o.getString("id"), ownerId = o.optString("owner_id"), title = o.optString("title"),
                description = o.optString("description"), category = o.optString("category"), osVersion = o.optString("os_version"),
                fileUrl = o.optString("file_url"), previewUrl = o.optString("preview_url"), status = o.optString("status", "pending"),
                rejectReason = o.optString("reject_reason"), downloads = o.optLong("downloads"), rating = o.optDouble("rating"),
                createdAt = o.optString("created_at")
            )
        }
    }

    private fun checkConfig() {
        if (!SupabaseConfig.configured) throw IOException("Chưa cấu hình SUPABASE_URL và SUPABASE_ANON_KEY")
    }

    private fun error(text: String, fallback: String): String = runCatching {
        JSONObject(text).optString("msg").ifBlank { JSONObject(text).optString("message") }.ifBlank { fallback }
    }.getOrDefault(fallback)

    private suspend fun <T> io(block: () -> T): Result<T> = withContext(Dispatchers.IO) { runCatching(block) }

    companion object {
        fun fileName(resolver: ContentResolver, uri: Uri): String {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) return it.getString(0) ?: "theme.mtz"
            }
            return uri.lastPathSegment ?: "theme.mtz"
        }

        private fun fileSize(resolver: ContentResolver, uri: Uri): Long {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
                if (it.moveToFirst() && !it.isNull(0)) return it.getLong(0)
            }
            return -1L
        }
    }
}
