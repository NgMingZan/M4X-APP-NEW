package com.m4xtheme.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.m4xtheme.app.ui.theme.M4XTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { M4XTheme { M4XApp() } }
    }
}

enum class Tab { HOME, QUEST, UPLOAD, WEB, PROFILE, ADMIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun M4XApp() {
    val context = LocalContext.current
    val api = remember { SupabaseApi(context) }
    var session by remember { mutableStateOf(api.restoreSession()) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var config by remember { mutableStateOf(RemoteConfig()) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var message by remember { mutableStateOf<String?>(null) }
    var showAirdropChest by remember { mutableStateOf(false) }
    var claimingAirdrop by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val appScope = rememberCoroutineScope()

    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); message = null } }
    LaunchedEffect(session) {
        session?.let { s ->
            api.profile(s).onSuccess { profile = it }.onFailure { message = it.message }
            api.remoteConfig(s).onSuccess { config = it }
            while (true) {
                delay((45_000L..120_000L).random())
                api.hasActiveAirdrop(s).onSuccess { if (it) showAirdropChest = true }
                while (showAirdropChest) delay(5_000L)
            }
        }
    }

    if (session == null) {
        AuthScreen(api, onSuccess = { api.saveSession(it); session = it }, onMessage = { message = it })
        return
    }

    val isAdmin = profile?.role in setOf("admin", "super_admin")
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Color(0xFF8B5CFF), Color(0xFF15C7E8)))), contentAlignment = Alignment.Center) {
                            Text("M4X", fontWeight = FontWeight.Black, color = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column { Text("M4X Theme", fontWeight = FontWeight.Black); Text("M4X COIN • Online v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                    }
                },
                actions = {
                    if (isAdmin) IconButton(onClick = { tab = Tab.ADMIN }) { Icon(Icons.Default.AdminPanelSettings, "Admin") }
                    IconButton(onClick = { message = "Bạn đang có ${profile?.points ?: 0} M4X COIN" }) { Icon(Icons.Default.Paid, "M4X COIN", tint = Color(0xFFFFC857)) }
                }
            )
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                Nav(tab == Tab.HOME, Icons.Default.Home, "Khám phá") { tab = Tab.HOME }
                Nav(tab == Tab.QUEST, Icons.Default.Map, "Nhiệm vụ") { tab = Tab.QUEST }
                Nav(tab == Tab.UPLOAD, Icons.Default.AddCircle, "Đăng") { tab = Tab.UPLOAD }
                Nav(tab == Tab.WEB, Icons.Default.Public, "M4X WEB") { tab = Tab.WEB }
                Nav(tab == Tab.PROFILE, Icons.Default.Person, "Hồ sơ") { tab = Tab.PROFILE }
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> HomeScreen(api, session!!, config, profile, onMessage = { message = it })
                Tab.QUEST -> QuestHub(api, session!!, profile, onMessage = { message = it })
                Tab.UPLOAD -> UploadScreen(api, session!!, onMessage = { message = it }, onDone = { tab = Tab.PROFILE })
                Tab.WEB -> M4XWebScreen()
                Tab.PROFILE -> ProfileScreen(api, session!!, profile, config, isAdmin, onOpenAdmin = { tab = Tab.ADMIN }, onLogout = { api.signOut(); session = null; profile = null }, onMessage = { message = it })
                Tab.ADMIN -> AdminScreen(api, session!!, profile, onMessage = { message = it })
            }
            if (showAirdropChest) {
                FloatingActionButton(
                    onClick = {
                        if (!claimingAirdrop) {
                            claimingAirdrop = true
                            appScope.launch {
                                api.claimAirdrop(session!!).onSuccess {
                                    message = "🎁 Bạn mở rương nhận được $it M4X COIN!"
                                    api.profile(session!!).onSuccess { profile = it }
                                }.onFailure { message = it.message ?: "Rương đã có người nhận" }
                                showAirdropChest = false
                                claimingAirdrop = false
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                    containerColor = Color(0xFFFFC857)
                ) { Icon(Icons.Default.Inventory2, "Rương Airdrop") }
            }
        }
    }
}

@Composable
private fun RowScope.Nav(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, click: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = click, icon = { Icon(icon, null) }, label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) })
}

@Composable
private fun AuthScreen(api: SupabaseApi, onSuccess: (Session) -> Unit, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF111A31), Color(0xFF070B15))))) {
        LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.Center) {
            item {
                Text("M4X Theme", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text(if (register) "Tạo tài khoản cộng đồng" else "Đăng nhập hệ thống M4X", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(20.dp))
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (register) {
                            OutlinedTextField(username, { username = it.trim() }, label = { Text("Tên đăng nhập") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(displayName, { displayName = it }, label = { Text("Tên hiển thị") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        OutlinedTextField(email, { email = it.trim() }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(enabled = !loading && SupabaseConfig.configured, onClick = {
                            authMessage = null; loading = true
                            scope.launch {
                                val result = if (register) api.signUp(email, password, username, displayName) else api.signIn(email, password)
                                result.onSuccess(onSuccess).onFailure { authMessage = it.message ?: "Có lỗi"; onMessage(authMessage!!) }
                                loading = false
                            }
                        }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp)); Text(if (loading) "Đang xử lý…" else if (register) "Đăng ký" else "Đăng nhập", fontWeight = FontWeight.Bold)
                        }
                        authMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
                TextButton(onClick = { register = !register }, modifier = Modifier.fillMaxWidth()) { Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký") }
            }
        }
    }
}

@Composable
private fun HomeScreen(api: SupabaseApi, session: Session, config: RemoteConfig, profile: Profile?, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        api.approvedThemes(session).onSuccess { themes = it }.onFailure { onMessage(it.message ?: "Lỗi tải theme") }
        api.activeEvents(session).onSuccess { events = it }
    }
    val filtered = themes.filter { (query.isBlank() || it.title.contains(query, true)) && (category == "Tất cả" || it.category.contains(category, true)) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(32.dp)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF5E36FF), Color(0xFF00BFD9)))).padding(22.dp)) {
                    Column {
                        Text("M4X UNIVERSE", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(14.dp)); Text(config.homeBannerTitle, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text(config.homeBannerSubtitle)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            MetricPill(Icons.Default.Paid, "${profile?.points ?: 0} COIN")
                            MetricPill(Icons.Default.Download, "${themes.sumOf { it.downloads }} lượt tải")
                        }
                    }
                }
            }
        }
        if (events.isNotEmpty()) item {
            SectionTitle("Sự kiện đang diễn ra", "Cập nhật online bởi Admin")
            EventBanner(events.first())
        }
        item {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Tìm theme, tác giả, phong cách…") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(22.dp), singleLine = true)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Tất cả", "HyperOS", "MIUI", "Lockscreen", "Icons", "Control Center").forEach { FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) }) }
            }
        }
        item { SectionTitle("Theme nổi bật", "Mua bằng M4X COIN hoặc tải miễn phí") }
        if (filtered.isEmpty()) item { EmptyState("Chưa có theme", "Theme được duyệt sẽ xuất hiện ở đây") }
        else items(filtered, key = { it.id }) { theme ->
            ThemeCard(theme) {
                if ((profile?.points ?: 0) < theme.coinPrice) onMessage("Bạn chưa đủ ${theme.coinPrice} M4X COIN")
                else scope.launch {
                    api.purchaseTheme(session, theme.id).onSuccess {
                        val url = theme.fileUrl.ifBlank { theme.driveUrl }
                        if (url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }.onFailure { onMessage(it.message ?: "Không thể mua theme") }
                }
            }
        }
    }
}

@Composable private fun MetricPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) { Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) { Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(text, fontWeight = FontWeight.Bold) } } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun EventBanner(event: EventItem) { ElevatedCard(shape = RoundedCornerShape(26.dp)) { Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFFF4E8A), Color(0xFF6E48FF)))).padding(20.dp)) { Column { Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(event.description); Spacer(Modifier.height(8.dp)); AssistChip(onClick = {}, label = { Text("${event.startAt.take(10)} → ${event.endAt.take(10)}") }) } } } }

@Composable
private fun ThemeCard(theme: ThemeItem, onBuy: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().height(180.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (theme.previewUrl.isNotBlank()) AsyncImage(theme.previewUrl, theme.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Default.Palette, null, Modifier.size(56.dp).align(Alignment.Center), tint = MaterialTheme.colorScheme.primary)
                Surface(color = Color.Black.copy(alpha = .62f), shape = RoundedCornerShape(bottomEnd = 16.dp), modifier = Modifier.align(Alignment.TopStart)) { Text("${theme.coinPrice} M4X COIN", Modifier.padding(10.dp), fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(theme.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text(theme.description.ifBlank { "Theme cộng đồng M4X" }, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = Color(0xFFFFC857)); Text(" ${"%.1f".format(theme.rating)}")
                    Spacer(Modifier.width(16.dp)); Icon(Icons.Default.Download, null, Modifier.size(18.dp)); Text(" ${theme.downloads}")
                    Spacer(Modifier.weight(1f)); Button(onClick = onBuy, shape = RoundedCornerShape(16.dp)) { Text(if (theme.coinPrice > 0) "Mua" else "Tải") }
                }
            }
        }
    }
}

@Composable
private fun QuestHub(api: SupabaseApi, session: Session, profile: Profile?, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var quests by remember { mutableStateOf<List<QuestItem>>(emptyList()) }
    var claimed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var gift by remember { mutableStateOf("") }
    var leaderboard by remember { mutableStateOf<List<LeaderboardItem>>(emptyList()) }
    fun reloadQuests() { scope.launch { api.claimedQuestIds(session).onSuccess { claimed = it }; api.activeQuests(session).onSuccess { quests = it } } }
    LaunchedEffect(Unit) { reloadQuests(); api.weeklyLeaderboard(session).onSuccess { leaderboard = it } }
    val available = quests.filterNot { it.id in claimed }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Card(shape = RoundedCornerShape(30.dp)) { Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF2B1C5B), Color(0xFF0E6670)))).padding(22.dp)) { Column { Text("M4X QUEST MAP", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("Vượt từng khu vực, mở rương và đánh Boss cuối tuần"); Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress = { .35f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)); Spacer(Modifier.height(8.dp)); Text("Cấp 3 • ${profile?.points ?: 0} M4X COIN") } } } }
        item { SectionTitle("Nhiệm vụ hôm nay", "Nhiệm vụ đã nhận thưởng sẽ tự ẩn") }
        if (available.isEmpty()) item { EmptyState("Đã hoàn thành hết", "Nhiệm vụ mới sẽ được Admin cập nhật online") }
        items(available, key = { it.id }) { q ->
            ElevatedCard(shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.TaskAlt, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(q.title, fontWeight = FontWeight.Bold); Text(q.description, color = MaterialTheme.colorScheme.onSurfaceVariant) }; AssistChip(onClick = { scope.launch { api.claimQuest(session, q.id).onSuccess { claimed = claimed + q.id; onMessage("Nhận ${q.reward} M4X COIN") }.onFailure { reloadQuests(); onMessage(it.message ?: "Không thể nhận") } } }, label = { Text("+${q.reward}") }) } }
        }
        item { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Nhập Giftcode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); OutlinedTextField(gift, { gift = it.uppercase() }, label = { Text("Mã quà tặng") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Button(onClick = { scope.launch { api.redeemGiftCode(session, gift).onSuccess { onMessage("Đã nhận $it M4X COIN"); gift = "" }.onFailure { onMessage(it.message ?: "Giftcode không hợp lệ") } } }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Redeem, null); Text(" Nhận quà") } } } }
        item { SectionTitle("Top thành viên tuần", "Thưởng từ 5.000 đến 50.000 M4X COIN") }
        items(leaderboard.take(5)) { item -> LeaderRow(item) }
        item { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Inventory2, null, tint = Color(0xFFFFC857)); Spacer(Modifier.width(12.dp)); Column { Text("Rương Airdrop ngẫu nhiên", fontWeight = FontWeight.Black); Text("Rương sẽ xuất hiện bất ngờ khi bạn đang sử dụng app") } } } }
    }
}

@Composable private fun LeaderRow(item: LeaderboardItem) { ElevatedCard(shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Text("#${item.rank}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Text(item.displayName, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("${item.score} điểm") } } }

@Composable
private fun UploadScreen(api: SupabaseApi, session: Session, onMessage: (String) -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope(); val context = LocalContext.current
    var fileUri by remember { mutableStateOf<Uri?>(null) }; var previewUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var driveUrl by remember { mutableStateOf("") }; var price by remember { mutableStateOf("0") }; var uploading by remember { mutableStateOf(false) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { fileUri = it }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { previewUris = it.take(5) }
    LazyColumn(Modifier.fillMaxSize().imePadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionTitle("Đăng theme mới", "File lớn có thể dùng Google Drive") }
        item { FormCard("Thông tin theme", Icons.Default.Edit) { OutlinedTextField(title, { title = it }, label = { Text("Tên theme") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(description, { description = it }, label = { Text("Mô tả") }, minLines = 3, modifier = Modifier.fillMaxWidth()); OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Giá M4X COIN") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth()) } }
        item { FormCard("Ảnh xem trước", Icons.Default.PhotoLibrary) { OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Chọn tối đa 5 ảnh (${previewUris.size}/5)") }; Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { previewUris.forEach { AsyncImage(it, null, contentScale = ContentScale.Crop, modifier = Modifier.size(92.dp).clip(RoundedCornerShape(16.dp))) } } } }
        item { FormCard("Nguồn tải", Icons.Default.CloudUpload) { OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) { Text(fileUri?.let { SupabaseApi.fileName(context.contentResolver, it) } ?: "Chọn file .mtz / .zip") }; OutlinedTextField(driveUrl, { driveUrl = it.trim() }, label = { Text("Hoặc link Google Drive") }, modifier = Modifier.fillMaxWidth()) } }
        item { Button(enabled = !uploading && title.isNotBlank() && (fileUri != null || driveUrl.isNotBlank()), onClick = { uploading = true; scope.launch { api.uploadTheme(session, fileUri, previewUris, driveUrl, title, description, "HyperOS", "", "", "", price.toIntOrNull() ?: 0).onSuccess { onMessage("Đã gửi Admin duyệt"); onDone() }.onFailure { onMessage(it.message ?: "Lỗi upload") }; uploading = false } }, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(18.dp)) { Text(if (uploading) "Đang tải…" else "Gửi duyệt Admin", fontWeight = FontWeight.Bold) } }
    }
}

@Composable private fun FormCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }; content() } } }

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun M4XWebScreen() {
    var input by remember { mutableStateOf("https://") }; var current by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(input, { input = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Dán bất kỳ link web") }); Spacer(Modifier.width(8.dp)); IconButton(onClick = { current = if (input.startsWith("http")) input else "https://$input" }) { Icon(Icons.Default.ArrowForward, null) } }
        if (current.isBlank()) EmptyState("M4X WEB", "Dán link để mở trang web ngay trong app") else AndroidView(factory = { ctx -> WebView(ctx).apply { settings.javaScriptEnabled = true; settings.domStorageEnabled = true; settings.allowFileAccess = true; webViewClient = WebViewClient(); webChromeClient = WebChromeClient(); loadUrl(current) } }, update = { if (it.url != current) it.loadUrl(current) }, modifier = Modifier.fillMaxSize())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScreen(api: SupabaseApi, session: Session, profile: Profile?, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope(); var selected by remember { mutableIntStateOf(0) }; var pending by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }; var allThemes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }; var users by remember { mutableStateOf<List<Profile>>(emptyList()) }; var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    fun reload() { scope.launch { api.pendingThemes(session).onSuccess { pending = it }; api.allThemes(session).onSuccess { allThemes = it }; api.users(session).onSuccess { users = it }; api.allEvents(session).onSuccess { events = it } } }
    LaunchedEffect(Unit) { reload() }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { SectionTitle("Trung tâm điều hành", "Quản lý toàn bộ M4X Universe") }
            item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { AdminMetric("Chờ duyệt", pending.size, Icons.Default.PendingActions, Modifier.weight(1f)); AdminMetric("Người dùng", users.size, Icons.Default.Groups, Modifier.weight(1f)); AdminMetric("Sự kiện", events.size, Icons.Default.Celebration, Modifier.weight(1f)) } }
            item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Tổng quan", "Theme", "Sự kiện", "Giftcode", "Người dùng").forEachIndexed { i, s -> FilterChip(selected = selected == i, onClick = { selected = i }, label = { Text(s) }) } } }
            when (selected) {
                0 -> item { FormCard("Công cụ nhanh", Icons.Default.Dashboard) { AdminAction("Phát hành Airdrop", Icons.Default.RocketLaunch) { scope.launch { api.createAirdrop(session).onSuccess { onMessage("Đã phát hành Airdrop") }.onFailure { onMessage(it.message ?: "Lỗi") } } }; AdminAction("Tuần sinh nhật Admin 01/08–07/08", Icons.Default.Cake) { scope.launch { api.publishBirthdayWeek(session).onSuccess { onMessage("Đã phát hành tuần sinh nhật") }.onFailure { onMessage(it.message ?: "Lỗi") } } }; AdminAction("Mở Boss cộng đồng", Icons.Default.SportsEsports) { onMessage("Boss cộng đồng đã được xếp lịch online") } } }
                1 -> if (allThemes.isEmpty()) item { EmptyState("Chưa có theme", "Theme người dùng đăng sẽ xuất hiện tại đây") } else items(allThemes, key = { it.id }) { t -> ReviewCard(t, onApprove = { scope.launch { api.reviewTheme(session, t.id, true).onSuccess { reload() } } }, onReject = { scope.launch { api.reviewTheme(session, t.id, false, "Cần bổ sung nội dung").onSuccess { reload() } } }, onSave = { title, desc, drive, price, status, previewUris -> scope.launch { api.updateThemeByAdmin(session, t.id, title, desc, drive, price, status, previewUris).onSuccess { onMessage("Đã cập nhật theme online"); reload() }.onFailure { onMessage(it.message ?: "Không thể sửa theme") } } }) }
                2 -> { item { AdminCreateEvent(api, session, onMessage) }; items(events) { EventBanner(it) } }
                3 -> item { AdminGiftCode(api, session, onMessage) }
                4 -> items(users, key = { it.id }) { u -> UserAdminRow(u, profile?.role == "super_admin") { scope.launch { api.setRole(session, u.id, if (u.role == "admin") "user" else "admin").onSuccess { reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } } }
            }
        }
    }
}

@Composable private fun AdminMetric(label: String, value: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) { ElevatedCard(modifier, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(14.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, style = MaterialTheme.typography.labelMedium) } } }
@Composable private fun AdminAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) { ListItem(headlineContent = { Text(title, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(icon, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable(onClick = click)) }
@Composable
private fun ReviewCard(
    t: ThemeItem,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSave: (String, String, String, Int, String, List<Uri>) -> Unit
) {
    var editing by remember { mutableStateOf(false) }
    var newPreviewUris by remember(t.id) { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        newPreviewUris = it.take(5)
    }

    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column {
            if (t.previewUrl.isNotBlank()) {
                AsyncImage(
                    t.previewUrl,
                    null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                )
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${t.status.uppercase()} • ${t.coinPrice} M4X COIN")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApprove, modifier = Modifier.weight(1f)) { Text("Duyệt") }
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) { Text("Từ chối") }
                    IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, "Sửa") }
                }
            }
        }
    }

    if (editing) {
        var title by remember(t.id) { mutableStateOf(t.title) }
        var desc by remember(t.id) { mutableStateOf(t.description) }
        var drive by remember(t.id) { mutableStateOf(t.driveUrl) }
        var price by remember(t.id) { mutableStateOf(t.coinPrice.toString()) }
        var status by remember(t.id) { mutableStateOf(t.status) }

        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Sửa theme") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(title, { title = it }, label = { Text("Tên theme") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Mô tả") })
                    OutlinedTextField(drive, { drive = it }, label = { Text("Link Drive") })
                    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Giá M4X COIN") })

                    Text("Ảnh hiện tại", fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentImages = if (t.previewUrls.isNotEmpty()) t.previewUrls else listOfNotNull(t.previewUrl.takeIf { it.isNotBlank() })
                        currentImages.forEach { url ->
                            AsyncImage(
                                url,
                                null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp))
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null)
                        Text(" Thay ảnh xem trước (${newPreviewUris.size}/5)")
                    }
                    if (newPreviewUris.isNotEmpty()) {
                        Text("Ảnh mới sẽ thay toàn bộ ảnh cũ", color = MaterialTheme.colorScheme.secondary)
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            newPreviewUris.forEach { uri ->
                                AsyncImage(
                                    uri,
                                    null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp))
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("pending", "approved", "rejected").forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(title, desc, drive, price.toIntOrNull() ?: 0, status, newPreviewUris)
                    newPreviewUris = emptyList()
                    editing = false
                }) { Text("Lưu online") }
            },
            dismissButton = {
                TextButton(onClick = { newPreviewUris = emptyList(); editing = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
private fun AdminCreateEvent(api: SupabaseApi, session: Session, onMessage: (String) -> Unit) { val scope = rememberCoroutineScope(); var title by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; FormCard("Phát hành sự kiện online", Icons.Default.Celebration) { OutlinedTextField(title, { title = it }, label = { Text("Tên sự kiện") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(desc, { desc = it }, label = { Text("Nội dung") }, modifier = Modifier.fillMaxWidth()); Button(onClick = { scope.launch { api.createEvent(session, title, desc).onSuccess { onMessage("Đã phát hành sự kiện") }.onFailure { onMessage(it.message ?: "Lỗi") } } }, modifier = Modifier.fillMaxWidth()) { Text("Phát hành ngay") } } }
@Composable
private fun AdminGiftCode(api: SupabaseApi, session: Session, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf("408") }
    var maxUses by remember { mutableStateOf("100") }
    var validHours by remember { mutableStateOf("24") }
    var loading by remember { mutableStateOf(false) }

    FormCard("Tạo Giftcode", Icons.Default.Redeem) {
        Text("Thiết lập phần thưởng, số lượt được nhập và thời gian hết hạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' } },
            label = { Text("Mã Giftcode") },
            supportingText = { Text("Ví dụ: M4X-408") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = reward,
            onValueChange = { reward = it.filter(Char::isDigit) },
            label = { Text("M4X COIN nhận được") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = maxUses,
            onValueChange = { maxUses = it.filter(Char::isDigit) },
            label = { Text("Giới hạn số lượt nhập") },
            supportingText = { Text("Mỗi tài khoản chỉ nhận mã một lần") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = validHours,
            onValueChange = { validHours = it.filter(Char::isDigit) },
            label = { Text("Thời hạn sử dụng (giờ)") },
            supportingText = { Text("24 giờ = 1 ngày, 168 giờ = 7 ngày") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            enabled = !loading,
            onClick = {
                scope.launch {
                    loading = true
                    api.createGiftCode(
                        session = session,
                        code = code,
                        reward = reward.toIntOrNull() ?: 0,
                        maxUses = maxUses.toIntOrNull() ?: 0,
                        validHours = validHours.toIntOrNull() ?: 0
                    ).onSuccess {
                        onMessage("Đã tạo Giftcode $code • tối đa $maxUses lượt • hiệu lực $validHours giờ")
                        code = ""
                    }.onFailure { onMessage(it.message ?: "Không thể tạo Giftcode") }
                    loading = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Redeem, null)
            Text(if (loading) " Đang tạo..." else " Tạo Giftcode")
        }
    }
}
@Composable private fun UserAdminRow(u: Profile, canEdit: Boolean, click: () -> Unit) { ElevatedCard(shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text(u.displayName.take(1).uppercase().ifBlank { "M" }, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(u.displayName.ifBlank { u.username }, fontWeight = FontWeight.Bold); Text("@${u.username} • ${u.role} • ${u.points} coin", color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (canEdit && u.role != "super_admin") TextButton(onClick = click) { Text(if (u.role == "admin") "Hạ quyền" else "Lên Admin") } } } }

@Composable
private fun ProfileScreen(api: SupabaseApi, session: Session, profile: Profile?, config: RemoteConfig, isAdmin: Boolean, onOpenAdmin: () -> Unit, onLogout: () -> Unit, onMessage: (String) -> Unit) {
    var mine by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }; var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    LaunchedEffect(Unit) { api.myThemes(session).onSuccess { mine = it }; api.inventory(session).onSuccess { inventory = it } }
    val downloads = mine.sumOf { it.downloads }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Card(shape = RoundedCornerShape(32.dp)) { Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF4A247B), Color(0xFF075D72)))).padding(22.dp)) { Column { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(88.dp).clip(CircleShape).background(Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(52.dp)) }; Spacer(Modifier.width(16.dp)); Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(profile?.displayName?.ifBlank { "M4X Member" } ?: "M4X Member", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Spacer(Modifier.width(6.dp)); Icon(Icons.Default.Verified, null, tint = Color(0xFF32D6FF)) }; Text("@${profile?.username}"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { AssistChip(onClick = {}, label = { Text("VIP 1") }); AssistChip(onClick = {}, label = { Text("LV.3") }); AssistChip(onClick = {}, label = { Text(profile?.role ?: "user") }) } } }; Spacer(Modifier.height(18.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ProfileStat("M4X COIN", "${profile?.points ?: 0}", Modifier.weight(1f)); ProfileStat("Theme", mine.size.toString(), Modifier.weight(1f)); ProfileStat("Lượt tải", downloads.toString(), Modifier.weight(1f)) } } } } }
        item { SectionTitle("Kho vật phẩm cá nhân", "Khung avatar, màu tên, hiệu ứng, VIP và vật phẩm đã mua") }
        item { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) { if (inventory.isEmpty()) repeat(4) { InventoryCard("Vật phẩm ${it + 1}", "Chưa sở hữu") } else inventory.forEach { InventoryCard(it.name, it.type) } } }
        item { FormCard("Thành tích", Icons.Default.EmojiEvents) { Text("Theme đã đăng: ${mine.size}"); Text("Tổng lượt tải: $downloads"); Text("Huy hiệu: Khách mời sinh nhật ADMIN"); Text("Xếp hạng tuần: Đang cập nhật online") } }
        if (isAdmin) item { Button(onClick = onOpenAdmin, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(18.dp)) { Icon(Icons.Default.AdminPanelSettings, null); Text(" Mở trung tâm Admin") } }
        item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Default.Logout, null); Text(" Đăng xuất") } }
    }
}
@Composable private fun ProfileStat(label: String, value: String, modifier: Modifier) { Surface(modifier, color = Color.White.copy(alpha = .1f), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(label, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun InventoryCard(name: String, type: String) { ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.width(150.dp)) { Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(8.dp)); Text(name, fontWeight = FontWeight.Bold, maxLines = 1); Text(type, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun EmptyState(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().padding(vertical = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Inbox, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(14.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
