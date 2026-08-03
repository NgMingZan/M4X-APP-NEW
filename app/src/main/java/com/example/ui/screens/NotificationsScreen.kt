package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: MainViewModel
) {
    val notifications by viewModel.notifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Thông báo hệ thống", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = { viewModel.markNotificationsRead() },
                modifier = Modifier.testTag("mark_notifs_read_button")
            ) {
                Icon(Icons.Outlined.MarkEmailRead, contentDescription = "Đã đọc tất cả", tint = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có thông báo mới.", color = Color(0xFF94A3B8))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications) { notif ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!notif.isRead) Color(0xFF1E293B) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, if (!notif.isRead) Color(0xFF06B6D4) else Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(notif.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Surface(
                                    color = when (notif.type) {
                                        "APPROVAL" -> Color(0xFF10B981)
                                        "REJECTED" -> Color(0xFFEF4444)
                                        else -> Color(0xFF06B6D4)
                                    }.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(notif.type, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(notif.message, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
