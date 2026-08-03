package com.example

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID
import okio.source

object SupabaseConfig {
    val url: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val configured: Boolean get() = url.startsWith("https://") && anonKey.length > 20
}

data class Session(val accessToken: String, val userId: String, val email: String)
data class Profile(val id: String, val username: String, val displayName: String, val role: String)
data class OnlineTheme(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val category: String,
    val preview: String,
    val fileUrl: String,
    val downloads: Long,
    val rating: Double,
    val approved: Boolean,
    val createdAt: String
)

class SupabaseClient(private val context: Context) {
    private val http = OkHttpClient()
    private val prefs = context.getSharedPreferences("m4x_supabase", Context.MODE_PRIVATE)

    fun restoreSession(): Session? {
        val token = prefs.getString("token", null) ?: return null
        val uid = prefs.getString("uid", null) ?: return null
        return Session(token, uid, prefs.getString("email", "") ?: "")
    }

    fun saveSession(session: Session) {
        prefs.edit().putString("token", session.accessToken).putString("uid", session.userId)
            .putString("email", session.email).apply()
    }

    fun signOut() = prefs.edit().clear().apply()

    suspend fun signUp(email: String, password: String, username: String, displayName: String): Result<Session> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", JSONObject().apply {
                    put("username", username.lowercase())
                    put("display_name", displayName)
                })
            }
            authRequest("/auth/v1/signup", body)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Session> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("email", email).put("password", password)
            authRequest("/auth/v1/token?grant_type=password", body)
        }
    }

    private fun authRequest(path: String, body: JSONObject): Session {
        requireConfigured()
        val req = Request.Builder().url(SupabaseConfig.url + path)
            .header("apikey", SupabaseConfig.anonKey)
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull())).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(errorMessage(text, "Đăng nhập thất bại (${res.code})"))
            val json = JSONObject(text)
            val token = json.optString("access_token")
            if (token.isBlank()) throw IOException("Hãy xác nhận email rồi đăng nhập lại")
            val user = json.getJSONObject("user")
            return Session(token, user.getString("id"), user.optString("email"))
        }
    }

    suspend fun getProfile(session: Session): Result<Profile> = withContext(Dispatchers.IO) {
        runCatching {
            val array = getJsonArray("/rest/v1/profiles?id=eq.${session.userId}&select=id,username,display_name,role", session)
            if (array.length() == 0) throw IOException("Chưa tạo được hồ sơ người dùng")
            val o = array.getJSONObject(0)
            Profile(o.getString("id"), o.optString("username"), o.optString("display_name"), o.optString("role", "user"))
        }
    }

    suspend fun getApprovedThemes(session: Session): Result<List<OnlineTheme>> = withContext(Dispatchers.IO) {
        runCatching { parseThemes(getJsonArray("/rest/v1/themes?approved=eq.true&select=*&order=created_at.desc", session)) }
    }

    suspend fun getPendingThemes(session: Session): Result<List<OnlineTheme>> = withContext(Dispatchers.IO) {
        runCatching { parseThemes(getJsonArray("/rest/v1/themes?approved=eq.false&select=*&order=created_at.desc", session)) }
    }

    suspend fun uploadTheme(
        session: Session,
        uri: Uri,
        name: String,
        description: String,
        category: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(name.isNotBlank()) { "Chưa nhập tên theme" }
            val original = queryName(context.contentResolver, uri)
            require(original.endsWith(".mtz", true) || original.endsWith(".zip", true)) { "Chỉ chấp nhận file .mtz hoặc .zip" }
            val safe = original.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val path = "${session.userId}/${UUID.randomUUID()}_$safe"
            uploadObject(session, uri, path)
            val publicUrl = "${SupabaseConfig.url}/storage/v1/object/public/themes/$path"
            val row = JSONObject().apply {
                put("name", name)
                put("author", session.userId)
                put("description", description)
                put("category", category)
                put("preview", "")
                put("file_url", publicUrl)
                put("downloads", 0)
                put("rating", 0)
                put("approved", false)
            }
            postJson("/rest/v1/themes", JSONArray().put(row).toString(), session)
        }
    }

    suspend fun approveTheme(session: Session, id: String, approved: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            patchJson("/rest/v1/themes?id=eq.$id", JSONObject().put("approved", approved).toString(), session)
        }
    }

    suspend fun deleteTheme(session: Session, id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val req = baseRequest(SupabaseConfig.url + "/rest/v1/themes?id=eq.$id", session).delete().build()
            executeNoBody(req)
        }
    }

    suspend fun listProfiles(session: Session): Result<List<Profile>> = withContext(Dispatchers.IO) {
        runCatching {
            val a = getJsonArray("/rest/v1/profiles?select=id,username,display_name,role&order=created_at.desc", session)
            List(a.length()) { i -> a.getJSONObject(i).let { Profile(it.getString("id"), it.optString("username"), it.optString("display_name"), it.optString("role")) } }
        }
    }

    suspend fun setRole(session: Session, userId: String, role: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(role in setOf("user", "creator", "admin", "banned"))
            val req = baseRequest(SupabaseConfig.url + "/rest/v1/rpc/set_user_role", session)
                .post(JSONObject().put("target_user_id", userId).put("new_role", role).toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            executeNoBody(req)
        }
    }

    private fun uploadObject(session: Session, uri: Uri, path: String) {
        requireConfigured()
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val body = object : RequestBody() {
            override fun contentType() = mime.toMediaTypeOrNull()
            override fun contentLength(): Long = querySize(resolver, uri)
            override fun writeTo(sink: okio.BufferedSink) {
                resolver.openInputStream(uri)?.use { input -> sink.writeAll(input.source()) }
                    ?: throw IOException("Không mở được file")
            }
        }
        val req = baseRequest("${SupabaseConfig.url}/storage/v1/object/themes/$path", session)
            .header("x-upsert", "false").post(body).build()
        executeNoBody(req)
    }

    private fun getJsonArray(path: String, session: Session): JSONArray {
        val req = baseRequest(SupabaseConfig.url + path, session).get().build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(errorMessage(text, "Không tải được dữ liệu (${res.code})"))
            return JSONArray(text)
        }
    }

    private fun postJson(path: String, json: String, session: Session) {
        val req = baseRequest(SupabaseConfig.url + path, session).header("Prefer", "return=minimal")
            .post(json.toRequestBody("application/json".toMediaTypeOrNull())).build()
        executeNoBody(req)
    }

    private fun patchJson(path: String, json: String, session: Session) {
        val req = baseRequest(SupabaseConfig.url + path, session).header("Prefer", "return=minimal")
            .patch(json.toRequestBody("application/json".toMediaTypeOrNull())).build()
        executeNoBody(req)
    }

    private fun executeNoBody(req: Request) {
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(errorMessage(text, "Yêu cầu thất bại (${res.code})"))
        }
    }

    private fun baseRequest(url: String, session: Session) = Request.Builder().url(url)
        .header("apikey", SupabaseConfig.anonKey).header("Authorization", "Bearer ${session.accessToken}")
        .header("Content-Type", "application/json")

    private fun parseThemes(a: JSONArray) = List(a.length()) { i ->
        val o = a.getJSONObject(i)
        OnlineTheme(o.getString("id"), o.optString("name"), o.optString("author"), o.optString("description"),
            o.optString("category"), o.optString("preview"), o.optString("file_url"), o.optLong("downloads"),
            o.optDouble("rating"), o.optBoolean("approved"), o.optString("created_at"))
    }

    private fun requireConfigured() {
        if (!SupabaseConfig.configured) throw IOException("Chưa cấu hình SUPABASE_URL và SUPABASE_ANON_KEY")
    }

    private fun errorMessage(text: String, fallback: String): String = runCatching {
        val j = JSONObject(text); j.optString("msg", j.optString("message", j.optString("error_description", fallback)))
    }.getOrDefault(fallback)

    companion object {
        fun queryName(resolver: ContentResolver, uri: Uri): String {
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) return c.getString(idx)
            }
            return uri.lastPathSegment ?: "theme.mtz"
        }
        fun querySize(resolver: ContentResolver, uri: Uri): Long {
            resolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.SIZE)
                if (idx >= 0 && c.moveToFirst() && !c.isNull(idx)) return c.getLong(idx)
            }
            return -1
        }
    }
}
