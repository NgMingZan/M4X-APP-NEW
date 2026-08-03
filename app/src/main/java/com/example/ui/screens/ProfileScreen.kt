package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ThemeStatus
import com.example.data.local.UserRole
import com.example.ui.components.ThemeCardItem
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenThemeDetail: (Long) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val myThemes by viewModel.myThemes.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val approvedThemes by viewModel.filteredApprovedThemes.collectAsState()
    val isOtaChecking by viewModel.isOtaChecking.collectAsState()
    val otaMessage by viewModel.otaVersionMessage.collectAsState()

    var activeProfileTab by remember { mutableIntStateOf(0) } // 0: My Submissions, 1: Favorites, 2: OTA Settings

    val favoriteThemes = approvedThemes.filter { favoriteIds.contains(it.id) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // User Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    when (currentUser?.role) {
                                        UserRole.ADMIN -> Color(0xFFEF4444)
                                        UserRole.CREATOR -> Color(0xFFA855F7)
                                        else -> Color(0xFF06B6D4)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser?.username?.take(1) ?: "U",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(currentUser?.username ?: "MinhTu_MIUI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(currentUser?.email ?: "", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = when (currentUser?.role) {
                                    UserRole.ADMIN -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                    UserRole.CREATOR -> Color(0xFFA855F7).copy(alpha = 0.2f)
                                    else -> Color(0xFF10B981).copy(alpha = 0.2f)
                                },
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Vai trò: ${currentUser?.role}",
                                    color = when (currentUser?.role) {
                                        UserRole.ADMIN -> Color(0xFFFCA5A5)
                                        UserRole.CREATOR -> Color(0xFFE9D5FF)
                                        else -> Color(0xFFA7F3D0)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Points Badge
                    Surface(
                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Stars, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${currentUser?.points ?: 0} Pts", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(currentUser?.bio ?: "", color = Color(0xFFCBD5E1), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Profile Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("🎨 Theme của tôi (${myThemes.size})", "❤️ Yêu thích (${favoriteThemes.size})", "⚡ Cập nhật OTA").forEachIndexed { idx, label ->
                val isSel = activeProfileTab == idx
                Surface(
                    onClick = { activeProfileTab = idx },
                    color = if (isSel) Color(0xFF06B6D4) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("profile_tab_$idx")
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (activeProfileTab == 0) {
                // My Submissions
                if (myThemes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Bạn chưa tải lên theme nào. Hãy bắt đầu sáng tạo!", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(myThemes) { theme ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(theme.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                                    Surface(
                                        color = when (theme.status) {
                                            ThemeStatus.APPROVED -> Color(0xFF10B981)
                                            ThemeStatus.PENDING -> Color(0xFFF59E0B)
                                            ThemeStatus.REJECTED -> Color(0xFFEF4444)
                                        },
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = when (theme.status) {
                                                ThemeStatus.APPROVED -> "ĐÃ DUYỆT"
                                                ThemeStatus.PENDING -> "ĐANG CHỜ DUYỆT"
                                                ThemeStatus.REJECTED -> "TỪ CHỐI"
                                            },
                                            color = Color.Black,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tương thích: ${theme.osCompatibility} • Phân loại: ${theme.category}", color = Color(0xFF38BDF8), fontSize = 11.sp)

                                if (theme.status == ThemeStatus.REJECTED && theme.rejectionReason.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Lý do từ chối: ${theme.rejectionReason}", color = Color(0xFFFCA5A5), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else if (activeProfileTab == 1) {
                // Favorites
                if (favoriteThemes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Chưa có theme nào trong danh sách Yêu thích.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(favoriteThemes) { theme ->
                        ThemeCardItem(
                            theme = theme,
                            isFavorite = true,
                            onClick = { onOpenThemeDetail(theme.id) },
                            onFavoriteToggle = { viewModel.toggleFavorite(theme.id) }
                        )
                    }
                }
            } else {
                // OTA Update Checker Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFF06B6D4))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.SystemUpdate, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Cập Nhật Nội Dung & OTA In-App", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Cập nhật giao diện mới không cần cài lại APK", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (otaMessage != null) {
                                Surface(
                                    color = Color(0xFF0F291E),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(otaMessage!!, color = Color(0xFF34D399), fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Button(
                                onClick = { viewModel.checkOtaUpdate() },
                                enabled = !isOtaChecking,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("ota_check_now_button")
                            ) {
                                if (isOtaChecking) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Đang kết nối OTA Server M4X...", fontSize = 13.sp)
                                } else {
                                    Text("Kiểm tra bản cập nhật OTA", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
