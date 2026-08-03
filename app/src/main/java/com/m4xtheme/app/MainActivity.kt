package com.m4xtheme.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import coil.compose.AsyncImage
import com.m4xtheme.app.ui.theme.M4XTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { M4XTheme { M4XApp() } }
    }
}

enum class Tab { HOME, UPLOAD, MY_THEMES, ADMIN, PROFILE }

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
    val snack = remember { SnackbarHostState() }

    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); message = null } }
    LaunchedEffect(session) {
        session?.let { s ->
            api.profile(s).onSuccess { profile = it }.onFailure { message = it.message }
            api.remoteConfig(s).onSuccess { config = it }
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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("M4X Theme", fontWeight = FontWeight.ExtraBold)
                        Text("HyperOS Community • v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    if (config.latestVersionCode > BuildConfig.VERSION_CODE) IconButton(onClick = {
                        if (config.updateUrl.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(config.updateUrl)))
                    }) { Icon(Icons.Default.SystemUpdate, "Cập nhật", tint = MaterialTheme.colorScheme.tertiary) }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 10.dp) {
                NavigationBar(windowInsets = WindowInsets.navigationBars) {
                    NavItem(tab == Tab.HOME, Icons.Default.Explore, "Khám phá") { tab = Tab.HOME }
                    NavItem(tab == Tab.UPLOAD, Icons.Default.CloudUpload, "Đăng") { tab = Tab.UPLOAD }
                    NavItem(tab == Tab.MY_THEMES, Icons.Default.Inventory2, "Kho") { tab = Tab.MY_THEMES }
                    if (isAdmin) NavItem(tab == Tab.ADMIN, Icons.Default.AdminPanelSettings, "Admin") { tab = Tab.ADMIN }
                    NavItem(tab == Tab.PROFILE, Icons.Default.Person, "Hồ sơ") { tab = Tab.PROFILE }
                }
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> HomeScreen(api, session!!, config, onMessage = { message = it })
                Tab.UPLOAD -> UploadScreen(api, session!!, onMessage = { message = it }, onDone = { tab = Tab.MY_THEMES })
                Tab.MY_THEMES -> MyThemesScreen(api, session!!, onMessage = { message = it })
                Tab.ADMIN -> AdminScreen(api, session!!, profile, onMessage = { message = it })
                Tab.PROFILE -> ProfileScreen(api, session!!, profile, config, onLogout = { api.signOut(); session = null; profile = null }, onMessage = { message = it })
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, click: () -> Unit) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().imePadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(76.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Spacer(Modifier.height(20.dp))
                Text("M4X Theme", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text(if (register) "Tạo hồ sơ nhà sáng tạo" else "Đăng nhập cộng đồng HyperOS & MIUI", color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(24.dp))
                ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (register) {
                            OutlinedTextField(username, { username = it.trim() }, label = { Text("Tên đăng nhập") }, leadingIcon = { Icon(Icons.Default.AlternateEmail, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(displayName, { displayName = it }, label = { Text("Tên hiển thị") }, leadingIcon = { Icon(Icons.Default.Badge, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedTextField(email, { email = it.trim() }, label = { Text("Email") }, leadingIcon = { Icon(Icons.Default.Email, null) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, leadingIcon = { Icon(Icons.Default.Lock, null) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                        Button(
                            enabled = !loading && SupabaseConfig.configured,
                            onClick = {
                                authMessage = null; loading = true
                                scope.launch {
                                    val result = if (register) api.signUp(email, password, username, displayName) else api.signIn(email, password)
                                    result.onSuccess(onSuccess).onFailure { authMessage = it.message ?: "Có lỗi xảy ra"; onMessage(authMessage!!) }
                                    loading = false
                                }
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            if (loading) Spacer(Modifier.width(10.dp))
                            Text(if (loading) "Đang kết nối…" else if (register) "Đăng ký" else "Đăng nhập", fontWeight = FontWeight.Bold)
                        }
                        authMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    }
                }
                TextButton(onClick = { register = !register }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký")
                }
                if (!SupabaseConfig.configured) Text("Chưa cấu hình Supabase trong GitHub Secrets", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun HomeScreen(api: SupabaseApi, session: Session, config: RemoteConfig, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    var loading by remember { mutableStateOf(true) }
    val cats = listOf("Tất cả", "HyperOS", "MIUI", "Lockscreen", "Icons", "Control Center")

    LaunchedEffect(Unit) {
        api.approvedThemes(session).onSuccess { themes = it }.onFailure { onMessage(it.message ?: "Không tải được theme") }
        loading = false
    }

    val filtered = themes.filter {
        (query.isBlank() || it.title.contains(query, true) || it.description.contains(query, true)) &&
            (category == "Tất cả" || it.category.contains(category, true) || it.osVersion.contains(category, true))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF5B2EFF), Color(0xFF04B8D4)))).padding(24.dp)) {
                    Column {
                        AssistChip(onClick = {}, label = { Text("M4X ONLINE") }, leadingIcon = { Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp)) })
                        Spacer(Modifier.height(16.dp))
                        Text(config.homeBannerTitle, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text(config.homeBannerSubtitle, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            StatPill(Icons.Default.Download, "${themes.sumOf { it.downloads }} lượt tải")
                            StatPill(Icons.Default.Collections, "${themes.size} theme")
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Tìm theme, tác giả, phong cách…") },
                shape = RoundedCornerShape(22.dp)
            )
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cats.forEach { FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) }) }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Theme nổi bật", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Đã được Admin kiểm duyệt", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.secondary)
            }
        }
        if (loading) items(3) { ThemeSkeleton() }
        else if (filtered.isEmpty()) item { EmptyState(Icons.Default.Inbox, "Chưa có theme phù hợp", "Hãy thử từ khóa hoặc bộ lọc khác.") }
        else items(filtered, key = { it.id }) { theme ->
            ThemeCard(theme, onDownload = {
                val url = theme.fileUrl.ifBlank { theme.driveUrl }
                if (url.isBlank()) onMessage("Theme chưa có link tải") else {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    scope.launch { api.incrementDownload(session, theme.id) }
                }
            })
        }
    }
}

@Composable
private fun StatPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Surface(color = Color.White.copy(alpha = .16f), shape = RoundedCornerShape(50)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ThemeCard(theme: ThemeItem, onDownload: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().height(190.dp).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (theme.previewUrl.isNotBlank()) AsyncImage(model = theme.previewUrl, contentDescription = theme.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Palette, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary) }
                Surface(shape = RoundedCornerShape(bottomEnd = 18.dp), color = Color.Black.copy(alpha = .6f), modifier = Modifier.align(Alignment.TopStart)) {
                    Text(theme.osVersion.ifBlank { "HyperOS / MIUI" }, Modifier.padding(horizontal = 12.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(theme.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(theme.description.ifBlank { "Theme cộng đồng M4X" }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Text(" ${"%.1f".format(theme.rating)}")
                    Spacer(Modifier.width(16.dp))
                    Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                    Text(" ${theme.downloads}")
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = onDownload, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Tải") }
                }
            }
        }
    }
}

@Composable
private fun UploadScreen(api: SupabaseApi, session: Session, onMessage: (String) -> Unit, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var previewUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("HyperOS") }
    var osVersion by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var driveUrl by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var uploading by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { fileUri = it }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { previewUris = it.take(5) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HeaderBlock("Đăng theme mới", "Gửi theme để Admin kiểm tra trước khi công khai", Icons.Default.CloudUpload)
        }
        item {
            SectionCard("Thông tin theme", Icons.Default.Edit) {
                OutlinedTextField(title, { title = it }, label = { Text("Tên theme") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Mô tả chi tiết") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("HyperOS", "MIUI", "Khác").forEach { FilterChip(selected = category == it, onClick = { category = it }, label = { Text(it) }) }
                }
                OutlinedTextField(osVersion, { osVersion = it }, label = { Text("Phiên bản hỗ trợ") }, placeholder = { Text("Ví dụ: HyperOS 2/3, MIUI 14") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, { tags = it }, label = { Text("Thẻ") }, placeholder = { Text("Dark, iOS, Minimal…") }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            SectionCard("Ảnh xem trước", Icons.Default.PhotoLibrary) {
                Text("Tải tối đa 5 ảnh: ảnh bìa, màn hình khóa, màn hình chính, icon…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Icon(Icons.Default.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text(if (previewUris.isEmpty()) "Chọn ảnh xem trước" else "Đã chọn ${previewUris.size} ảnh")
                }
                if (previewUris.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    previewUris.forEach { uri -> AsyncImage(model = uri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(92.dp).clip(RoundedCornerShape(18.dp))) }
                }
            }
        }
        item {
            SectionCard("Nguồn tải theme", Icons.Default.FolderZip) {
                OutlinedButton(onClick = { filePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Icon(Icons.Default.AttachFile, null); Spacer(Modifier.width(8.dp)); Text(fileUri?.let { SupabaseApi.fileName(LocalContext.current.contentResolver, it) } ?: "Chọn file .mtz / .zip")
                }
                Row(verticalAlignment = Alignment.CenterVertically) { HorizontalDivider(Modifier.weight(1f)); Text("  HOẶC FILE QUÁ LỚN  ", style = MaterialTheme.typography.labelSmall); HorizontalDivider(Modifier.weight(1f)) }
                OutlinedTextField(driveUrl, { driveUrl = it.trim() }, label = { Text("Link Google Drive") }, leadingIcon = { Icon(Icons.Default.Link, null) }, placeholder = { Text("https://drive.google.com/…") }, modifier = Modifier.fillMaxWidth())
                Text("Chỉ cần file trực tiếp hoặc link Drive. Admin sẽ kiểm tra trước khi duyệt.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú riêng cho Admin") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }
        item {
            Button(
                enabled = !uploading && title.isNotBlank() && (fileUri != null || driveUrl.isNotBlank()),
                onClick = {
                    uploading = true
                    scope.launch {
                        api.uploadTheme(session, fileUri, previewUris, driveUrl, title, description, category, osVersion, tags, note)
                            .onSuccess { onMessage("Đã gửi theme. Vui lòng chờ Admin duyệt."); onDone() }
                            .onFailure { onMessage(it.message ?: "Không thể đăng theme") }
                        uploading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp), shape = RoundedCornerShape(20.dp)
            ) {
                if (uploading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                if (uploading) Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Send, null); Spacer(Modifier.width(8.dp)); Text(if (uploading) "Đang tải lên…" else "Gửi duyệt Admin", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            content()
        }
    }
}

@Composable
private fun HeaderBlock(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary) } }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun MyThemesScreen(api: SupabaseApi, session: Session, onMessage: (String) -> Unit) {
    var themes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { api.myThemes(session).onSuccess { themes = it }.onFailure { onMessage(it.message ?: "Không tải được kho") }; loading = false }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { HeaderBlock("Kho của tôi", "Theo dõi trạng thái và phản hồi duyệt", Icons.Default.Inventory2) }
        if (loading) items(3) { ThemeSkeleton() }
        else if (themes.isEmpty()) item { EmptyState(Icons.Default.Inventory2, "Bạn chưa đăng theme", "Mở mục Đăng để gửi theme đầu tiên.") }
        else items(themes, key = { it.id }) { ThemeStatusCard(it) }
    }
}

@Composable
private fun ThemeStatusCard(theme: ThemeItem) {
    ElevatedCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = theme.previewUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(92.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(theme.osVersion, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                StatusChip(theme.status)
                if (theme.status == "rejected" && theme.rejectReason.isNotBlank()) Text("Lý do: ${theme.rejectReason}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (text, icon, color) = when (status) {
        "approved" -> Triple("Đã duyệt", Icons.Default.CheckCircle, Color(0xFF45D483))
        "rejected" -> Triple("Từ chối", Icons.Default.Cancel, MaterialTheme.colorScheme.error)
        else -> Triple("Chờ duyệt", Icons.Default.Schedule, MaterialTheme.colorScheme.tertiary)
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = .15f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(16.dp), tint = color); Spacer(Modifier.width(5.dp)); Text(text, color = color, style = MaterialTheme.typography.labelMedium) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScreen(api: SupabaseApi, session: Session, profile: Profile?, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var selected by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var users by remember { mutableStateOf<List<Profile>>(emptyList()) }
    suspend fun reload() {
        api.pendingThemes(session).onSuccess { pending = it }.onFailure { onMessage(it.message ?: "Lỗi tải theme") }
        api.users(session).onSuccess { users = it }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp)) {
            HeaderBlock("Bảng quản trị", "Duyệt theme, quản lý người dùng và quyền hạn", Icons.Default.AdminPanelSettings)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetric("Chờ duyệt", pending.size.toString(), Icons.Default.Schedule, Modifier.weight(1f))
                AdminMetric("Người dùng", users.size.toString(), Icons.Default.Group, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            PrimaryTabRow(selectedTabIndex = selected) {
                Tab(selected = selected == 0, onClick = { selected = 0 }, text = { Text("Chờ duyệt (${pending.size})") })
                Tab(selected = selected == 1, onClick = { selected = 1 }, text = { Text("Người dùng") })
            }
        }
        if (selected == 0) LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (pending.isEmpty()) item { EmptyState(Icons.Default.Verified, "Không có theme chờ duyệt", "Theme mới sẽ xuất hiện tại đây.") }
            else items(pending, key = { it.id }) { theme ->
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column {
                        if (theme.previewUrl.isNotBlank()) AsyncImage(model = theme.previewUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(170.dp))
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(theme.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(theme.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (theme.driveUrl.isNotBlank()) Text("Có link Google Drive", color = MaterialTheme.colorScheme.secondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { scope.launch { api.reviewTheme(session, theme.id, true).onSuccess { reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Check, null); Text(" Duyệt") }
                                OutlinedButton(onClick = { scope.launch { api.reviewTheme(session, theme.id, false, "Cần bổ sung hoặc chỉnh sửa nội dung").onSuccess { reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Close, null); Text(" Từ chối") }
                            }
                        }
                    }
                }
            }
        } else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users, key = { it.id }) { user ->
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(50.dp)) { Box(contentAlignment = Alignment.Center) { Text(user.displayName.take(1).uppercase().ifBlank { "M" }, fontWeight = FontWeight.Black) } }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.Bold); Text("@${user.username} • ${user.role}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (profile?.role == "super_admin" && user.role != "super_admin") {
                            TextButton(onClick = { scope.launch { api.setRole(session, user.id, if (user.role == "admin") "user" else "admin").onSuccess { reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }) { Text(if (user.role == "admin") "Hạ quyền" else "Lên Admin") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetric(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun ProfileScreen(api: SupabaseApi, session: Session, profile: Profile?, config: RemoteConfig, onLogout: () -> Unit, onMessage: (String) -> Unit) {
    var mine by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    LaunchedEffect(Unit) { api.myThemes(session).onSuccess { mine = it }.onFailure { onMessage(it.message ?: "Lỗi tải thống kê") } }
    val approved = mine.count { it.status == "approved" }
    val pending = mine.count { it.status == "pending" }
    val totalDownloads = mine.sumOf { it.downloads }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF39215E), Color(0xFF123A52)))).padding(22.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(26.dp), color = Color.White.copy(alpha = .14f), modifier = Modifier.size(82.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) } }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(profile?.displayName?.ifBlank { "M4X Member" } ?: "M4X Member", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, maxLines = 2)
                                Text("@${profile?.username.orEmpty()}", color = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.height(8.dp)); StatusChip(if (profile?.role == "super_admin") "approved" else "pending")
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProfileStat("Điểm", profile?.points?.toString() ?: "0", Modifier.weight(1f))
                            ProfileStat("Theme", mine.size.toString(), Modifier.weight(1f))
                            ProfileStat("Lượt tải", totalDownloads.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item {
            SectionCard("Hoạt động", Icons.Default.Insights) {
                SettingsRow(Icons.Default.Verified, "Đã duyệt", "$approved theme")
                SettingsRow(Icons.Default.Schedule, "Đang chờ", "$pending theme")
                SettingsRow(Icons.Default.AdminPanelSettings, "Vai trò", profile?.role ?: "user")
            }
        }
        item {
            SectionCard("Cập nhật online", Icons.Default.CloudSync) {
                Text("Theme, banner, trạng thái duyệt và quyền Admin được đồng bộ trực tiếp từ Supabase.")
                SettingsRow(Icons.Default.Android, "Phiên bản hiện tại", BuildConfig.VERSION_NAME)
                SettingsRow(Icons.Default.SystemUpdate, "Phiên bản online", config.latestVersionName.ifBlank { BuildConfig.VERSION_NAME })
            }
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(20.dp)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = .1f)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(label, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(12.dp)); Text(title, Modifier.weight(1f)); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(82.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary) } }
        Spacer(Modifier.height(16.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ThemeSkeleton() {
    ElevatedCard(shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
        Column { Box(Modifier.fillMaxWidth().height(170.dp).background(MaterialTheme.colorScheme.surfaceVariant)); Column(Modifier.padding(16.dp)) { Box(Modifier.fillMaxWidth(.65f).height(22.dp).background(MaterialTheme.colorScheme.surfaceVariant)); Spacer(Modifier.height(10.dp)); Box(Modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.surfaceVariant)) } }
    }
}
