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
    val previewUrls: List<String>,
    val driveUrl: String,
    val status: String,
    val rejectReason: String,
    val downloads: Long,
    val rating: Double,
    val createdAt: String,
    val coinPrice: Int = 0
)

data class EventItem(val id: String, val title: String, val description: String, val startAt: String, val endAt: String, val active: Boolean)
data class QuestItem(val id: String, val title: String, val description: String, val reward: Int)
data class LeaderboardItem(val rank: Int, val displayName: String, val score: Long)
data class InventoryItem(val id: String, val name: String, val type: String)

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
        fileUri: Uri?,
        previewUris: List<Uri>,
        driveUrl: String,
        title: String,
        description: String,
        category: String,
        osVersion: String,
        tags: String,
        adminNote: String,
        coinPrice: Int = 0
    ): Result<Unit> = io {
        require(title.isNotBlank()) { "Chưa nhập tên theme" }
        require(fileUri != null || driveUrl.isNotBlank()) { "Hãy chọn file hoặc dán link Google Drive" }
        if (driveUrl.isNotBlank()) require(driveUrl.startsWith("https://")) { "Link Drive phải bắt đầu bằng https://" }

        var publicUrl = ""
        if (fileUri != null) {
            val name = fileName(context.contentResolver, fileUri)
            require(name.endsWith(".mtz", true) || name.endsWith(".zip", true)) { "Chỉ nhận file .mtz hoặc .zip" }
            val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val path = "${session.userId}/files/${UUID.randomUUID()}_$safeName"
            uploadFile(session, fileUri, path)
            publicUrl = "${SupabaseConfig.url}/storage/v1/object/public/themes/$path"
        }

        val previews = previewUris.take(5).mapIndexed { index, uri ->
            val original = fileName(context.contentResolver, uri)
            val ext = original.substringAfterLast('.', "jpg").lowercase().take(5)
            val path = "${session.userId}/previews/${UUID.randomUUID()}_${index}.$ext"
            uploadFile(session, uri, path)
            "${SupabaseConfig.url}/storage/v1/object/public/themes/$path"
        }

        val row = JSONObject().apply {
            put("owner_id", session.userId)
            put("title", title.trim())
            put("description", description.trim())
            put("category", category.trim())
            put("os_version", osVersion.trim())
            put("tags", tags.trim())
            put("admin_note", adminNote.trim())
            put("file_url", publicUrl)
            put("drive_url", driveUrl.trim())
            put("preview_url", previews.firstOrNull().orEmpty())
            put("preview_urls", JSONArray(previews))
            put("status", "pending")
            put("coin_price", coinPrice.coerceAtLeast(0))
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

    suspend fun purchaseTheme(session: Session, themeId: String): Result<Unit> = io {
        val body = JSONObject().put("theme_id", themeId)
        execute(base("${SupabaseConfig.url}/rest/v1/rpc/purchase_theme", session).post(body.toString().toRequestBody(jsonType)).build())
    }

    suspend fun activeEvents(session: Session): Result<List<EventItem>> = io {
        parseEvents(get("/rest/v1/events?active=eq.true&select=*&order=start_at.desc", session))
    }

    suspend fun allEvents(session: Session): Result<List<EventItem>> = io {
        parseEvents(get("/rest/v1/events?select=*&order=created_at.desc", session))
    }

    suspend fun createEvent(session: Session, title: String, description: String): Result<Unit> = io {
        require(title.isNotBlank()) { "Chưa nhập tên sự kiện" }
        val row = JSONObject().put("title", title).put("description", description).put("active", true)
            .put("start_at", isoAfter(0)).put("end_at", isoAfter(604800))
        post("/rest/v1/events", JSONArray().put(row).toString(), session)
    }

    suspend fun activeQuests(session: Session): Result<List<QuestItem>> = io {
        val a = get("/rest/v1/quests?active=eq.true&select=id,title,description,reward&order=sort_order.asc", session)
        List(a.length()) { i -> a.getJSONObject(i).let { QuestItem(it.getString("id"), it.optString("title"), it.optString("description"), it.optInt("reward")) } }
    }

    suspend fun claimQuest(session: Session, questId: String): Result<Unit> = io {
        val body = JSONObject().put("quest_id", questId)
        execute(base("${SupabaseConfig.url}/rest/v1/rpc/claim_quest", session).post(body.toString().toRequestBody(jsonType)).build())
    }

    suspend fun redeemGiftCode(session: Session, code: String): Result<Int> = io {
        require(code.isNotBlank()) { "Chưa nhập Giftcode" }
        val body = JSONObject().put("gift_code", code.trim().uppercase())
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/redeem_giftcode", session).post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty(); if (!res.isSuccessful) throw IOException(error(text, "Giftcode không hợp lệ")); text.trim().trim('"').toIntOrNull() ?: 0
        }
    }

    suspend fun createGiftCode(session: Session, code: String, reward: Int): Result<Unit> = io {
        require(code.isNotBlank() && reward > 0) { "Mã hoặc phần thưởng không hợp lệ" }
        val row = JSONObject().put("code", code.trim().uppercase()).put("reward", reward).put("max_uses", 100).put("active", true)
        post("/rest/v1/giftcodes", JSONArray().put(row).toString(), session)
    }

    suspend fun weeklyLeaderboard(session: Session): Result<List<LeaderboardItem>> = io {
        val a = get("/rest/v1/weekly_leaderboard?select=rank,display_name,score&order=rank.asc&limit=20", session)
        List(a.length()) { i -> a.getJSONObject(i).let { LeaderboardItem(it.optInt("rank", i + 1), it.optString("display_name", "M4X Member"), it.optLong("score")) } }
    }

    suspend fun inventory(session: Session): Result<List<InventoryItem>> = io {
        val a = get("/rest/v1/user_inventory?user_id=eq.${session.userId}&select=id,item_name,item_type", session)
        List(a.length()) { i -> a.getJSONObject(i).let { InventoryItem(it.getString("id"), it.optString("item_name"), it.optString("item_type")) } }
    }

    suspend fun claimAirdrop(session: Session): Result<Int> = io {
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/claim_active_airdrop", session).post("{}".toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res -> val text = res.body?.string().orEmpty(); if (!res.isSuccessful) throw IOException(error(text, "Airdrop đã hết")); text.trim().trim('"').toIntOrNull() ?: 0 }
    }

    suspend fun createAirdrop(session: Session): Result<Unit> = io {
        val row = JSONObject().put("reward", (100..2000).random()).put("active", true).put("expires_at", isoAfter(3600))
        post("/rest/v1/airdrops", JSONArray().put(row).toString(), session)
    }

    suspend fun publishBirthdayWeek(session: Session): Result<Unit> = io {
        val row = JSONObject().put("title", "Tuần lễ sinh nhật Admin").put("description", "01/08–07/08: đăng nhập nhận quà, x2 nhiệm vụ, Giftcode bí mật, 4.080 M4X COIN, vòng quay, giảm 40% và Boss cộng đồng.").put("active", true).put("start_at", "2026-08-01T00:00:00Z").put("end_at", "2026-08-07T23:59:59Z")
        post("/rest/v1/events", JSONArray().put(row).toString(), session)
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

    private fun parseEvents(a: JSONArray) = List(a.length()) { i ->
        a.getJSONObject(i).let { EventItem(it.getString("id"), it.optString("title"), it.optString("description"), it.optString("start_at"), it.optString("end_at"), it.optBoolean("active")) }
    }

    private fun parseThemes(a: JSONArray) = List(a.length()) { i ->
        a.getJSONObject(i).let { o ->
            ThemeItem(
                id = o.getString("id"), ownerId = o.optString("owner_id"), title = o.optString("title"),
                description = o.optString("description"), category = o.optString("category"), osVersion = o.optString("os_version"),
                fileUrl = o.optString("file_url"), previewUrl = o.optString("preview_url"),
                previewUrls = parseStringList(o.optJSONArray("preview_urls")), driveUrl = o.optString("drive_url"),
                status = o.optString("status", "pending"),
                rejectReason = o.optString("reject_reason"), downloads = o.optLong("downloads"), rating = o.optDouble("rating"),
                createdAt = o.optString("created_at"), coinPrice = o.optInt("coin_price")
            )
        }
    }


    private fun parseStringList(a: JSONArray?): List<String> {
        if (a == null) return emptyList()
        return List(a.length()) { i -> a.optString(i) }.filter { it.isNotBlank() }
    }
    private fun isoAfter(seconds: Long): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(java.util.Date(System.currentTimeMillis() + seconds * 1000L))
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
