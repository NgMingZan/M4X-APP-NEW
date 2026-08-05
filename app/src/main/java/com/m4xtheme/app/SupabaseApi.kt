package com.m4xtheme.app

import com.m4xtheme.app.rust.RustThemeValidator
import com.m4xtheme.app.rust.ThemeValidationResult
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
data class Profile(
    val id: String,
    val username: String,
    val displayName: String,
    val role: String,
    val points: Long,
    val avatarUrl: String = ""
)
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
    val coinPrice: Int = 0,
    val clientValidationStatus: String = "unchecked",
    val clientValidationMessage: String = "",
    val clientFileSha256: String = "",
    val clientFileSizeBytes: Long = 0L,
    val clientValidationAt: String = "",
    val clientSafetyScore: Int = 0,
    val clientSafetyLevel: String = "danger",
    val clientThemeMetadata: String = "{}",
    val clientModuleReport: String = "[]",
    val clientValidationReport: String = "{}",
    val approvedFileSha256: String = "",
    val approvedFileSizeBytes: Long = 0L
)

data class ThemePurchaseResult(
    val themeId: String,
    val balance: Long,
    val alreadyOwned: Boolean
)


data class ThemeReviewChecklist(
    val previewOk: Boolean = false,
    val downloadOk: Boolean = false,
    val compatibilityOk: Boolean = false,
    val safeContent: Boolean = false,
    val notDuplicate: Boolean = false
) {
    val completed: Boolean
        get() = previewOk && downloadOk && compatibilityOk && safeContent && notDuplicate

    fun toJson(): JSONObject = JSONObject()
        .put("preview_ok", previewOk)
        .put("download_ok", downloadOk)
        .put("compatibility_ok", compatibilityOk)
        .put("safe_content", safeContent)
        .put("not_duplicate", notDuplicate)

    companion object {
        fun fromJson(raw: String): ThemeReviewChecklist = runCatching {
            val o = JSONObject(raw.ifBlank { "{}" })
            ThemeReviewChecklist(
                previewOk = o.optBoolean("preview_ok"),
                downloadOk = o.optBoolean("download_ok"),
                compatibilityOk = o.optBoolean("compatibility_ok"),
                safeContent = o.optBoolean("safe_content"),
                notDuplicate = o.optBoolean("not_duplicate")
            )
        }.getOrDefault(ThemeReviewChecklist())
    }
}

data class ThemeReviewHistory(
    val id: String,
    val themeId: String,
    val themeTitle: String,
    val ownerId: String,
    val ownerName: String,
    val reviewerId: String,
    val reviewerName: String,
    val reviewerRole: String,
    val decision: String,
    val reason: String,
    val checklist: ThemeReviewChecklist,
    val createdAt: String
)

data class CreatorReputation(
    val userId: String,
    val score: Int,
    val approvedCount: Int,
    val rejectedCount: Int,
    val revokedCount: Int,
    val totalReviews: Int
) {
    val level: String
        get() = when {
            score >= 90 -> "Chuyên gia"
            score >= 75 -> "Đáng tin cậy"
            score >= 60 -> "Ổn định"
            score >= 40 -> "Đang phát triển"
            else -> "Cần cải thiện"
        }
}

data class ThemeReviewNotification(
    val id: String,
    val themeId: String,
    val type: String,
    val title: String,
    val message: String,
    val reason: String,
    val readAt: String,
    val createdAt: String
)

data class EventItem(val id: String, val title: String, val description: String, val startAt: String, val endAt: String, val active: Boolean)
data class QuestItem(val id: String, val title: String, val description: String, val reward: Int)
data class LeaderboardItem(
    val rank: Int,
    val displayName: String,
    val score: Long,
    val approvedThemes: Int = 0,
    val downloadsReceived: Int = 0,
    val activeMinutes: Int = 0
)
data class ChestResult(val reward: Int, val balance: Long, val message: String)
data class InventoryItem(
    val id: String,
    val itemId: String,
    val name: String,
    val type: String,
    val equipped: Boolean = false,
    val metadata: String = "{}",
    val imageUrl: String = ""
)
data class ShopItem(
    val id: String,
    val name: String,
    val type: String,
    val price: Int,
    val imageUrl: String,
    val limited: Boolean
)
data class MiniGameResult(
    val reward: Int,
    val balance: Long,
    val message: String,
    val remaining: Int
)

data class ArenaBotStartResult(
    val sessionId: String,
    val minSeconds: Int,
    val maxSeconds: Int,
    val startedAt: String
)

data class ArenaBotClaimResult(
    val reward: Int,
    val balance: Long,
    val message: String,
    val rank: Int,
    val kills: Int,
    val deaths: Int
)


data class FishingMapInfo(
    val code: String,
    val name: String,
    val subtitle: String,
    val unlockCost: Int,
    val difficulty: Int,
    val unlocked: Boolean,
    val fishNames: List<String>,
    val theme: String
)

data class FishingRodInfo(
    val code: String,
    val name: String,
    val price: Int,
    val power: Int,
    val stability: Int,
    val luck: Int,
    val owned: Boolean,
    val equipped: Boolean,
    val description: String
)

data class FishingCatchInfo(
    val id: String,
    val fishCode: String,
    val fishName: String,
    val mapName: String,
    val rarity: String,
    val weightGrams: Int,
    val sellValue: Int,
    val caughtAt: String
)

data class FishingGameState(
    val balance: Long,
    val maps: List<FishingMapInfo>,
    val rods: List<FishingRodInfo>,
    val catches: List<FishingCatchInfo>,
    val equippedRodCode: String,
    val inventoryValue: Int
)

data class FishingCastStart(
    val castId: String,
    val mapCode: String,
    val mapName: String,
    val fishCode: String,
    val fishName: String,
    val rarity: String,
    val fishDifficulty: Int,
    val biteDelayMs: Int,
    val minReelMs: Int,
    val maxReelMs: Int,
    val rodPower: Int,
    val rodStability: Int,
    val rodLuck: Int
)

data class FishingCastFinish(
    val caught: Boolean,
    val catchId: String,
    val fishName: String,
    val rarity: String,
    val weightGrams: Int,
    val sellValue: Int,
    val message: String
)

data class FishingCoinAction(
    val balance: Long,
    val amount: Int,
    val message: String
)

data class ArcadeRewardResult(
    val reward: Int,
    val balance: Long,
    val dailyTotal: Int,
    val message: String
)

data class MazeStartResult(
    val sessionId: String,
    val difficulty: String,
    val seed: Long,
    val fee: Int,
    val balance: Long,
    val minSeconds: Int
)

data class MazeFinishResult(
    val reward: Int,
    val balance: Long,
    val message: String
)

data class TreasureLeader(
    val rank: Int,
    val displayName: String,
    val openedCount: Int
)

data class TreasureState(
    val weekStart: String,
    val day: Int,
    val openedDays: List<Int>,
    val keys: Int,
    val secretDay: Int,
    val secretClaimed: Boolean,
    val bronzeClaimed: Boolean,
    val silverClaimed: Boolean,
    val goldClaimed: Boolean,
    val streakWeeks: Int,
    val rescueUsed: Boolean,
    val bossEnergy: Int,
    val shareCode: String,
    val taskCode: String,
    val taskTitle: String,
    val taskProgress: Int,
    val taskTarget: Int,
    val bossName: String,
    val bossHp: Int,
    val bossMaxHp: Int,
    val seasonName: String,
    val seasonRewardItem: String,
    val leaders: List<TreasureLeader>
)

data class TreasureActionResult(
    val reward: Int = 0,
    val bonus: Int = 0,
    val streakBonus: Int = 0,
    val balance: Long = 0L,
    val openedDay: Int = 0,
    val rescuedDay: Int = 0,
    val damage: Int = 0,
    val bossHp: Int = 0,
    val bossMaxHp: Int = 0,
    val defeated: Boolean = false,
    val rareItem: Boolean = false,
    val secret: Boolean = false,
    val shareCode: String = ""
)

data class PetState(
    val name: String,
    val type: String,
    val level: Int,
    val xp: Int,
    val xpTarget: Int,
    val hunger: Int,
    val food: Int,
    val levelReward: Int,
    val balance: Long
)

data class RemoteConfig(
    val minVersionCode: Int = 0,
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val updateUrl: String = "",
    val updateMessage: String = "",
    val forceUpdate: Boolean = false,
    val homeBannerTitle: String = "M4X Theme",
    val homeBannerSubtitle: String = "Kho giao diện HyperOS & MIUI",
    val webFootballUrl: String = "https://xoilacxtl.tv/",
    val webMovieUrl: String = "https://cobephim.pro/",
    val webAdultUrl: String = "https://vnsextop1.com/",
    val webFootballEnabled: Boolean = true,
    val webMovieEnabled: Boolean = true,
    val webAdultEnabled: Boolean = true
)

data class OnlineSpinReward(
    val reward: Int,
    val weight: Int
)

data class OnlineControlConfig(
    val bannerEnabled: Boolean = true,
    val bannerTitle: String = "M4X Theme",
    val bannerSubtitle: String = "Kho giao diện HyperOS & MIUI",
    val noticeEnabled: Boolean = false,
    val noticeTitle: String = "Thông báo",
    val noticeMessage: String = "",
    val dailyQuestEnabled: Boolean = true,
    val dailyQuestTitle: String = "Điểm danh nhiệm vụ online",
    val dailyQuestDescription: String = "Mở ứng dụng và nhận quà hôm nay",
    val dailyQuestReward: Int = 100,
    val checkinEnabled: Boolean = true,
    val checkinRewards: List<Int> = listOf(
        50, 75, 100, 125, 150, 200, 300
    ),
    val spinEnabled: Boolean = true,
    val spinCost: Int = 25,
    val spinRewards: List<OnlineSpinReward> = listOf(
        OnlineSpinReward(0, 20),
        OnlineSpinReward(10, 45),
        OnlineSpinReward(50, 20),
        OnlineSpinReward(100, 10),
        OnlineSpinReward(250, 5)
    ),
    val fishingEnabled: Boolean = true,
    val fishingClosedMessage: String = "M4X Fishing đang bảo trì",
    val fishingRewardMultiplier: Double = 1.0,
    val fishingBossHpMultiplier: Double = 1.0,
    val featuredThemeId: String = ""
)

data class OnlineCoinResult(
    val reward: Int,
    val balance: Long,
    val message: String,
    val streak: Int = 0,
    val cost: Int = 0
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
        val a = get("/rest/v1/profiles?id=eq.${session.userId}&select=id,username,display_name,role,points,avatar_url", session)
        if (a.length() == 0) throw IOException("Không tìm thấy hồ sơ. Hãy kiểm tra trigger profiles trong Supabase")
        val o = a.getJSONObject(0)
        Profile(o.getString("id"), o.optString("username"), o.optString("display_name"), o.optString("role", "user"), o.optLong("points"), o.optString("avatar_url"))
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

    suspend fun allThemes(session: Session): Result<List<ThemeItem>> = io {
        parseThemes(get("/rest/v1/themes?select=*&order=created_at.desc", session))
    }

    suspend fun users(session: Session): Result<List<Profile>> = io {
        val a = get("/rest/v1/profiles?select=id,username,display_name,role,points,avatar_url&order=created_at.desc", session)
        List(a.length()) { i -> a.getJSONObject(i).let { o ->
            Profile(o.getString("id"), o.optString("username"), o.optString("display_name"), o.optString("role", "user"), o.optLong("points"), o.optString("avatar_url"))
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

        var clientValidation: ThemeValidationResult? = null
        var publicUrl = ""
        if (fileUri != null) {
            val name = fileName(context.contentResolver, fileUri)
            require(name.endsWith(".mtz", true) || name.endsWith(".zip", true)) { "Chỉ nhận file .mtz hoặc .zip" }
            val validation = RustThemeValidator.validate(
                context = context,
                uri = fileUri,
                maxSizeBytes = RustThemeValidator.DEFAULT_MAX_SIZE_BYTES
            ).getOrThrow()
            require(validation.valid) {
                validation.errors.firstOrNull() ?: validation.message
            }
            clientValidation = validation
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
            clientValidation?.let { validation ->
                put("client_validation_status", validation.status)
                put("client_validation_message", validation.adminSummary)
                put("client_file_sha256", validation.sha256)
                put("client_file_size_bytes", validation.sizeBytes)
                put("client_validation_at", isoAfter(0))
                put("client_safety_score", validation.safetyScore)
                put("client_safety_level", validation.safetyLevel)
                put("client_theme_metadata", validation.metadata.toJson())
                put("client_module_report", validation.modulesJson())
                put("client_validation_report", JSONObject(validation.rawJson))
            }
        }
        post("/rest/v1/themes", JSONArray().put(row).toString(), session)
    }

    suspend fun reviewTheme(
        session: Session,
        id: String,
        approved: Boolean,
        reason: String = "",
        checklist: ThemeReviewChecklist = ThemeReviewChecklist()
    ): Result<Unit> = io {
        if (approved) require(checklist.completed) { "Hãy hoàn thành đủ checklist trước khi duyệt" }
        if (!approved) require(reason.trim().length >= 5) { "Hãy nhập lý do từ chối rõ ràng" }
        val body = JSONObject()
            .put("target_theme_id", id)
            .put("approve_theme", approved)
            .put("review_reason", if (approved) "" else reason.trim())
            .put("review_checklist", checklist.toJson())
        execute(
            base("${SupabaseConfig.url}/rest/v1/rpc/review_theme", session)
                .post(body.toString().toRequestBody(jsonType))
                .build()
        )
    }

    suspend fun themeReviewHistory(session: Session): Result<List<ThemeReviewHistory>> = io {
        parseThemeReviewHistory(
            get("/rest/v1/theme_reviews?select=*&order=created_at.desc&limit=250", session)
        )
    }

    suspend fun creatorReputation(
        session: Session,
        userId: String = session.userId
    ): Result<CreatorReputation?> = io {
        val rows = get(
            "/rest/v1/creator_reputation?user_id=eq.$userId&select=*&limit=1",
            session
        )
        if (rows.length() == 0) null else parseCreatorReputation(rows.getJSONObject(0))
    }

    suspend fun creatorReputations(session: Session): Result<List<CreatorReputation>> = io {
        val rows = get(
            "/rest/v1/creator_reputation?select=*&order=score.desc,total_reviews.desc",
            session
        )
        List(rows.length()) { index -> parseCreatorReputation(rows.getJSONObject(index)) }
    }

    suspend fun themeReviewNotifications(
        session: Session
    ): Result<List<ThemeReviewNotification>> = io {
        parseThemeReviewNotifications(
            get(
                "/rest/v1/theme_review_notifications?user_id=eq.${session.userId}&select=*&order=created_at.desc&limit=100",
                session
            )
        )
    }

    suspend fun markAllThemeNotificationsRead(session: Session): Result<Unit> = io {
        val body = JSONObject().put("read_at", isoAfter(0))
        patch(
            "/rest/v1/theme_review_notifications?user_id=eq.${session.userId}&read_at=is.null",
            body.toString(),
            session
        )
    }

    suspend fun revokeThemeApproval(
        session: Session,
        themeId: String,
        reason: String
    ): Result<Unit> = io {
        require(reason.trim().length >= 5) { "Hãy nhập lý do thu hồi" }
        val body = JSONObject()
            .put("target_theme_id", themeId)
            .put("revoke_reason", reason.trim())
        execute(
            base("${SupabaseConfig.url}/rest/v1/rpc/revoke_theme_approval", session)
                .post(body.toString().toRequestBody(jsonType))
                .build()
        )
    }

    suspend fun resubmitTheme(
        session: Session,
        themeId: String,
        title: String,
        description: String,
        driveUrl: String,
        coinPrice: Int
    ): Result<Unit> = io {
        require(title.isNotBlank()) { "Tên theme không được để trống" }
        if (driveUrl.isNotBlank()) require(driveUrl.startsWith("https://")) {
            "Link tải phải bắt đầu bằng https://"
        }
        val body = JSONObject()
            .put("target_theme_id", themeId)
            .put("new_title", title.trim())
            .put("new_description", description.trim())
            .put("new_drive_url", driveUrl.trim())
            .put("new_coin_price", coinPrice.coerceAtLeast(0))
        execute(
            base("${SupabaseConfig.url}/rest/v1/rpc/resubmit_theme", session)
                .post(body.toString().toRequestBody(jsonType))
                .build()
        )
    }

    suspend fun updateThemeByAdmin(
        session: Session,
        id: String,
        title: String,
        description: String,
        driveUrl: String,
        coinPrice: Int,
        status: String,
        previewUris: List<Uri> = emptyList()
    ): Result<Unit> = io {
        require(title.isNotBlank()) { "Tên theme không được để trống" }
        require(status in setOf("pending", "approved", "rejected")) { "Trạng thái không hợp lệ" }
        val uploadedPreviews = previewUris.take(5).mapIndexed { index, uri ->
            val original = fileName(context.contentResolver, uri)
            val ext = original.substringAfterLast('.', "jpg").lowercase().take(5)
            val path = "${session.userId}/admin-previews/${UUID.randomUUID()}_${index}.$ext"
            uploadFile(session, uri, path)
            "${SupabaseConfig.url}/storage/v1/object/public/themes/$path"
        }

        val json = JSONObject()
            .put("title", title.trim())
            .put("description", description.trim())
            .put("drive_url", driveUrl.trim())
            .put("coin_price", coinPrice.coerceAtLeast(0))
            .put("status", status)
            .put("updated_at", isoAfter(0))
        if (uploadedPreviews.isNotEmpty()) {
            json.put("preview_url", uploadedPreviews.first())
            json.put("preview_urls", JSONArray(uploadedPreviews))
        }
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

    suspend fun purchaseThemeV2(
        session: Session,
        themeId: String
    ): Result<ThemePurchaseResult> = io {
        val body = JSONObject().put("target_theme_id", themeId)
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/purchase_theme_v2",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(
                        text,
                        "Không thể mua theme (${response.code})"
                    )
                )
            }
            val o = rpcObject(text)
            ThemePurchaseResult(
                themeId = o.optString("theme_id", themeId),
                balance = o.optLong("balance"),
                alreadyOwned = o.optBoolean("already_owned")
            )
        }
    }

    suspend fun ownedThemeIds(
        session: Session
    ): Result<Set<String>> = io {
        val body = "{}".toRequestBody(jsonType)
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/get_owned_theme_ids",
            session
        ).post(body).build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(
                        text,
                        "Không tải được quyền sở hữu theme (${response.code})"
                    )
                )
            }
            val rows = JSONArray(text)
            val ids = mutableSetOf<String>()
            repeat(rows.length()) {
                val id = rows.getJSONObject(it).optString("theme_id")
                if (id.isNotBlank()) ids += id
            }
            ids
        }
    }

    suspend fun ownedThemes(
        session: Session
    ): Result<List<ThemeItem>> = io {
        val body = "{}".toRequestBody(jsonType)
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/get_owned_themes",
            session
        ).post(body).build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(
                        text,
                        "Không tải được thư viện theme (${response.code})"
                    )
                )
            }
            parseThemes(JSONArray(text))
        }
    }

    suspend fun creatorUpdateTheme(
        session: Session,
        themeId: String,
        title: String,
        description: String,
        category: String,
        osVersion: String,
        driveUrl: String,
        coinPrice: Int
    ): Result<Unit> = io {
        require(title.isNotBlank()) {
            "Tên theme không được để trống"
        }
        if (driveUrl.isNotBlank()) {
            require(driveUrl.startsWith("https://")) {
                "Link tải phải bắt đầu bằng https://"
            }
        }

        val body = JSONObject()
            .put("target_theme_id", themeId)
            .put("new_title", title.trim())
            .put("new_description", description.trim())
            .put("new_category", category.trim())
            .put("new_os_version", osVersion.trim())
            .put("new_drive_url", driveUrl.trim())
            .put("new_coin_price", coinPrice.coerceAtLeast(0))

        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/creator_update_theme",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(
                        text,
                        "Không thể sửa theme (${response.code})"
                    )
                )
            }
            rpcObject(text)
            Unit
        }
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

    suspend fun claimedQuestIds(session: Session): Result<Set<String>> = io {
        val a = get("/rest/v1/quest_claims?user_id=eq.${session.userId}&select=quest_id", session)
        buildSet { repeat(a.length()) { add(a.getJSONObject(it).optString("quest_id")) } }.filter { it.isNotBlank() }.toSet()
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

    suspend fun createGiftCode(
        session: Session,
        code: String,
        reward: Int,
        maxUses: Int,
        validHours: Int
    ): Result<Unit> = io {
        require(code.isNotBlank()) { "Chưa nhập mã Giftcode" }
        require(reward > 0) { "M4X COIN phải lớn hơn 0" }
        require(maxUses in 1..1_000_000) { "Số lượt nhập phải từ 1 đến 1.000.000" }
        require(validHours in 1..8760) { "Thời hạn phải từ 1 giờ đến 365 ngày" }
        val row = JSONObject()
            .put("code", code.trim().uppercase())
            .put("reward", reward)
            .put("max_uses", maxUses)
            .put("used_count", 0)
            .put("expires_at", isoAfter(validHours.toLong() * 3600L))
            .put("active", true)
        post("/rest/v1/giftcodes", JSONArray().put(row).toString(), session)
    }

    suspend fun weeklyLeaderboard(session: Session): Result<List<LeaderboardItem>> = io {
        val a = get("/rest/v1/weekly_leaderboard?select=rank,display_name,score,approved_themes,downloads_received,active_minutes&order=rank.asc&limit=20", session)
        List(a.length()) { i -> a.getJSONObject(i).let {
            LeaderboardItem(
                rank = it.optInt("rank", i + 1),
                displayName = it.optString("display_name", "M4X Member"),
                score = it.optLong("score"),
                approvedThemes = it.optInt("approved_themes"),
                downloadsReceived = it.optInt("downloads_received"),
                activeMinutes = it.optInt("active_minutes")
            )
        } }
    }

    suspend fun recordAppUsage(session: Session, minutes: Int = 1): Result<Unit> = io {
        val body = JSONObject().put("p_minutes", minutes.coerceIn(1, 5))
        execute(base("${SupabaseConfig.url}/rest/v1/rpc/record_app_usage", session)
            .post(body.toString().toRequestBody(jsonType)).build())
    }

    suspend fun openCoinChest(session: Session): Result<ChestResult> = io {
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/open_coin_chest", session)
            .post("{}".toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể mở rương"))
            val o = if (text.trim().startsWith("[")) JSONArray(text).getJSONObject(0) else JSONObject(text)
            ChestResult(o.optInt("reward"), o.optLong("balance"), o.optString("message"))
        }
    }

    suspend fun inventory(session: Session): Result<List<InventoryItem>> = io {
        val a = get(
            "/rest/v1/user_inventory?user_id=eq.${session.userId}" +
                "&select=id,item_id,item_name,item_type,equipped,item_metadata,item_image_url" +
                "&order=equipped.desc,acquired_at.desc",
            session
        )
        List(a.length()) { i -> a.getJSONObject(i).let {
            InventoryItem(
                id = it.getString("id"),
                itemId = it.optString("item_id"),
                name = it.optString("item_name"),
                type = it.optString("item_type"),
                equipped = it.optBoolean("equipped"),
                metadata = it.optJSONObject("item_metadata")?.toString() ?: "{}",
                imageUrl = it.optString("item_image_url")
            )
        } }
    }

    suspend fun equipInventoryItem(session: Session, inventoryId: String): Result<Boolean> = io {
        val body = JSONObject().put("p_inventory_id", inventoryId)
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/equip_inventory_item", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể sử dụng vật phẩm"))
            val o = if (text.trim().startsWith("[")) JSONArray(text).getJSONObject(0) else JSONObject(text)
            o.optBoolean("equipped")
        }
    }

    suspend fun shopItems(session: Session): Result<List<ShopItem>> = io {
        val a = get("/rest/v1/shop_items?active=eq.true&select=id,name,item_type,price,image_url,limited&order=price.asc", session)
        List(a.length()) { i -> a.getJSONObject(i).let {
            ShopItem(
                id = it.getString("id"),
                name = it.optString("name"),
                type = it.optString("item_type"),
                price = it.optInt("price"),
                imageUrl = it.optString("image_url"),
                limited = it.optBoolean("limited")
            )
        } }
    }

    suspend fun purchaseShopItem(session: Session, itemId: String): Result<Long> = io {
        val body = JSONObject().put("p_item_id", itemId)
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/purchase_shop_item", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể mua vật phẩm"))
            text.trim().trim('"').toLongOrNull() ?: throw IOException("Không đọc được số dư mới")
        }
    }


    // Arena Online RPCs kept together with the newer Fishing RPCs.
    suspend fun joinArenaMatch(
        session: Session,
        displayName: String
    ): Result<ArenaMatchTicket> = io {
        val body = JSONObject()
            .put(
                "p_display_name",
                displayName.trim().ifBlank { "M4X Hunter" }.take(28)
            )
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/arena_join_match",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(error(raw, "Không thể tìm trận Arena"))
            }
            parseArenaTicket(rpcObject(raw))
        }
    }

    suspend fun arenaMatchStatus(
        session: Session,
        matchId: String
    ): Result<ArenaMatchTicket> = io {
        val body = JSONObject().put("p_match_id", matchId)
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/arena_match_status",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(error(raw, "Không đọc được trạng thái trận"))
            }
            parseArenaTicket(rpcObject(raw))
        }
    }

    suspend fun leaveArenaMatch(
        session: Session,
        matchId: String
    ): Result<Unit> = io {
        val body = JSONObject().put("p_match_id", matchId)
        execute(
            base(
                "${SupabaseConfig.url}/rest/v1/rpc/arena_leave_match",
                session
            ).post(body.toString().toRequestBody(jsonType)).build()
        )
    }

    suspend fun finishArenaMatch(
        session: Session,
        matchId: String,
        durationSeconds: Int,
        results: JSONArray
    ): Result<Unit> = io {
        val body = JSONObject()
            .put("p_match_id", matchId)
            .put("p_duration_seconds", durationSeconds.coerceIn(0, 600))
            .put("p_results", results)

        execute(
            base(
                "${SupabaseConfig.url}/rest/v1/rpc/arena_finish_match",
                session
            ).post(body.toString().toRequestBody(jsonType)).build()
        )
    }

    suspend fun claimArenaReward(
        session: Session,
        matchId: String
    ): Result<ArenaRewardClaim> = io {
        val body = JSONObject().put("p_match_id", matchId)
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/arena_claim_reward",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(error(raw, "Chưa thể nhận thưởng Arena"))
            }
            val json = rpcObject(raw)
            ArenaRewardClaim(
                reward = json.optInt("reward"),
                balance = json.optLong("balance"),
                message = json.optString("message", "Đã nhận thưởng Arena")
            )
        }
    }

    suspend fun startArenaBotMatch(
        session: Session
    ): Result<ArenaBotStartResult> = io {
        val req = base(
            "${SupabaseConfig.url}/rest/v1/rpc/arena_bot_start",
            session
        ).post("{}".toRequestBody(jsonType)).build()

        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể bắt đầu trận Arena")
                )
            }
            val o = rpcObject(raw)
            ArenaBotStartResult(
                sessionId = o.optString("session_id"),
                minSeconds = o.optInt("min_seconds", 45),
                maxSeconds = o.optInt("max_seconds", 360),
                startedAt = o.optString("started_at")
            )
        }
    }

    suspend fun claimArenaBotReward(
        session: Session,
        arenaSessionId: String,
        durationSeconds: Int,
        rank: Int,
        kills: Int,
        deaths: Int
    ): Result<ArenaBotClaimResult> = io {
        val body = JSONObject()
            .put("p_session_id", arenaSessionId)
            .put("p_duration_seconds", durationSeconds.coerceIn(0, 600))
            .put("p_rank", rank.coerceIn(1, 10))
            .put("p_kills", kills.coerceIn(0, 100))
            .put("p_deaths", deaths.coerceIn(0, 100))

        val req = base(
            "${SupabaseConfig.url}/rest/v1/rpc/arena_bot_claim",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể nhận thưởng Arena")
                )
            }
            val o = rpcObject(raw)
            ArenaBotClaimResult(
                reward = o.optInt("reward"),
                balance = o.optLong("balance"),
                message = o.optString(
                    "message",
                    "Đã nhận thưởng Arena"
                ),
                rank = o.optInt("rank", rank),
                kills = o.optInt("kills", kills),
                deaths = o.optInt("deaths", deaths)
            )
        }
    }

    suspend fun fishingState(
        session: Session
    ): Result<FishingGameState> = io {
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/fishing_get_state",
            session
        ).post("{}".toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(raw, "Không tải được dữ liệu câu cá")
                )
            }

            val json = rpcObject(raw)
            val mapsJson = json.optJSONArray("maps") ?: JSONArray()
            val rodsJson = json.optJSONArray("rods") ?: JSONArray()
            val catchesJson = json.optJSONArray("catches") ?: JSONArray()

            val maps = List(mapsJson.length()) { index ->
                val item = mapsJson.optJSONObject(index) ?: JSONObject()
                val fishJson = item.optJSONArray("fish") ?: JSONArray()
                FishingMapInfo(
                    code = item.optString("code"),
                    name = item.optString("name"),
                    subtitle = item.optString("subtitle"),
                    unlockCost = item.optInt("unlock_cost"),
                    difficulty = item.optInt("difficulty", 1),
                    unlocked = item.optBoolean("unlocked"),
                    fishNames = List(fishJson.length()) { fishIndex ->
                        fishJson.optString(fishIndex)
                    }.filter { it.isNotBlank() },
                    theme = item.optString("theme", "lotus")
                )
            }

            val rods = List(rodsJson.length()) { index ->
                val item = rodsJson.optJSONObject(index) ?: JSONObject()
                FishingRodInfo(
                    code = item.optString("code"),
                    name = item.optString("name"),
                    price = item.optInt("price"),
                    power = item.optInt("power", 1),
                    stability = item.optInt("stability", 1),
                    luck = item.optInt("luck", 1),
                    owned = item.optBoolean("owned"),
                    equipped = item.optBoolean("equipped"),
                    description = item.optString("description")
                )
            }

            val catches = List(catchesJson.length()) { index ->
                val item = catchesJson.optJSONObject(index) ?: JSONObject()
                FishingCatchInfo(
                    id = item.optString("id"),
                    fishCode = item.optString("fish_code"),
                    fishName = item.optString("fish_name"),
                    mapName = item.optString("map_name"),
                    rarity = item.optString("rarity", "common"),
                    weightGrams = item.optInt("weight_grams"),
                    sellValue = item.optInt("sell_value"),
                    caughtAt = item.optString("caught_at")
                )
            }

            FishingGameState(
                balance = json.optLong("balance"),
                maps = maps,
                rods = rods,
                catches = catches,
                equippedRodCode = json.optString("equipped_rod"),
                inventoryValue = json.optInt("inventory_value")
            )
        }
    }

    suspend fun unlockFishingMap(
        session: Session,
        mapCode: String
    ): Result<FishingCoinAction> = fishingCoinAction(
        session = session,
        rpc = "fishing_unlock_map",
        body = JSONObject().put("p_map_code", mapCode),
        fallback = "Không thể mở khóa khu câu"
    )

    suspend fun buyFishingRod(
        session: Session,
        rodCode: String
    ): Result<FishingCoinAction> = fishingCoinAction(
        session = session,
        rpc = "fishing_buy_rod",
        body = JSONObject().put("p_rod_code", rodCode),
        fallback = "Không thể mua cần câu"
    )

    suspend fun equipFishingRod(
        session: Session,
        rodCode: String
    ): Result<FishingCoinAction> = fishingCoinAction(
        session = session,
        rpc = "fishing_equip_rod",
        body = JSONObject().put("p_rod_code", rodCode),
        fallback = "Không thể trang bị cần câu"
    )

    suspend fun startFishingCast(
        session: Session,
        mapCode: String
    ): Result<FishingCastStart> = io {
        val body = JSONObject().put("p_map_code", mapCode)
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/fishing_start_cast",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể thả câu")
                )
            }

            val json = rpcObject(raw)
            FishingCastStart(
                castId = json.optString("cast_id"),
                mapCode = json.optString("map_code"),
                mapName = json.optString("map_name"),
                fishCode = json.optString("fish_code"),
                fishName = json.optString("fish_name"),
                rarity = json.optString("rarity", "common"),
                fishDifficulty = json.optInt("fish_difficulty", 1),
                biteDelayMs = json.optInt("bite_delay_ms", 2500),
                minReelMs = json.optInt("min_reel_ms", 3500),
                maxReelMs = json.optInt("max_reel_ms", 22000),
                rodPower = json.optInt("rod_power", 1),
                rodStability = json.optInt("rod_stability", 1),
                rodLuck = json.optInt("rod_luck", 1)
            )
        }
    }

    suspend fun finishFishingCast(
        session: Session,
        castId: String,
        success: Boolean,
        reelDurationMs: Int,
        reelQuality: Int
    ): Result<FishingCastFinish> = io {
        val body = JSONObject()
            .put("p_cast_id", castId)
            .put("p_success", success)
            .put(
                "p_reel_duration_ms",
                reelDurationMs.coerceIn(0, 120_000)
            )
            .put("p_reel_quality", reelQuality.coerceIn(0, 100))

        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/fishing_finish_cast",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể xác nhận lượt câu")
                )
            }

            val json = rpcObject(raw)
            FishingCastFinish(
                caught = json.optBoolean("caught"),
                catchId = json.optString("catch_id"),
                fishName = json.optString("fish_name"),
                rarity = json.optString("rarity", "common"),
                weightGrams = json.optInt("weight_grams"),
                sellValue = json.optInt("sell_value"),
                message = json.optString("message")
            )
        }
    }

    suspend fun sellFishingCatch(
        session: Session,
        catchId: String
    ): Result<FishingCoinAction> = fishingCoinAction(
        session = session,
        rpc = "fishing_sell_catch",
        body = JSONObject().put("p_catch_id", catchId),
        fallback = "Không thể bán cá"
    )

    suspend fun sellAllFishingCatches(
        session: Session
    ): Result<FishingCoinAction> = fishingCoinAction(
        session = session,
        rpc = "fishing_sell_all",
        body = JSONObject(),
        fallback = "Không thể bán cá trong kho"
    )

    private suspend fun fishingCoinAction(
        session: Session,
        rpc: String,
        body: JSONObject,
        fallback: String
    ): Result<FishingCoinAction> = io {
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/$rpc",
            session
        ).post(body.toString().toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(error(raw, fallback))
            }
            val json = rpcObject(raw)
            FishingCoinAction(
                balance = json.optLong("balance"),
                amount = json.optInt("amount"),
                message = json.optString("message", "Đã cập nhật")
            )
        }
    }

    suspend fun playMiniGame(session: Session, gameCode: String, choice: Int): Result<MiniGameResult> = io {
        val body = JSONObject().put("p_game_code", gameCode).put("p_choice", choice)
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/play_m4x_minigame", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể chơi minigame"))
            val o = if (text.trim().startsWith("[")) JSONArray(text).getJSONObject(0) else JSONObject(text)
            MiniGameResult(
                reward = o.optInt("reward"),
                balance = o.optLong("balance"),
                message = o.optString("message"),
                remaining = o.optInt("remaining")
            )
        }
    }

    suspend fun recordThemeView(session: Session, themeId: String): Result<Unit> = io {
        val body = JSONObject().put("p_theme_id", themeId)
        execute(base("${SupabaseConfig.url}/rest/v1/rpc/record_theme_view", session)
            .post(body.toString().toRequestBody(jsonType)).build())
    }

    suspend fun claimObstacleReward(session: Session, score: Int): Result<ArcadeRewardResult> = io {
        val body = JSONObject().put("p_score", score.coerceAtLeast(0))
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/claim_obstacle_reward", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể nhận thưởng Né chướng ngại"))
            val o = rpcObject(text)
            ArcadeRewardResult(o.optInt("reward"), o.optLong("balance"), o.optInt("daily_total"), o.optString("message"))
        }
    }

    suspend fun startMazeGame(session: Session, difficulty: String): Result<MazeStartResult> = io {
        val body = JSONObject().put("p_difficulty", difficulty)
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/start_maze_game", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể bắt đầu mê cung"))
            val o = rpcObject(text)
            MazeStartResult(
                sessionId = o.optString("session_id"),
                difficulty = o.optString("difficulty"),
                seed = o.optLong("seed"),
                fee = o.optInt("fee", 50),
                balance = o.optLong("balance"),
                minSeconds = o.optInt("min_seconds")
            )
        }
    }

    suspend fun finishMazeGame(session: Session, sessionId: String): Result<MazeFinishResult> = io {
        val body = JSONObject().put("p_session_id", sessionId)
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/finish_maze_game", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể nhận thưởng mê cung"))
            val o = rpcObject(text)
            MazeFinishResult(o.optInt("reward"), o.optLong("balance"), o.optString("message"))
        }
    }

    suspend fun treasureState(session: Session): Result<TreasureState> = io {
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/get_treasure_state", session)
            .post("{}".toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không tải được Bản đồ kho báu"))
            val o = rpcObject(text)
            val opened = o.optJSONArray("opened_days")?.let { a ->
                List(a.length()) { i -> a.optInt(i) }.filter { it in 1..7 }
            }.orEmpty()
            val season = o.optJSONObject("season") ?: JSONObject()
            val leadersJson = o.optJSONArray("leaders") ?: JSONArray()
            val leaders = List(leadersJson.length()) { i -> leadersJson.getJSONObject(i).let { l ->
                TreasureLeader(l.optInt("rank", i + 1), l.optString("display_name", "M4X Member"), l.optInt("opened_count"))
            } }
            TreasureState(
                weekStart = o.optString("week_start"),
                day = o.optInt("day", 1),
                openedDays = opened,
                keys = o.optInt("keys"),
                secretDay = o.optInt("secret_day"),
                secretClaimed = o.optBoolean("secret_claimed"),
                bronzeClaimed = o.optBoolean("bronze_claimed"),
                silverClaimed = o.optBoolean("silver_claimed"),
                goldClaimed = o.optBoolean("gold_claimed"),
                streakWeeks = o.optInt("streak_weeks"),
                rescueUsed = o.optBoolean("rescue_used"),
                bossEnergy = o.optInt("boss_energy"),
                shareCode = o.optString("share_code"),
                taskCode = o.optString("task_code"),
                taskTitle = o.optString("task_title"),
                taskProgress = o.optInt("task_progress"),
                taskTarget = o.optInt("task_target"),
                bossName = o.optString("boss_name", "Cướp biển Bóng Đêm"),
                bossHp = o.optInt("boss_hp"),
                bossMaxHp = o.optInt("boss_max_hp", 5000),
                seasonName = season.optString("name", "Mùa M4X"),
                seasonRewardItem = season.optString("reward_item"),
                leaders = leaders
            )
        }
    }

    suspend fun claimTreasureDay(session: Session): Result<TreasureActionResult> = treasureAction(session, "claim_treasure_day", JSONObject())

    suspend fun claimTreasureChest(session: Session, type: String): Result<TreasureActionResult> =
        treasureAction(session, "claim_treasure_chest", JSONObject().put("p_chest_type", type))

    suspend fun useTreasureRescueCard(session: Session): Result<TreasureActionResult> =
        treasureAction(session, "use_treasure_rescue_card", JSONObject())

    suspend fun attackTreasureBoss(session: Session): Result<TreasureActionResult> =
        treasureAction(session, "attack_treasure_boss", JSONObject())

    suspend fun redeemTreasureShareCode(session: Session, code: String): Result<TreasureActionResult> =
        treasureAction(session, "redeem_treasure_share_code", JSONObject().put("p_code", code.trim().uppercase()))

    private suspend fun treasureAction(session: Session, rpc: String, body: JSONObject): Result<TreasureActionResult> = io {
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/$rpc", session)
            .post(body.toString().toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể xử lý Bản đồ kho báu"))
            val o = rpcObject(text)
            TreasureActionResult(
                reward = o.optInt("reward"),
                bonus = o.optInt("bonus"),
                streakBonus = o.optInt("streak_bonus"),
                balance = o.optLong("balance"),
                openedDay = o.optInt("opened_day"),
                rescuedDay = o.optInt("rescued_day"),
                damage = o.optInt("damage"),
                bossHp = o.optInt("boss_hp"),
                bossMaxHp = o.optInt("boss_max_hp"),
                defeated = o.optBoolean("defeated"),
                rareItem = o.optBoolean("rare_item"),
                secret = o.optBoolean("secret"),
                shareCode = o.optString("share_code")
            )
        }
    }

    suspend fun petState(session: Session): Result<PetState> = petRpc(session, "get_m4x_pet")

    suspend fun feedPet(session: Session): Result<PetState> = petRpc(session, "feed_m4x_pet")

    private suspend fun petRpc(session: Session, rpc: String): Result<PetState> = io {
        val req = base("${SupabaseConfig.url}/rest/v1/rpc/$rpc", session)
            .post("{}".toRequestBody(jsonType)).build()
        http.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IOException(error(text, "Không thể tải thú cưng M4X"))
            val o = rpcObject(text)
            PetState(
                name = o.optString("name", "M4X Nova"),
                type = o.optString("type", "nova"),
                level = o.optInt("level", 1),
                xp = o.optInt("xp"),
                xpTarget = o.optInt("xp_target", 100),
                hunger = o.optInt("hunger", 80),
                food = o.optInt("food"),
                levelReward = o.optInt("level_reward"),
                balance = o.optLong("balance")
            )
        }
    }

    suspend fun updateAvatar(session: Session, imageUri: Uri): Result<Profile> = io {
        val resolver = context.contentResolver
        val mime = resolver.getType(imageUri).orEmpty()
        require(mime.startsWith("image/")) { "Hãy chọn một tệp ảnh" }
        val original = fileName(resolver, imageUri)
        val ext = original.substringAfterLast('.', "jpg").lowercase().take(5)
        val path = "${session.userId}/avatar/${UUID.randomUUID()}.$ext"
        uploadFile(session, imageUri, path)
        val publicUrl = "${SupabaseConfig.url}/storage/v1/object/public/themes/$path"
        patch(
            "/rest/v1/profiles?id=eq.${session.userId}",
            JSONObject().put("avatar_url", publicUrl).toString(),
            session
        )
        val a = get("/rest/v1/profiles?id=eq.${session.userId}&select=id,username,display_name,role,points,avatar_url", session)
        if (a.length() == 0) throw IOException("Không tìm thấy hồ sơ")
        val o = a.getJSONObject(0)
        Profile(
            o.getString("id"),
            o.optString("username"),
            o.optString("display_name"),
            o.optString("role", "user"),
            o.optLong("points"),
            o.optString("avatar_url")
        )
    }

    suspend fun hasActiveAirdrop(session: Session): Result<Boolean> = io {
        val a = get("/rest/v1/airdrops?active=eq.true&claimed_by=is.null&expires_at=gt.${isoAfter(0)}&select=id&limit=1", session)
        a.length() > 0
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

    suspend fun onlineControlConfig(
        session: Session
    ): Result<OnlineControlConfig> = io {
        val rows = get(
            "/rest/v1/online_control_config?id=eq.main&select=*",
            session
        )
        if (rows.length() == 0) {
            OnlineControlConfig()
        } else {
            parseOnlineControlConfig(rows.getJSONObject(0))
        }
    }

    suspend fun updateOnlineControlConfig(
        session: Session,
        config: OnlineControlConfig
    ): Result<OnlineControlConfig> = io {
        require(config.bannerTitle.trim().isNotBlank()) {
            "Tiêu đề banner không được để trống"
        }
        require(config.dailyQuestReward in 0..1_000_000) {
            "Thưởng nhiệm vụ không hợp lệ"
        }
        require(config.checkinRewards.isNotEmpty()) {
            "Cần ít nhất một phần thưởng điểm danh"
        }
        require(config.checkinRewards.all { it in 0..1_000_000 }) {
            "Phần thưởng điểm danh không hợp lệ"
        }
        require(config.spinCost in 0..1_000_000) {
            "Phí vòng quay không hợp lệ"
        }
        require(
            config.spinRewards.isNotEmpty() &&
                config.spinRewards.all {
                    it.reward in 0..1_000_000 &&
                        it.weight > 0
                }
        ) {
            "Danh sách phần thưởng vòng quay không hợp lệ"
        }
        require(config.fishingRewardMultiplier in 0.1..10.0) {
            "Hệ số thưởng cá phải từ 0.1 đến 10"
        }
        require(config.fishingBossHpMultiplier in 0.1..10.0) {
            "Hệ số máu Boss phải từ 0.1 đến 10"
        }

        val checkinJson = JSONArray()
        config.checkinRewards.forEach { checkinJson.put(it) }

        val spinJson = JSONArray()
        config.spinRewards.forEach {
            spinJson.put(
                JSONObject()
                    .put("reward", it.reward)
                    .put("weight", it.weight)
            )
        }

        val body = JSONObject()
            .put("banner_enabled", config.bannerEnabled)
            .put("banner_title", config.bannerTitle.trim())
            .put(
                "banner_subtitle",
                config.bannerSubtitle.trim()
            )
            .put("notice_enabled", config.noticeEnabled)
            .put("notice_title", config.noticeTitle.trim())
            .put(
                "notice_message",
                config.noticeMessage.trim()
            )
            .put(
                "daily_quest_enabled",
                config.dailyQuestEnabled
            )
            .put(
                "daily_quest_title",
                config.dailyQuestTitle.trim()
            )
            .put(
                "daily_quest_description",
                config.dailyQuestDescription.trim()
            )
            .put(
                "daily_quest_reward",
                config.dailyQuestReward
            )
            .put("checkin_enabled", config.checkinEnabled)
            .put("checkin_rewards", checkinJson)
            .put("spin_enabled", config.spinEnabled)
            .put("spin_cost", config.spinCost)
            .put("spin_rewards", spinJson)
            .put("fishing_enabled", config.fishingEnabled)
            .put(
                "fishing_closed_message",
                config.fishingClosedMessage.trim()
            )
            .put(
                "fishing_reward_multiplier",
                config.fishingRewardMultiplier
            )
            .put(
                "fishing_boss_hp_multiplier",
                config.fishingBossHpMultiplier
            )
            .put(
                "featured_theme_id",
                if (config.featuredThemeId.isBlank()) {
                    JSONObject.NULL
                } else {
                    config.featuredThemeId
                }
            )
        patch(
            "/rest/v1/online_control_config?id=eq.main",
            body.toString(),
            session
        )

        val rows = get(
            "/rest/v1/online_control_config?id=eq.main&select=*",
            session
        )
        if (rows.length() == 0) {
            OnlineControlConfig()
        } else {
            parseOnlineControlConfig(rows.getJSONObject(0))
        }
    }

    suspend fun claimOnlineDailyQuest(
        session: Session
    ): Result<OnlineCoinResult> = io {
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/claim_online_daily_quest",
            session
        ).post("{}".toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể nhận nhiệm vụ online")
                )
            }
            val o = rpcObject(raw)
            OnlineCoinResult(
                reward = o.optInt("reward"),
                balance = o.optLong("balance"),
                message = o.optString(
                    "message",
                    "Đã nhận quà nhiệm vụ online"
                )
            )
        }
    }

    suspend fun claimOnlineCheckin(
        session: Session
    ): Result<OnlineCoinResult> = io {
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/claim_online_checkin",
            session
        ).post("{}".toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể điểm danh")
                )
            }
            val o = rpcObject(raw)
            OnlineCoinResult(
                reward = o.optInt("reward"),
                balance = o.optLong("balance"),
                message = o.optString(
                    "message",
                    "Điểm danh thành công"
                ),
                streak = o.optInt("streak")
            )
        }
    }

    suspend fun spinOnlineWheel(
        session: Session
    ): Result<OnlineCoinResult> = io {
        val request = base(
            "${SupabaseConfig.url}/rest/v1/rpc/spin_online_wheel",
            session
        ).post("{}".toRequestBody(jsonType)).build()

        http.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(
                    error(raw, "Không thể quay vòng quay")
                )
            }
            val o = rpcObject(raw)
            OnlineCoinResult(
                reward = o.optInt("reward"),
                balance = o.optLong("balance"),
                message = o.optString(
                    "message",
                    "Đã quay vòng quay"
                ),
                cost = o.optInt("cost")
            )
        }
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
            homeBannerSubtitle = o.optString("home_banner_subtitle", "Kho giao diện HyperOS & MIUI"),
            webFootballUrl = o.optString("web_football_url", "https://xoilacxtl.tv/"),
            webMovieUrl = o.optString("web_movie_url", "https://cobephim.pro/"),
            webAdultUrl = o.optString("web_adult_url", "https://vnsextop1.com/"),
            webFootballEnabled = o.optBoolean("web_football_enabled", true),
            webMovieEnabled = o.optBoolean("web_movie_enabled", true),
            webAdultEnabled = o.optBoolean("web_adult_enabled", true)
        )
    }

    suspend fun updateWebConfig(
        session: Session,
        footballUrl: String,
        movieUrl: String,
        adultUrl: String,
        footballEnabled: Boolean,
        movieEnabled: Boolean,
        adultEnabled: Boolean
    ): Result<RemoteConfig> = io {
        listOf(footballUrl, movieUrl, adultUrl).forEach { require(it.startsWith("https://")) { "Link phải bắt đầu bằng https://" } }
        val body = JSONObject()
            .put("web_football_url", footballUrl.trim())
            .put("web_movie_url", movieUrl.trim())
            .put("web_adult_url", adultUrl.trim())
            .put("web_football_enabled", footballEnabled)
            .put("web_movie_enabled", movieEnabled)
            .put("web_adult_enabled", adultEnabled)
        patch("/rest/v1/app_config?id=eq.main", body.toString(), session)
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
            homeBannerSubtitle = o.optString("home_banner_subtitle", "Kho giao diện HyperOS & MIUI"),
            webFootballUrl = o.optString("web_football_url", "https://xoilacxtl.tv/"),
            webMovieUrl = o.optString("web_movie_url", "https://cobephim.pro/"),
            webAdultUrl = o.optString("web_adult_url", "https://vnsextop1.com/"),
            webFootballEnabled = o.optBoolean("web_football_enabled", true),
            webMovieEnabled = o.optBoolean("web_movie_enabled", true),
            webAdultEnabled = o.optBoolean("web_adult_enabled", true)
        )
    }

    private fun parseOnlineControlConfig(
        o: JSONObject
    ): OnlineControlConfig {
        val checkinJson =
            o.optJSONArray("checkin_rewards") ?: JSONArray()
        val checkinRewards =
            List(checkinJson.length()) { index ->
                checkinJson.optInt(index)
            }.filter { it >= 0 }
                .ifEmpty {
                    listOf(50, 75, 100, 125, 150, 200, 300)
                }

        val spinJson =
            o.optJSONArray("spin_rewards") ?: JSONArray()
        val spinRewards =
            List(spinJson.length()) { index ->
                val reward = spinJson.optJSONObject(index)
                    ?: JSONObject()
                OnlineSpinReward(
                    reward = reward.optInt("reward"),
                    weight = reward.optInt("weight", 1)
                        .coerceAtLeast(1)
                )
            }.ifEmpty {
                listOf(
                    OnlineSpinReward(0, 20),
                    OnlineSpinReward(10, 45),
                    OnlineSpinReward(50, 20),
                    OnlineSpinReward(100, 10),
                    OnlineSpinReward(250, 5)
                )
            }

        return OnlineControlConfig(
            bannerEnabled =
                o.optBoolean("banner_enabled", true),
            bannerTitle =
                o.optString("banner_title", "M4X Theme"),
            bannerSubtitle =
                o.optString(
                    "banner_subtitle",
                    "Kho giao diện HyperOS & MIUI"
                ),
            noticeEnabled =
                o.optBoolean("notice_enabled", false),
            noticeTitle =
                o.optString("notice_title", "Thông báo"),
            noticeMessage =
                o.optString("notice_message"),
            dailyQuestEnabled =
                o.optBoolean("daily_quest_enabled", true),
            dailyQuestTitle =
                o.optString(
                    "daily_quest_title",
                    "Điểm danh nhiệm vụ online"
                ),
            dailyQuestDescription =
                o.optString(
                    "daily_quest_description",
                    "Mở ứng dụng và nhận quà hôm nay"
                ),
            dailyQuestReward =
                o.optInt("daily_quest_reward", 100),
            checkinEnabled =
                o.optBoolean("checkin_enabled", true),
            checkinRewards = checkinRewards,
            spinEnabled =
                o.optBoolean("spin_enabled", true),
            spinCost =
                o.optInt("spin_cost", 25),
            spinRewards = spinRewards,
            fishingEnabled =
                o.optBoolean("fishing_enabled", true),
            fishingClosedMessage =
                o.optString(
                    "fishing_closed_message",
                    "M4X Fishing đang bảo trì"
                ),
            fishingRewardMultiplier =
                o.optDouble(
                    "fishing_reward_multiplier",
                    1.0
                ),
            fishingBossHpMultiplier =
                o.optDouble(
                    "fishing_boss_hp_multiplier",
                    1.0
                ),
            featuredThemeId =
                o.optString("featured_theme_id")
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

    private fun rpcObject(text: String): JSONObject {
        val trimmed = text.trim()
        return if (trimmed.startsWith("[")) {
            val a = JSONArray(trimmed)
            if (a.length() == 0) JSONObject() else a.getJSONObject(0)
        } else JSONObject(trimmed)
    }


    private fun parseArenaTicket(json: JSONObject): ArenaMatchTicket {
        val playersJson = json.optJSONArray("players") ?: JSONArray()
        val players = List(playersJson.length()) { index ->
            val player = playersJson.optJSONObject(index) ?: JSONObject()
            ArenaOnlinePlayer(
                userId = player.optString("userId")
                    .ifBlank { player.optString("user_id") },
                displayName = player.optString("displayName")
                    .ifBlank {
                        player.optString("display_name", "M4X Hunter")
                    },
                slot = if (player.has("slot")) {
                    player.optInt("slot")
                } else {
                    player.optInt("player_slot")
                }
            )
        }.filter { it.userId.isNotBlank() }

        return ArenaMatchTicket(
            matchId = json.optString("matchId")
                .ifBlank { json.optString("match_id") },
            slot = json.optInt("slot"),
            hostUserId = json.optString("hostUserId")
                .ifBlank { json.optString("host_user_id") },
            status = json.optString("status", "waiting"),
            players = players,
            waitSeconds = json.optInt("waitSeconds")
                .takeIf { it > 0 }
                ?: json.optInt("wait_seconds")
        )
    }

    private fun parseEvents(a: JSONArray) = List(a.length()) { i ->
        a.getJSONObject(i).let { EventItem(it.getString("id"), it.optString("title"), it.optString("description"), it.optString("start_at"), it.optString("end_at"), it.optBoolean("active")) }
    }

    private fun parseThemeReviewHistory(a: JSONArray) = List(a.length()) { i ->
        a.getJSONObject(i).let { o ->
            ThemeReviewHistory(
                id = o.optString("id"),
                themeId = o.optString("theme_id"),
                themeTitle = o.optString("theme_title", "Theme đã xóa"),
                ownerId = o.optString("owner_id"),
                ownerName = o.optString("owner_name", "Người đăng"),
                reviewerId = o.optString("reviewer_id"),
                reviewerName = o.optString("reviewer_name", "Người duyệt"),
                reviewerRole = o.optString("reviewer_role", "creator"),
                decision = o.optString("decision"),
                reason = o.optString("reason"),
                checklist = ThemeReviewChecklist.fromJson(
                    o.optJSONObject("checklist")?.toString() ?: "{}"
                ),
                createdAt = o.optString("created_at")
            )
        }
    }

    private fun parseCreatorReputation(o: JSONObject) = CreatorReputation(
        userId = o.optString("user_id"),
        score = o.optInt("score", 50),
        approvedCount = o.optInt("approved_count"),
        rejectedCount = o.optInt("rejected_count"),
        revokedCount = o.optInt("revoked_count"),
        totalReviews = o.optInt("total_reviews")
    )

    private fun parseThemeReviewNotifications(a: JSONArray) = List(a.length()) { i ->
        a.getJSONObject(i).let { o ->
            ThemeReviewNotification(
                id = o.optString("id"),
                themeId = o.optString("theme_id"),
                type = o.optString("type"),
                title = o.optString("title", "Cập nhật theme"),
                message = o.optString("message"),
                reason = o.optString("reason"),
                readAt = o.optString("read_at"),
                createdAt = o.optString("created_at")
            )
        }
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
                createdAt = o.optString("created_at"), coinPrice = o.optInt("coin_price"),
                clientValidationStatus = o.optString("client_validation_status", "unchecked"),
                clientValidationMessage = o.optString("client_validation_message"),
                clientFileSha256 = o.optString("client_file_sha256"),
                clientFileSizeBytes = o.optLong("client_file_size_bytes"),
                clientValidationAt = o.optString("client_validation_at"),
                clientSafetyScore = o.optInt("client_safety_score"),
                clientSafetyLevel = o.optString("client_safety_level", "danger"),
                clientThemeMetadata = o.optJSONObject("client_theme_metadata")?.toString() ?: "{}",
                clientModuleReport = o.optJSONArray("client_module_report")?.toString() ?: "[]",
                clientValidationReport = o.optJSONObject("client_validation_report")?.toString() ?: "{}",
                approvedFileSha256 = o.optString("approved_file_sha256"),
                approvedFileSizeBytes = o.optLong("approved_file_size_bytes")
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

    private fun error(text: String, fallback: String): String {
        if (text.contains("quest_claims_user_id_quest_id_key")) return "Nhiệm vụ này đã được nhận"
        if (text.contains("giftcode_claims_giftcode_id_user_id_key")) return "Bạn đã sử dụng Giftcode này"
        if (text.contains("giftcodes_code_key")) return "Mã Giftcode đã tồn tại"
        return runCatching {
            JSONObject(text).optString("msg").ifBlank { JSONObject(text).optString("message") }.ifBlank { fallback }
        }.getOrDefault(fallback)
    }

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
