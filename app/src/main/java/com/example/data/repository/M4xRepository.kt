package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.*

data class AdminAnalytics(
    val totalUsers: Int,
    val totalThemes: Int,
    val pendingCount: Int,
    val totalDownloads: Int,
    val totalViews: Int,
    val averageRating: Float,
    val dailyStats: List<DailyStat>
)

data class DailyStat(
    val dayLabel: String,
    val downloads: Int,
    val views: Int,
    val submissions: Int
)

data class MtzValidationResult(
    val isValid: Boolean,
    val fileSizeMb: Double,
    val log: String,
    val detectedOs: String
)

class M4xRepository(private val db: M4xDatabase) {

    private val userDao = db.userDao()
    private val themeDao = db.themeDao()
    private val commentDao = db.commentDao()
    private val favoriteDao = db.favoriteDao()
    private val followDao = db.followDao()
    private val bugReportDao = db.bugReportDao()
    private val notificationDao = db.notificationDao()
    private val taskDao = db.rewardTaskDao()

    // Active User State
    private val _currentUserId = MutableStateFlow<Long>(4) // Default normal user "MinhTu_MIUI"
    val currentUserId: StateFlow<Long> = _currentUserId.asStateFlow()

    val currentUser: Flow<UserEntity?> = _currentUserId.flatMapLatest { id ->
        userDao.getUserById(id)
    }

    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()

    fun setCurrentUserId(userId: Long) {
        _currentUserId.value = userId
    }

    // Themes
    val approvedThemes: Flow<List<ThemeEntity>> = themeDao.getApprovedThemes()
    val pendingThemes: Flow<List<ThemeEntity>> = themeDao.getPendingThemes()
    val allThemes: Flow<List<ThemeEntity>> = themeDao.getAllThemes()

    fun getThemeById(id: Long): Flow<ThemeEntity?> = themeDao.getThemeById(id)

    fun getThemesByCreator(creatorId: Long): Flow<List<ThemeEntity>> = themeDao.getThemesByCreator(creatorId)

    fun getCommentsByTheme(themeId: Long): Flow<List<CommentEntity>> = commentDao.getCommentsByTheme(themeId)

    fun getFavoriteIdsForCurrentUser(): Flow<List<Long>> = _currentUserId.flatMapLatest { id ->
        favoriteDao.getFavoriteThemeIds(id)
    }

    fun getFollowedCreatorIds(): Flow<List<Long>> = _currentUserId.flatMapLatest { id ->
        followDao.getFollowedCreatorIds(id)
    }

    fun getNotificationsForCurrentUser(): Flow<List<NotificationEntity>> = _currentUserId.flatMapLatest { id ->
        notificationDao.getNotificationsForUser(id)
    }

    val allBugReports: Flow<List<BugReportEntity>> = bugReportDao.getAllBugReports()
    val rewardTasks: Flow<List<RewardTaskEntity>> = taskDao.getAllTasks()

    // Analytics calculation
    val adminAnalytics: Flow<AdminAnalytics> = combine(
        allUsers,
        allThemes
    ) { users, themes ->
        val totalUsers = users.size
        val totalThemes = themes.size
        val pendingCount = themes.count { it.status == ThemeStatus.PENDING }
        val totalDownloads = themes.sumOf { it.downloadCount }
        val totalViews = themes.sumOf { it.viewCount }
        val approved = themes.filter { it.status == ThemeStatus.APPROVED }
        val avgRating = if (approved.isNotEmpty()) {
            approved.map { it.averageRating }.average().toFloat()
        } else 0f

        val dummyDaily = listOf(
            DailyStat("T2", 340, 1200, 2),
            DailyStat("T3", 520, 1850, 4),
            DailyStat("T4", 410, 1400, 1),
            DailyStat("T5", 680, 2300, 3),
            DailyStat("T6", 920, 3100, 5),
            DailyStat("T7", 1250, 4200, 8),
            DailyStat("CN", 1100, 3800, 6)
        )

        AdminAnalytics(
            totalUsers = totalUsers,
            totalThemes = totalThemes,
            pendingCount = pendingCount,
            totalDownloads = totalDownloads,
            totalViews = totalViews,
            averageRating = (Math.round(avgRating * 10) / 10.0).toFloat(),
            dailyStats = dummyDaily
        )
    }

    // Actions
    suspend fun incrementViewCount(themeId: Long) {
        themeDao.incrementViewCount(themeId)
    }

    suspend fun incrementDownloadCount(themeId: Long) {
        themeDao.incrementDownloadCount(themeId)
    }

    suspend fun toggleFavorite(themeId: Long) {
        val userId = _currentUserId.value
        val existing = favoriteDao.getFavorite(userId, themeId)
        if (existing != null) {
            favoriteDao.removeFavorite(userId, themeId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(userId = userId, themeId = themeId))
        }
    }

    suspend fun toggleFollowCreator(creatorId: Long) {
        val userId = _currentUserId.value
        val currentFollows = favoriteDao.getFavoriteThemeIds(userId).first() // proxy check
        followDao.followCreator(FollowEntity(userId = userId, creatorId = creatorId))
    }

    suspend fun addComment(themeId: Long, rating: Int, text: String) {
        val user = currentUser.first() ?: return
        val comment = CommentEntity(
            themeId = themeId,
            userId = user.id,
            userName = user.username,
            userAvatar = user.avatarUrl,
            rating = rating,
            commentText = text
        )
        commentDao.insertComment(comment)

        // Recalculate average rating for theme
        val comments = commentDao.getCommentsByTheme(themeId).first()
        if (comments.isNotEmpty()) {
            val newAvg = comments.map { it.rating }.average().toFloat()
            val count = comments.size
            themeDao.updateRating(themeId, (Math.round(newAvg * 10) / 10.0).toFloat(), count)
        }
    }

    // Automated MTZ file validation simulation
    fun validateMtzFile(fileName: String, sizeMb: Double): MtzValidationResult {
        val lowerName = fileName.lowercase()
        val isMtz = lowerName.endsWith(".mtz") || lowerName.endsWith(".zip")
        val sizeOk = sizeMb in 2.0..100.0

        if (!isMtz) {
            return MtzValidationResult(
                isValid = false,
                fileSizeMb = sizeMb,
                log = "❌ Lỗi: Định dạng file phải là .mtz hoặc .zip chuẩn MIUI/HyperOS.",
                detectedOs = "Chưa xác định"
            )
        }

        if (!sizeOk) {
            return MtzValidationResult(
                isValid = false,
                fileSizeMb = sizeMb,
                log = "❌ Lỗi: Dung lượng file (${sizeMb}MB) vượt ngưỡng quy định (2MB - 100MB).",
                detectedOs = "Chưa xác định"
            )
        }

        val os = if (lowerName.contains("hyperos2") || lowerName.contains("v2")) {
            "HyperOS 2.0"
        } else if (lowerName.contains("hyperos") || lowerName.contains("v1")) {
            "HyperOS 1.0"
        } else {
            "MIUI 14"
        }

        return MtzValidationResult(
            isValid = true,
            fileSizeMb = sizeMb,
            log = "✅ Đã kiểm tra cấu trúc .mtz thành công:\n• `theme_manifest.xml` hợp lệ\n• `lockscreen/` depth engine verified\n• `icons/` 108dp asset package OK\n• `clock_manifest.xml` no error syntax",
            detectedOs = os
        )
    }

    suspend fun uploadTheme(
        title: String,
        osCompatibility: String,
        category: String,
        description: String,
        tags: String,
        fileName: String,
        fileSizeMb: Double
    ): Boolean {
        val user = currentUser.first() ?: return false
        val validation = validateMtzFile(fileName, fileSizeMb)

        val newTheme = ThemeEntity(
            title = title,
            creatorId = user.id,
            creatorName = user.username,
            osCompatibility = osCompatibility,
            category = category,
            description = description,
            version = "1.0.0",
            fileSizeMb = fileSizeMb,
            tags = tags,
            status = ThemeStatus.PENDING,
            isAutoCheckPassed = validation.isValid,
            autoCheckLog = validation.log
        )

        val themeId = themeDao.insertTheme(newTheme)

        // Notify user & creator
        notificationDao.insertNotification(
            NotificationEntity(
                targetUserId = user.id,
                title = "Đã gửi Theme thành công",
                message = "Theme '$title' đã được tự động kiểm tra .mtz (${validation.detectedOs}) và chuyển sang hàng chờ duyệt Admin.",
                type = "APPROVAL"
            )
        )

        return themeId > 0
    }

    // Admin Operations
    suspend fun approveTheme(themeId: Long) {
        themeDao.updateThemeStatus(themeId, ThemeStatus.APPROVED, "")
        val theme = themeDao.getThemeById(themeId).first()
        if (theme != null) {
            notificationDao.insertNotification(
                NotificationEntity(
                    targetUserId = theme.creatorId,
                    title = "🎉 Theme đã được phê duyệt!",
                    message = "Chúc mừng! Theme '${theme.title}' của bạn đã được duyệt và đăng công khai trên M4X Theme Store.",
                    type = "APPROVAL"
                )
            )
            // Reward points to creator
            userDao.addPoints(theme.creatorId, 100)
        }
    }

    suspend fun rejectTheme(themeId: Long, reason: String) {
        themeDao.updateThemeStatus(themeId, ThemeStatus.REJECTED, reason)
        val theme = themeDao.getThemeById(themeId).first()
        if (theme != null) {
            notificationDao.insertNotification(
                NotificationEntity(
                    targetUserId = theme.creatorId,
                    title = "⚠️ Theme từ chối phê duyệt",
                    message = "Theme '${theme.title}' không đạt yêu cầu. Lý do: $reason",
                    type = "REJECTED"
                )
            )
        }
    }

    suspend fun toggleFeaturedTheme(themeId: Long, isFeatured: Boolean) {
        themeDao.setFeatured(themeId, isFeatured)
    }

    suspend fun deleteTheme(themeId: Long) {
        themeDao.deleteTheme(themeId)
    }

    suspend fun banUser(userId: Long, isBanned: Boolean) {
        userDao.setBannedStatus(userId, isBanned)
    }

    suspend fun sendBroadcastNotification(title: String, message: String) {
        val users = userDao.getAllUsers().first()
        users.forEach { user ->
            notificationDao.insertNotification(
                NotificationEntity(
                    targetUserId = user.id,
                    title = title,
                    message = message,
                    type = "SYSTEM"
                )
            )
        }
    }

    suspend fun submitBugReport(themeId: Long, themeTitle: String, title: String, desc: String, osVer: String) {
        val user = currentUser.first() ?: return
        bugReportDao.insertBugReport(
            BugReportEntity(
                themeId = themeId,
                themeTitle = themeTitle,
                reporterId = user.id,
                reporterName = user.username,
                bugTitle = title,
                description = desc,
                osVersion = osVer
            )
        )
    }

    suspend fun claimTaskReward(taskId: Long, rewardPoints: Int) {
        val user = currentUser.first() ?: return
        taskDao.completeTask(taskId)
        userDao.addPoints(user.id, rewardPoints)
    }

    suspend fun markNotificationsRead() {
        val userId = _currentUserId.value
        notificationDao.markAllAsRead(userId)
    }
}
