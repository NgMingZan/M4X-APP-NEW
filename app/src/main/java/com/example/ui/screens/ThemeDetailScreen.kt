package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ThemeEntity
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDetailScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val theme by viewModel.selectedTheme.collectAsState()
    val comments by viewModel.selectedThemeComments.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val followedCreatorIds by viewModel.followedCreatorIds.collectAsState()

    var activePreviewTab by remember { mutableIntStateOf(0) } // 0: Lockscreen, 1: Home Icons, 2: Control Center, 3: System UI
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }

    // Comment input state
    var userRating by remember { mutableIntStateOf(5) }
    var commentInput by remember { mutableStateOf("") }

    // Bug report modal
    var showBugDialog by remember { mutableStateOf(false) }
    var bugTitle by remember { mutableStateOf("") }
    var bugDesc by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    if (theme == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text("Không tìm thấy thông tin theme.", color = Color.White)
        }
        return
    }

    val currentTheme = theme!!
    val isFav = favoriteIds.contains(currentTheme.id)
    val isFollowing = followedCreatorIds.contains(currentTheme.creatorId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTheme.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Trở về", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleFavorite(currentTheme.id) },
                        modifier = Modifier.testTag("detail_fav_button")
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Yêu thích",
                            tint = if (isFav) Color(0xFFEF4444) else Color.White
                        )
                    }
                    IconButton(
                        onClick = { showBugDialog = true },
                        modifier = Modifier.testTag("report_bug_button")
                    ) {
                        Icon(Icons.Outlined.BugReport, contentDescription = "Báo lỗi", tint = Color(0xFFF59E0B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview Image Card Showcase
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_banner_hyperos_1785716083770),
                            contentDescription = "Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xCC0F172A))
                                    )
                                )
                        )

                        // Preview Category Label
                        Surface(
                            color = Color(0xFF06B6D4),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = when (activePreviewTab) {
                                    0 -> "🔒 Giao diện Khóa (Lockscreen)"
                                    1 -> "📱 Icon Bộ ứng dụng"
                                    2 -> "⚙️ Trung tâm điều khiển (Control Center)"
                                    else -> "🎨 System UI & Settings"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        // Preview Selector Tabs
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Lockscreen", "Icons", "Control Center", "System UI").forEachIndexed { index, label ->
                                val isTabSelected = activePreviewTab == index
                                Surface(
                                    onClick = { activePreviewTab = index },
                                    color = if (isTabSelected) Color(0xFF06B6D4) else Color(0x990F172A),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isTabSelected) Color(0xFF38BDF8) else Color(0xFF475569))
                                ) {
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Theme Main Title & Meta Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTheme.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = currentTheme.osCompatibility,
                                            color = Color(0xFF38BDF8),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = Color(0xFFA855F7).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = currentTheme.category,
                                            color = Color(0xFFC084FC),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format("%.1f", currentTheme.averageRating),
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "${currentTheme.ratingCount} lượt đánh giá",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Bar: Version, File Size, Downloads, Views
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Phiên bản", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("v${currentTheme.version}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Dung lượng", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("${currentTheme.fileSizeMb} MB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Lượt tải", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("${currentTheme.downloadCount}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Lượt xem", color = Color(0xFF64748B), fontSize = 10.sp)
                                Text("${currentTheme.viewCount}", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Download Button with progress bar
                        if (isDownloading) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Đang tải file .mtz...", color = Color(0xFF06B6D4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${(downloadProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = Color(0xFF06B6D4),
                                    trackColor = Color(0xFF334155)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isDownloading = true
                                        downloadProgress = 0f
                                        while (downloadProgress < 1f) {
                                            delay(150)
                                            downloadProgress += 0.2f
                                        }
                                        isDownloading = false
                                        viewModel.downloadTheme(currentTheme.id, currentTheme.title)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("download_mtz_button")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tải Theme (.mtz / .zip)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Creator Profile Card with Follow Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFA855F7)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Palette, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(currentTheme.creatorName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Nhà sáng tạo đã được xác minh", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.toggleFollowCreator(currentTheme.creatorId) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isFollowing) Color(0xFF10B981) else Color(0xFF06B6D4)
                            ),
                            border = BorderStroke(1.dp, if (isFollowing) Color(0xFF10B981) else Color(0xFF06B6D4)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("follow_creator_button")
                        ) {
                            Icon(
                                imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFollowing) "Đã theo dõi" else "Theo dõi", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Automated MTZ Integrity Log
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F291E)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF059669))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kiểm tra an toàn hệ thống M4X AutoCheck", color = Color(0xFF34D399), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(currentTheme.autoCheckLog, color = Color(0xFFA7F3D0), fontSize = 11.sp)
                    }
                }
            }

            // Theme Description
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Mô tả chủ đề", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentTheme.description, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 20.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentTheme.tags.split(",").forEach { tag ->
                                Surface(
                                    color = Color(0xFF334155),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("#$tag", color = Color(0xFF38BDF8), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Rating & Add Comment Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Đánh giá & Bình luận", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Star selector (1 to 5)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Chấm điểm: ", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = { userRating = star },
                                    modifier = Modifier.size(32.dp).testTag("star_select_$star")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = "Star $star",
                                        tint = if (star <= userRating) Color(0xFFF59E0B) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Viết cảm nhận của bạn về theme này...", color = Color(0xFF64748B), fontSize = 12.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("comment_input_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                viewModel.addComment(currentTheme.id, userRating, commentInput)
                                commentInput = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                            modifier = Modifier.align(Alignment.End).testTag("submit_comment_button")
                        ) {
                            Text("Gửi bình luận", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // List of User Comments
            items(comments) { comment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF06B6D4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(comment.userName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(comment.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Row {
                                (1..5).forEach { s ->
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = if (s <= comment.rating) Color(0xFFF59E0B) else Color(0xFF475569),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(comment.commentText, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Report Bug Modal
    if (showBugDialog) {
        AlertDialog(
            onDismissRequest = { showBugDialog = false },
            title = { Text("Báo lỗi Theme", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Theme: ${currentTheme.title}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                    OutlinedTextField(
                        value = bugTitle,
                        onValueChange = { bugTitle = it },
                        label = { Text("Tên lỗi (Ví dụ: Lỗi Control Center)") },
                        modifier = Modifier.fillMaxWidth().testTag("bug_title_input")
                    )
                    OutlinedTextField(
                        value = bugDesc,
                        onValueChange = { bugDesc = it },
                        label = { Text("Mô tả chi tiết lỗi và phiên bản OS") },
                        modifier = Modifier.fillMaxWidth().testTag("bug_desc_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.submitBugReport(currentTheme.id, currentTheme.title, bugTitle, bugDesc, currentTheme.osCompatibility)
                        showBugDialog = false
                        bugTitle = ""
                        bugDesc = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    modifier = Modifier.testTag("submit_bug_dialog_button")
                ) {
                    Text("Gửi báo cáo", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBugDialog = false }) {
                    Text("Hủy", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}
