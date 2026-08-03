package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    USER, CREATOR, ADMIN
}

enum class ThemeStatus {
    PENDING, APPROVED, REJECTED
}

enum class OsCompatibility {
    HYPEROS_2, HYPEROS_1, MIUI_14, MIUI_13
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val email: String,
    val role: UserRole = UserRole.USER,
    val avatarUrl: String = "",
    val points: Int = 120,
    val bio: String = "Người yêu thích giao diện HyperOS & MIUI",
    val isBanned: Boolean = false,
    val joinedDate: String = "2026-08-01"
)

@Entity(tableName = "themes")
data class ThemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val creatorId: Long,
    val creatorName: String,
    val osCompatibility: String, // e.g. "HyperOS 2.0"
    val category: String, // e.g. "Minimal", "Cyberpunk", "iOS Style", "Dark Mode"
    val description: String,
    val version: String = "1.0.0",
    val fileSizeMb: Double = 24.5,
    val previewImageRes: String = "", // resource or asset name
    val lockscreenImageRes: String = "",
    val iconsImageRes: String = "",
    val controlCenterImageRes: String = "",
    val tags: String = "HyperOS,Dark,Minimal",
    val status: ThemeStatus = ThemeStatus.APPROVED,
    val rejectionReason: String = "",
    val downloadCount: Int = 0,
    val viewCount: Int = 0,
    val favoriteCount: Int = 0,
    val averageRating: Float = 4.8f,
    val ratingCount: Int = 12,
    val isFeatured: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isAutoCheckPassed: Boolean = true,
    val autoCheckLog: String = "Passed: ZIP structure, manifest.xml, 108dp icons verified."
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val themeId: Long,
    val userId: Long,
    val userName: String,
    val userAvatar: String = "",
    val rating: Int = 5,
    val commentText: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val themeId: Long
)

@Entity(tableName = "followed_creators")
data class FollowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val creatorId: Long
)

@Entity(tableName = "bug_reports")
data class BugReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val themeId: Long,
    val themeTitle: String,
    val reporterId: Long,
    val reporterName: String,
    val bugTitle: String,
    val description: String,
    val osVersion: String = "HyperOS 2.0",
    val status: String = "OPEN", // "OPEN", "RESOLVED"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val targetUserId: Long,
    val title: String,
    val message: String,
    val type: String = "SYSTEM", // "APPROVAL", "REJECTED", "SYSTEM", "UPDATE"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "reward_tasks")
data class RewardTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val rewardPoints: Int,
    val taskType: String, // "DAILY_CHECKIN", "UPLOAD_THEME", "RATE_THEME", "SHARE"
    val isCompleted: Boolean = false
)
