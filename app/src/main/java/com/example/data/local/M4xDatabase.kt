package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        ThemeEntity::class,
        CommentEntity::class,
        FavoriteEntity::class,
        FollowEntity::class,
        BugReportEntity::class,
        NotificationEntity::class,
        RewardTaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class M4xDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun themeDao(): ThemeDao
    abstract fun commentDao(): CommentDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun followDao(): FollowDao
    abstract fun bugReportDao(): BugReportDao
    abstract fun notificationDao(): NotificationDao
    abstract fun rewardTaskDao(): RewardTaskDao

    companion object {
        @Volatile
        private var INSTANCE: M4xDatabase? = null

        fun getInstance(context: Context): M4xDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    M4xDatabase::class.java,
                    "m4x_theme_db"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDatabase(database)
                    }
                }
            }
        }

        private suspend fun seedDatabase(db: M4xDatabase) {
            val userDao = db.userDao()
            val themeDao = db.themeDao()
            val commentDao = db.commentDao()
            val taskDao = db.rewardTaskDao()
            val notifDao = db.notificationDao()

            // 1. Initial Users
            val adminUser = UserEntity(
                id = 1,
                username = "M4X Admin",
                email = "admin@m4xtheme.vn",
                role = UserRole.ADMIN,
                points = 9999,
                bio = "Quản trị viên hệ thống M4X Theme Platform"
            )
            val creator1 = UserEntity(
                id = 2,
                username = "HyperDesign Studio",
                email = "creator@hyperdesign.com",
                role = UserRole.CREATOR,
                points = 1550,
                bio = "Chuyên thiết kế Theme HyperOS & MIUI cao cấp, Dark Glass Visuals"
            )
            val creator2 = UserEntity(
                id = 3,
                username = "NeoCraft UI",
                email = "neo@neocraft.io",
                role = UserRole.CREATOR,
                points = 820,
                bio = "Cyberpunk & iOS Minimalist theme creator cho Xiaomi"
            )
            val normalUser = UserEntity(
                id = 4,
                username = "MinhTu_MIUI",
                email = "user@gmail.com",
                role = UserRole.USER,
                points = 240,
                bio = "Mi Fan từ 2018, thích chủ đề tối & widget tùy biến"
            )

            userDao.insertUser(adminUser)
            userDao.insertUser(creator1)
            userDao.insertUser(creator2)
            userDao.insertUser(normalUser)

            // 2. Initial Themes
            val theme1 = ThemeEntity(
                id = 1,
                title = "CyberGlass HyperOS 2.0",
                creatorId = 2,
                creatorName = "HyperDesign Studio",
                osCompatibility = "HyperOS 2.0",
                category = "Cyberpunk",
                description = "Chủ đề phong cách kính thủy tinh Cyberpunk tương lai với Control Center tùy biến, Đồng hồ Lockscreen siêu đẹp, Bộ icon 3D Glow sắc nét cho HyperOS 2.0.",
                version = "2.1.0",
                fileSizeMb = 34.2,
                tags = "HyperOS,Glass,Cyber,Dark,3D",
                status = ThemeStatus.APPROVED,
                downloadCount = 4820,
                viewCount = 15900,
                favoriteCount = 1240,
                averageRating = 4.9f,
                ratingCount = 88,
                isFeatured = true
            )

            val theme2 = ThemeEntity(
                id = 2,
                title = "iOS 18 Pro Hyper Edition",
                creatorId = 3,
                creatorName = "NeoCraft UI",
                osCompatibility = "HyperOS 1.0",
                category = "iOS Style",
                description = "Giao diện iOS 18 chân thực trên Xiaomi! Dynamic Island hoạt động linh hoạt, Widget mượt mà, LockscreenDepth эффект đỉnh cao.",
                version = "1.8.4",
                fileSizeMb = 28.6,
                tags = "iOS,DynamicIsland,Minimal,Clean",
                status = ThemeStatus.APPROVED,
                downloadCount = 6120,
                viewCount = 21400,
                favoriteCount = 1890,
                averageRating = 4.8f,
                ratingCount = 115,
                isFeatured = true
            )

            val theme3 = ThemeEntity(
                id = 3,
                title = "Pure Minimal White MIUI 14",
                creatorId = 2,
                creatorName = "HyperDesign Studio",
                osCompatibility = "MIUI 14",
                category = "Minimal",
                description = "Phong cách tối giản trắng tinh tế, phông chữ chữ việt thanh thoát, tối ưu hóa pin và dung lượng RAM cực mượt.",
                version = "1.0.2",
                fileSizeMb = 18.1,
                tags = "Minimal,White,MIUI14,Clean,Smooth",
                status = ThemeStatus.APPROVED,
                downloadCount = 2300,
                viewCount = 8400,
                favoriteCount = 510,
                averageRating = 4.6f,
                ratingCount = 42,
                isFeatured = false
            )

            val theme4 = ThemeEntity(
                id = 4,
                title = "Neon Drive Dark Mode",
                creatorId = 3,
                creatorName = "NeoCraft UI",
                osCompatibility = "HyperOS 2.0",
                category = "Dark Mode",
                description = "Chủ đề chế độ tối Neon tím rực rỡ, giúp tiết kiệm pin màn hình AMOLED, hiệu ứng mở khóa xe đua năng động.",
                version = "1.1.0",
                fileSizeMb = 31.0,
                tags = "DarkMode,Neon,Amoled,HyperOS",
                status = ThemeStatus.APPROVED,
                downloadCount = 3150,
                viewCount = 9800,
                favoriteCount = 780,
                averageRating = 4.7f,
                ratingCount = 56,
                isFeatured = false
            )

            val pendingTheme = ThemeEntity(
                id = 5,
                title = "Aesthetic Anime Sakura V2",
                creatorId = 3,
                creatorName = "NeoCraft UI",
                osCompatibility = "HyperOS 1.0",
                category = "Anime",
                description = "Theme Sakura rực rỡ phong cách Anime Nhật Bản, nhạc chuông tùy biến và ảnh nền khóa hoa đào rơi.",
                version = "1.0.0",
                fileSizeMb = 42.5,
                tags = "Anime,Sakura,Japan,Pink",
                status = ThemeStatus.PENDING,
                downloadCount = 0,
                viewCount = 45,
                favoriteCount = 0,
                averageRating = 0.0f,
                ratingCount = 0,
                isFeatured = false
            )

            themeDao.insertTheme(theme1)
            themeDao.insertTheme(theme2)
            themeDao.insertTheme(theme3)
            themeDao.insertTheme(theme4)
            themeDao.insertTheme(pendingTheme)

            // 3. Initial Comments
            commentDao.insertComment(
                CommentEntity(
                    themeId = 1,
                    userId = 4,
                    userName = "MinhTu_MIUI",
                    rating = 5,
                    commentText = "Theme tuyệt đẹp! Control Center trong suốt cực kỳ đẳng cấp trên HyperOS 2.0 của mình."
                )
            )
            commentDao.insertComment(
                CommentEntity(
                    themeId = 1,
                    userId = 1,
                    userName = "M4X Admin",
                    rating = 5,
                    commentText = "Tác phẩm xuất sắc từ HyperDesign Studio. Đã gắn nhãn Nổi bật."
                )
            )
            commentDao.insertComment(
                CommentEntity(
                    themeId = 2,
                    userId = 4,
                    userName = "MinhTu_MIUI",
                    rating = 5,
                    commentText = "Dynamic Island mượt như iPhone thật luôn, đáng 5 sao!"
                )
            )

            // 4. Initial Tasks
            taskDao.insertTasks(
                listOf(
                    RewardTaskEntity(id = 1, title = "Điểm danh hàng ngày", description = "Đăng nhập ứng dụng M4X Theme mỗi ngày", rewardPoints = 20, taskType = "DAILY_CHECKIN", isCompleted = false),
                    RewardTaskEntity(id = 2, title = "Đánh giá 1 Theme", description = "Đóng góp ý kiến và chấm điểm cho theme bạn đã dùng", rewardPoints = 30, taskType = "RATE_THEME", isCompleted = false),
                    RewardTaskEntity(id = 3, title = "Đăng tải Theme đầu tay", description = "Dành cho Creator: Upload file .mtz chuẩn HyperOS/MIUI", rewardPoints = 100, taskType = "UPLOAD_THEME", isCompleted = false),
                    RewardTaskEntity(id = 4, title = "Chia sẻ ứng dụng", description = "Mời bạn bè Mi Fan tham gia cộng đồng M4X Theme", rewardPoints = 50, taskType = "SHARE", isCompleted = false)
                )
            )

            // 5. Initial Notifications
            notifDao.insertNotification(
                NotificationEntity(
                    targetUserId = 4,
                    title = "Chào mừng tới M4X Theme!",
                    message = "Khám phá hàng nghìn chủ đề HyperOS & MIUI miễn phí, tích điểm đổi quà ngay hôm nay.",
                    type = "SYSTEM"
                )
            )
            notifDao.insertNotification(
                NotificationEntity(
                    targetUserId = 3,
                    title = "Theme đang chờ duyệt",
                    message = "Theme 'Aesthetic Anime Sakura V2' của bạn đã được gửi tới Ban quản trị và đang chờ kiểm duyệt.",
                    type = "APPROVAL"
                )
            )
        }
    }
}
