package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Announcement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ThemeEntity
import com.example.data.local.UserRole
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onOpenThemeDetail: (Long) -> Unit
) {
    val analytics by viewModel.adminAnalytics.collectAsState()
    val pendingThemes by viewModel.pendingThemes.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val filteredThemes by viewModel.filteredApprovedThemes.collectAsState()

    var activeAdminTab by remember { mutableIntStateOf(0) } // 0: Overview & Queue, 1: User Management, 2: Theme Catalog

    // Reject Dialog modal
    var showRejectDialog by remember { mutableStateOf(false) }
    var selectedThemeToReject by remember { mutableStateOf<ThemeEntity?>(null) }
    var rejectReason by remember { mutableStateOf("") }

    // Broadcast Notif modal
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Bảng Điều Khiển Admin", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Quản lý kiểm duyệt Theme & hệ thống M4X Store", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }

            IconButton(
                onClick = { showBroadcastDialog = true },
                modifier = Modifier.testTag("broadcast_notif_button")
            ) {
                Icon(Icons.Outlined.Announcement, contentDescription = "Gửi thông báo", tint = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Admin Navigation Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("📊 Bảng chỉ số", "👥 Người dùng (${allUsers.size})", "📁 Theme (${filteredThemes.size})").forEachIndexed { idx, label ->
                val isSel = activeAdminTab == idx
                Surface(
                    onClick = { activeAdminTab = idx },
                    color = if (isSel) Color(0xFFEF4444) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("admin_tab_$idx")
                ) {
                    Text(
                        text = label,
                        color = Color.White,
                        fontSize = 12.sp,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeAdminTab == 0) {
                // Overview & Stats Cards
                item {
                    val data = analytics
                    if (data != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // 2x3 Grid of Metric Cards
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricCard("Tổng người dùng", "${data.totalUsers}", Icons.Filled.People, Color(0xFF06B6D4), Modifier.weight(1f))
                                MetricCard("Tổng số Theme", "${data.totalThemes}", Icons.Filled.Palette, Color(0xFFA855F7), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricCard("Theme chờ duyệt", "${data.pendingCount}", Icons.Filled.HourglassTop, Color(0xFFF59E0B), Modifier.weight(1f))
                                MetricCard("Lượt tải xuống", "${data.totalDownloads}", Icons.Filled.Download, Color(0xFF10B981), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricCard("Tổng lượt xem", "${data.totalViews}", Icons.Filled.Visibility, Color(0xFF38BDF8), Modifier.weight(1f))
                                MetricCard("Đánh giá TB", "${data.averageRating}★", Icons.Filled.Star, Color(0xFFEAB308), Modifier.weight(1f))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Interactive Analytics Bar Chart
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Biểu đồ Thống kê Lượt Tải & Xem theo Ngày", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        data.dailyStats.forEach { stat ->
                                            val maxVal = 4500f
                                            val hPercent = (stat.views / maxVal).coerceIn(0.15f, 1f)

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(18.dp)
                                                        .fillMaxHeight(hPercent)
                                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
                                                            )
                                                        )
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(stat.dayLabel, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Queue: Pending Theme Approvals
                item {
                    Text("Theme Đang Chờ Duyệt (${pendingThemes.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                if (pendingThemes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("Không có theme nào đang chờ duyệt. Mọi thứ đã sạch đĩa! 🎉", color = Color(0xFF10B981), fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(pendingThemes) { theme ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(theme.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Text("Người đăng: ${theme.creatorName} • ${theme.osCompatibility}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                    }

                                    Surface(
                                        color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("CHỜ DUYỆT", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(theme.description, color = Color(0xFFCBD5E1), fontSize = 12.sp, maxLines = 2)

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(theme.autoCheckLog, color = Color(0xFF34D399), fontSize = 10.sp, modifier = Modifier.padding(8.dp))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { onOpenThemeDetail(theme.id) }) {
                                        Text("Xem trước", color = Color(0xFF38BDF8), fontSize = 12.sp)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = {
                                            selectedThemeToReject = theme
                                            showRejectDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("reject_theme_${theme.id}")
                                    ) {
                                        Text("Từ chối", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Button(
                                        onClick = { viewModel.approveTheme(theme.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("approve_theme_${theme.id}")
                                    ) {
                                        Text("Phê duyệt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (activeAdminTab == 1) {
                // User Management
                items(allUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(
                                        when (user.role) {
                                            UserRole.ADMIN -> Color(0xFFEF4444)
                                            UserRole.CREATOR -> Color(0xFFA855F7)
                                            else -> Color(0xFF06B6D4)
                                        }
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(user.username.take(1), color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(user.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("${user.email} • ${user.role}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                }
                            }

                            if (user.role != UserRole.ADMIN) {
                                Button(
                                    onClick = { viewModel.banUser(user.id, !user.isBanned) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (user.isBanned) Color(0xFF10B981) else Color(0xFFDC2626)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(if (user.isBanned) "Mở khóa" else "Khóa Banned", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                // Theme Catalog Management
                items(filteredThemes) { theme ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(theme.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Bởi: ${theme.creatorName} • ${theme.osCompatibility}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }

                            Row {
                                IconButton(
                                    onClick = { viewModel.toggleFeatured(theme.id, !theme.isFeatured) }
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = "Feature", tint = if (theme.isFeatured) Color(0xFFF59E0B) else Color(0xFF475569))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteTheme(theme.id) }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Rejection Reason Modal
    if (showRejectDialog && selectedThemeToReject != null) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            title = { Text("Từ chối Theme", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme: ${selectedThemeToReject?.title}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Lý do từ chối (Gửi thông báo cho Creator)") },
                        modifier = Modifier.fillMaxWidth().testTag("reject_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectTheme(selectedThemeToReject!!.id, rejectReason)
                        showRejectDialog = false
                        rejectReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_reject_button")
                ) {
                    Text("Từ chối ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRejectDialog = false }) {
                    Text("Hủy", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    // Broadcast Notification Modal
    if (showBroadcastDialog) {
        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Gửi Thông Báo Hệ Thống", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Tiêu đề thông báo") },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_title_input")
                    )
                    OutlinedTextField(
                        value = broadcastMsg,
                        onValueChange = { broadcastMsg = it },
                        label = { Text("Nội dung thông báo toàn bộ người dùng") },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_msg_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendBroadcastNotif(broadcastTitle, broadcastMsg)
                        showBroadcastDialog = false
                        broadcastTitle = ""
                        broadcastMsg = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    modifier = Modifier.testTag("send_broadcast_button")
                ) {
                    Text("Gửi ngay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) {
                    Text("Hủy", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp)
                Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
