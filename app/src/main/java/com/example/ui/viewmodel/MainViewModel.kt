package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.AdminAnalytics
import com.example.data.repository.M4xRepository
import com.example.data.repository.MtzValidationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ScreenRoute {
    HOME, THEME_DETAIL, UPLOAD_THEME, ADMIN_DASHBOARD, PROFILE, BUG_REPORTS, REWARDS, NOTIFICATIONS
}

enum class HomeTab {
    FEATURED, LATEST, TOP_DOWNLOADS, TOP_RATED, UPDATES
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isNotBlank() && password.isNotBlank()) {
            _isAuthenticated.value = true
            showSnackbar("Đăng nhập thành công")
        } else showSnackbar("Vui lòng nhập email và mật khẩu")
    }

    fun logout() {
        _isAuthenticated.value = false
        _currentScreen.value = ScreenRoute.HOME
    }


    private val db = M4xDatabase.getInstance(application)
    val repository = M4xRepository(db)

    // Current Screen
    private val _currentScreen = MutableStateFlow(ScreenRoute.HOME)
    val currentScreen: StateFlow<ScreenRoute> = _currentScreen.asStateFlow()

    // Selected Theme for detail view
    private val _selectedThemeId = MutableStateFlow<Long?>(1)
    val selectedThemeId: StateFlow<Long?> = _selectedThemeId.asStateFlow()

    // Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedOs = MutableStateFlow("ALL") // "ALL", "HyperOS 2.0", "HyperOS 1.0", "MIUI 14", "MIUI 13"
    val selectedOs: StateFlow<String> = _selectedOs.asStateFlow()

    private val _selectedCategory = MutableStateFlow("ALL") // "ALL", "Minimal", "Cyberpunk", "iOS Style", "Dark Mode", "Anime"
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _activeHomeTab = MutableStateFlow(HomeTab.FEATURED)
    val activeHomeTab: StateFlow<HomeTab> = _activeHomeTab.asStateFlow()

    // User & Role State
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteIds: StateFlow<List<Long>> = repository.getFavoriteIdsForCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val followedCreatorIds: StateFlow<List<Long>> = repository.getFollowedCreatorIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.getNotificationsForCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifCount: StateFlow<Int> = notifications.map { list -> list.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val adminAnalytics: StateFlow<AdminAnalytics?> = repository.adminAnalytics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pendingThemes: StateFlow<List<ThemeEntity>> = repository.pendingThemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bugReports: StateFlow<List<BugReportEntity>> = repository.allBugReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rewardTasks: StateFlow<List<RewardTaskEntity>> = repository.rewardTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Approved Themes
    val filteredApprovedThemes: StateFlow<List<ThemeEntity>> = combine(
        repository.approvedThemes,
        _searchQuery,
        _selectedOs,
        _selectedCategory,
        _activeHomeTab
    ) { themes, query, osFilter, catFilter, tab ->
        themes.filter { theme ->
            val matchesQuery = query.isEmpty() ||
                    theme.title.contains(query, ignoreCase = true) ||
                    theme.creatorName.contains(query, ignoreCase = true) ||
                    theme.tags.contains(query, ignoreCase = true)

            val matchesOs = osFilter == "ALL" || theme.osCompatibility.equals(osFilter, ignoreCase = true)
            val matchesCat = catFilter == "ALL" || theme.category.equals(catFilter, ignoreCase = true)

            matchesQuery && matchesOs && matchesCat
        }.let { filtered ->
            when (tab) {
                HomeTab.FEATURED -> filtered.sortedByDescending { it.isFeatured }
                HomeTab.LATEST -> filtered.sortedByDescending { it.createdTimestamp }
                HomeTab.TOP_DOWNLOADS -> filtered.sortedByDescending { it.downloadCount }
                HomeTab.TOP_RATED -> filtered.sortedByDescending { it.averageRating }
                HomeTab.UPDATES -> filtered.sortedByDescending { it.version }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected theme flow
    val selectedTheme: StateFlow<ThemeEntity?> = combine(
        repository.allThemes,
        _selectedThemeId
    ) { themes, id ->
        themes.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedThemeComments: StateFlow<List<CommentEntity>> = _selectedThemeId.flatMapLatest { id ->
        if (id != null) repository.getCommentsByTheme(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // User's own themes (for Creator view in Profile)
    val myThemes: StateFlow<List<ThemeEntity>> = currentUser.flatMapLatest { user ->
        if (user != null) repository.getThemesByCreator(user.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toast / Snackbar message
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Upload Form State
    var uploadTitle = MutableStateFlow("")
    var uploadOs = MutableStateFlow("HyperOS 2.0")
    var uploadCategory = MutableStateFlow("Cyberpunk")
    var uploadDescription = MutableStateFlow("")
    var uploadTags = MutableStateFlow("HyperOS,Dark,3D")
    var uploadFileName = MutableStateFlow("M4X_Theme_Pack_v1.mtz")
    var uploadFileSizeMb = MutableStateFlow(28.5)
    var uploadValidationResult = MutableStateFlow<MtzValidationResult?>(null)

    // OTA Simulation
    private val _isOtaChecking = MutableStateFlow(false)
    val isOtaChecking: StateFlow<Boolean> = _isOtaChecking.asStateFlow()

    private val _otaVersionMessage = MutableStateFlow<String?>(null)
    val otaVersionMessage: StateFlow<String?> = _otaVersionMessage.asStateFlow()

    // Navigation helper
    fun navigateTo(screen: ScreenRoute) {
        _currentScreen.value = screen
    }

    fun openThemeDetail(themeId: Long) {
        _selectedThemeId.value = themeId
        viewModelScope.launch {
            repository.incrementViewCount(themeId)
        }
        _currentScreen.value = ScreenRoute.THEME_DETAIL
    }

    // Role switcher helper
    fun switchUserRole(userId: Long) {
        repository.setCurrentUserId(userId)
        val roleName = when (userId) {
            1L -> "Quản trị viên (Admin)"
            2L -> "Nhà sáng tạo (Creator - HyperDesign)"
            3L -> "Nhà sáng tạo (Creator - NeoCraft)"
            else -> "Người dùng (User - MinhTu)"
        }
        showSnackbar("Đã chuyển sang tài khoản: $roleName")
    }

    // Filter updates
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedOs(os: String) {
        _selectedOs.value = os
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setActiveHomeTab(tab: HomeTab) {
        _activeHomeTab.value = tab
    }

    // User theme actions
    fun toggleFavorite(themeId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(themeId)
            val isFav = favoriteIds.value.contains(themeId)
            if (isFav) {
                showSnackbar("Đã xóa khỏi danh sách Yêu thích")
            } else {
                showSnackbar("Đã thêm vào danh sách Yêu thích ❤️")
            }
        }
    }

    fun toggleFollowCreator(creatorId: Long) {
        viewModelScope.launch {
            repository.toggleFollowCreator(creatorId)
            showSnackbar("Đã theo dõi nhà sáng tạo này!")
        }
    }

    fun addComment(themeId: Long, rating: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(themeId, rating, text)
            showSnackbar("Cảm ơn bạn đã đánh giá & bình luận!")
        }
    }

    fun downloadTheme(themeId: Long, title: String) {
        viewModelScope.launch {
            repository.incrementDownloadCount(themeId)
            showSnackbar("Đang tải file .mtz '$title'... Đã hoàn tất! Bạn có thể áp dụng trong Quản lý Theme HyperOS.")
        }
    }

    // Upload & Auto validation
    fun testValidateMtz() {
        val res = repository.validateMtzFile(uploadFileName.value, uploadFileSizeMb.value)
        uploadValidationResult.value = res
        if (res.isValid) {
            showSnackbar("Kiểm tra file .mtz hợp lệ! Sẵn sàng tải lên.")
        } else {
            showSnackbar("File .mtz không đạt tiêu chuẩn!")
        }
    }

    fun submitUploadTheme() {
        if (uploadTitle.value.isBlank() || uploadDescription.value.isBlank()) {
            showSnackbar("Vui lòng điền đầy đủ Tên Theme và Mô tả!")
            return
        }

        viewModelScope.launch {
            val success = repository.uploadTheme(
                title = uploadTitle.value,
                osCompatibility = uploadOs.value,
                category = uploadCategory.value,
                description = uploadDescription.value,
                tags = uploadTags.value,
                fileName = uploadFileName.value,
                fileSizeMb = uploadFileSizeMb.value
            )

            if (success) {
                showSnackbar("Theme của bạn đã được tải lên và gửi tới Admin kiểm duyệt!")
                uploadTitle.value = ""
                uploadDescription.value = ""
                uploadValidationResult.value = null
                _currentScreen.value = ScreenRoute.PROFILE
            } else {
                showSnackbar("Lỗi khi tải lên theme. Vui lòng thử lại.")
            }
        }
    }

    // Admin Operations
    fun approveTheme(themeId: Long) {
        viewModelScope.launch {
            repository.approveTheme(themeId)
            showSnackbar("Đã phê duyệt theme thành công!")
        }
    }

    fun rejectTheme(themeId: Long, reason: String) {
        viewModelScope.launch {
            repository.rejectTheme(themeId, if (reason.isBlank()) "Không đạt tiêu chuẩn thiết kế" else reason)
            showSnackbar("Đã từ chối phê duyệt theme.")
        }
    }

    fun toggleFeatured(themeId: Long, isFeatured: Boolean) {
        viewModelScope.launch {
            repository.toggleFeaturedTheme(themeId, isFeatured)
            showSnackbar(if (isFeatured) "Đã gắn nhãn Nổi bật" else "Đã bỏ nhãn Nổi bật")
        }
    }

    fun deleteTheme(themeId: Long) {
        viewModelScope.launch {
            repository.deleteTheme(themeId)
            showSnackbar("Đã xóa theme khỏi hệ thống.")
        }
    }

    fun banUser(userId: Long, isBanned: Boolean) {
        viewModelScope.launch {
            repository.banUser(userId, isBanned)
            showSnackbar(if (isBanned) "Đã khóa tài khoản người dùng" else "Đã mở khóa tài khoản")
        }
    }

    fun sendBroadcastNotif(title: String, message: String) {
        if (title.isBlank() || message.isBlank()) return
        viewModelScope.launch {
            repository.sendBroadcastNotification(title, message)
            showSnackbar("Đã gửi thông báo hệ thống tới toàn bộ người dùng!")
        }
    }

    // Bug report
    fun submitBugReport(themeId: Long, themeTitle: String, bugTitle: String, desc: String, osVer: String) {
        if (bugTitle.isBlank() || desc.isBlank()) return
        viewModelScope.launch {
            repository.submitBugReport(themeId, themeTitle, bugTitle, desc, osVer)
            showSnackbar("Đã gửi báo cáo lỗi tới nhà phát triển.")
        }
    }

    // Tasks & Rewards
    fun claimTaskReward(taskId: Long, points: Int) {
        viewModelScope.launch {
            repository.claimTaskReward(taskId, points)
            showSnackbar("Chúc mừng! Bạn đã nhận +$points điểm M4X Reward!")
        }
    }

    fun markNotificationsRead() {
        viewModelScope.launch {
            repository.markNotificationsRead()
        }
    }

    // OTA Simulation
    fun checkOtaUpdate() {
        viewModelScope.launch {
            _isOtaChecking.value = true
            _otaVersionMessage.value = null
            kotlinx.coroutines.delay(1200)
            _isOtaChecking.value = false
            _otaVersionMessage.value = "M4X Theme Store v2.4.0 (HyperOS 2.0 OTA Bundle) - Hệ thống mới nhất!"
            showSnackbar("Đã kiểm tra cập nhật OTA: Bạn đang ở phiên bản mới nhất!")
        }
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
