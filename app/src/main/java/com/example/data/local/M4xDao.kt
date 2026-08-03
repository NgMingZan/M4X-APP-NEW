package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserById(userId: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET points = points + :pointsToAdd WHERE id = :userId")
    suspend fun addPoints(userId: Long, pointsToAdd: Int)

    @Query("UPDATE users SET isBanned = :isBanned WHERE id = :userId")
    suspend fun setBannedStatus(userId: Long, isBanned: Boolean)
}

@Dao
interface ThemeDao {
    @Query("SELECT * FROM themes ORDER BY createdTimestamp DESC")
    fun getAllThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE status = 'APPROVED' ORDER BY isFeatured DESC, downloadCount DESC")
    fun getApprovedThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE status = 'PENDING' ORDER BY createdTimestamp ASC")
    fun getPendingThemes(): Flow<List<ThemeEntity>>

    @Query("SELECT * FROM themes WHERE id = :themeId LIMIT 1")
    fun getThemeById(themeId: Long): Flow<ThemeEntity?>

    @Query("SELECT * FROM themes WHERE creatorId = :creatorId ORDER BY createdTimestamp DESC")
    fun getThemesByCreator(creatorId: Long): Flow<List<ThemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeEntity): Long

    @Update
    suspend fun updateTheme(theme: ThemeEntity)

    @Query("DELETE FROM themes WHERE id = :themeId")
    suspend fun deleteTheme(themeId: Long)

    @Query("UPDATE themes SET downloadCount = downloadCount + 1 WHERE id = :themeId")
    suspend fun incrementDownloadCount(themeId: Long)

    @Query("UPDATE themes SET viewCount = viewCount + 1 WHERE id = :themeId")
    suspend fun incrementViewCount(themeId: Long)

    @Query("UPDATE themes SET status = :status, rejectionReason = :reason WHERE id = :themeId")
    suspend fun updateThemeStatus(themeId: Long, status: ThemeStatus, reason: String)

    @Query("UPDATE themes SET averageRating = :newAvg, ratingCount = :newCount WHERE id = :themeId")
    suspend fun updateRating(themeId: Long, newAvg: Float, newCount: Int)

    @Query("UPDATE themes SET isFeatured = :isFeatured WHERE id = :themeId")
    suspend fun setFeatured(themeId: Long, isFeatured: Boolean)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE themeId = :themeId ORDER BY timestamp DESC")
    fun getCommentsByTheme(themeId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT themeId FROM favorites WHERE userId = :userId")
    fun getFavoriteThemeIds(userId: Long): Flow<List<Long>>

    @Query("SELECT * FROM favorites WHERE userId = :userId AND themeId = :themeId LIMIT 1")
    suspend fun getFavorite(userId: Long, themeId: Long): FavoriteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE userId = :userId AND themeId = :themeId")
    suspend fun removeFavorite(userId: Long, themeId: Long)
}

@Dao
interface FollowDao {
    @Query("SELECT creatorId FROM followed_creators WHERE userId = :userId")
    fun getFollowedCreatorIds(userId: Long): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun followCreator(follow: FollowEntity)

    @Query("DELETE FROM followed_creators WHERE userId = :userId AND creatorId = :creatorId")
    suspend fun unfollowCreator(userId: Long, creatorId: Long)
}

@Dao
interface BugReportDao {
    @Query("SELECT * FROM bug_reports ORDER BY timestamp DESC")
    fun getAllBugReports(): Flow<List<BugReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBugReport(report: BugReportEntity): Long

    @Query("UPDATE bug_reports SET status = :status WHERE id = :reportId")
    suspend fun updateStatus(reportId: Long, status: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE targetUserId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE targetUserId = :userId")
    suspend fun markAllAsRead(userId: Long)
}

@Dao
interface RewardTaskDao {
    @Query("SELECT * FROM reward_tasks")
    fun getAllTasks(): Flow<List<RewardTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<RewardTaskEntity>)

    @Query("UPDATE reward_tasks SET isCompleted = 1 WHERE id = :taskId")
    suspend fun completeTask(taskId: Long)
}
