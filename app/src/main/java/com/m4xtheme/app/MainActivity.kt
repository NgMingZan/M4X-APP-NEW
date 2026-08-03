package com.m4xtheme.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

    val admin = profile?.role in setOf("admin", "super_admin")
    val updateAvailable = config.latestVersionCode > BuildConfig.VERSION_CODE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("M4X Theme", fontWeight = FontWeight.Bold); Text("Online v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall) } },
                actions = { if (updateAvailable) Icon(Icons.Default.SystemUpdate, "Có cập nhật", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 16.dp)) }
            )
        },
        bottomBar = {
            NavigationBar {
                NavItem(tab == Tab.HOME, Icons.Default.Explore, "Khám phá") { tab = Tab.HOME }
                NavItem(tab == Tab.UPLOAD, Icons.Default.CloudUpload, "Đăng theme") { tab = Tab.UPLOAD }
                NavItem(tab == Tab.MY_THEMES, Icons.Default.Inventory2, "Của tôi") { tab = Tab.MY_THEMES }
                if (admin) NavItem(tab == Tab.ADMIN, Icons.Default.AdminPanelSettings, "Admin") { tab = Tab.ADMIN }
                NavItem(tab == Tab.PROFILE, Icons.Default.Person, "Tài khoản") { tab = Tab.PROFILE }
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
                Tab.PROFILE -> ProfileScreen(profile, config, onLogout = { api.signOut(); session = null; profile = null }, onMessage = { message = it })
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, click: () -> Unit) {
    NavigationBarItem(selected = selected, onClick = click, icon = { Icon(icon, null) }, label = { Text(label, maxLines = 1) })
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

    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Palette, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text("M4X Theme", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(if (register) "Tạo tài khoản mới" else "Đăng nhập hệ thống online", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            if (register) {
                OutlinedTextField(username, { username = it.trim() }, label = { Text("Tên đăng nhập") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Tên hiển thị") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(email, { email = it.trim() }, label = { Text("Email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !loading && SupabaseConfig.configured,
                onClick = {
                    loading = true
                    scope.launch {
                        val result = if (register) api.signUp(email, password, username, displayName) else api.signIn(email, password)
                        result.onSuccess(onSuccess).onFailure { onMessage(it.message ?: "Có lỗi xảy ra") }
                        loading = false
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Đang xử lý…" else if (register) "Đăng ký" else "Đăng nhập") }
            TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký")
            }
            if (!SupabaseConfig.configured) Text("Chưa cấu hình Supabase trong GitHub Secrets", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun HomeScreen(api: SupabaseApi, session: Session, config: RemoteConfig, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    suspend fun reload() { loading = true; api.approvedThemes(session).onSuccess { themes = it }.onFailure { onMessage(it.message ?: "Không tải được theme") }; loading = false }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 16.dp)) {
        item {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(config.homeBannerTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(config.homeBannerSubtitle, color = MaterialTheme.colorScheme.secondary)
                    Text("Banner và nội dung này cập nhật trực tiếp từ Supabase.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Tìm theme") }) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        val visible = themes.filter { it.title.contains(query, true) || it.category.contains(query, true) || it.osVersion.contains(query, true) }
        if (!loading && visible.isEmpty()) item { EmptyState("Chưa có theme nào", "Theme chỉ xuất hiện sau khi Admin duyệt.") }
        items(visible, key = { it.id }) { theme -> ThemeCard(theme, onDownload = {
            scope.launch {
                api.incrementDownload(session, theme.id)
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(theme.fileUrl))) }
                    .onFailure { onMessage("Không mở được file tải") }
            }
        }) }
    }
}

@Composable
private fun ThemeCard(theme: ThemeItem, onDownload: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(theme.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${theme.osVersion} • ${theme.category}", color = MaterialTheme.colorScheme.primary)
                }
                AssistChip(onClick = {}, label = { Text("★ ${"%.1f".format(theme.rating)}") })
            }
            Text(theme.description, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${theme.downloads} lượt tải", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                Button(onClick = onDownload) { Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Tải") }
            }
        }
    }
}

@Composable
private fun UploadScreen(api: SupabaseApi, session: Session, onMessage: (String) -> Unit, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Giao diện") }
    var osVersion by remember { mutableStateOf("HyperOS") }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        uri = selected
        selected?.let { runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Đăng theme", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Text("File được tải lên Supabase Storage và mặc định ở trạng thái chờ duyệt.") }
        item { OutlinedTextField(title, { title = it }, label = { Text("Tên theme") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(description, { description = it }, label = { Text("Mô tả") }, minLines = 4, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(osVersion, { osVersion = it }, label = { Text("HyperOS / MIUI") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(category, { category = it }, label = { Text("Danh mục") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedButton(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text(uri?.let { SupabaseApi.fileName(context.contentResolver, it) } ?: "Chọn file .mtz hoặc .zip")
            }
        }
        item {
            Button(enabled = !loading && uri != null, onClick = {
                loading = true
                scope.launch {
                    api.uploadTheme(session, uri!!, title, description, category, osVersion)
                        .onSuccess { onMessage("Đã gửi theme, vui lòng chờ Admin duyệt"); onDone() }
                        .onFailure { onMessage(it.message ?: "Upload thất bại") }
                    loading = false
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Đang tải lên…" else "Gửi duyệt") }
        }
    }
}

@Composable
private fun MyThemesScreen(api: SupabaseApi, session: Session, onMessage: (String) -> Unit) {
    var themes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { api.myThemes(session).onSuccess { themes = it }.onFailure { onMessage(it.message ?: "Lỗi") }; loading = false }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Theme của tôi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (!loading && themes.isEmpty()) item { EmptyState("Bạn chưa đăng theme", "Chọn Đăng theme để gửi file đầu tiên.") }
        items(themes, key = { it.id }) { t ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(t.title, style = MaterialTheme.typography.titleLarge)
                StatusChip(t.status)
                if (t.status == "rejected" && t.rejectReason.isNotBlank()) Text("Lý do: ${t.rejectReason}", color = MaterialTheme.colorScheme.error)
            } }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val label = when (status) { "approved" -> "Đã duyệt"; "rejected" -> "Bị từ chối"; else -> "Chờ duyệt" }
    AssistChip(onClick = {}, label = { Text(label) })
}

@Composable
private fun AdminScreen(api: SupabaseApi, session: Session, profile: Profile?, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var users by remember { mutableStateOf<List<Profile>>(emptyList()) }
    suspend fun reload() {
        api.pendingThemes(session).onSuccess { pending = it }.onFailure { onMessage(it.message ?: "Lỗi") }
        if (profile?.role == "super_admin") api.users(session).onSuccess { users = it }
    }
    LaunchedEffect(Unit) { reload() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Bảng quản trị", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = section) {
            Tab(selected = section == 0, onClick = { section = 0 }, text = { Text("Chờ duyệt (${pending.size})") })
            if (profile?.role == "super_admin") Tab(selected = section == 1, onClick = { section = 1 }, text = { Text("Người dùng") })
        }
        Spacer(Modifier.height(12.dp))
        if (section == 0) LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (pending.isEmpty()) item { EmptyState("Không có theme chờ duyệt", "Theme mới sẽ xuất hiện tại đây.") }
            items(pending, key = { it.id }) { t ->
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t.title, style = MaterialTheme.typography.titleLarge)
                    Text(t.description, maxLines = 3)
                    Row {
                        Button(onClick = { scope.launch { api.reviewTheme(session, t.id, true).onSuccess { onMessage("Đã duyệt"); reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }) { Text("Duyệt") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = { scope.launch { api.reviewTheme(session, t.id, false, "Theme chưa đạt tiêu chuẩn").onSuccess { onMessage("Đã từ chối"); reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }) { Text("Từ chối") }
                    }
                } }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users, key = { it.id }) { user ->
                ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(user.displayName.ifBlank { user.username }, fontWeight = FontWeight.Bold); Text("@${user.username} • ${user.role}") }
                    if (user.id != profile?.id && user.role != "super_admin") TextButton(onClick = {
                        val next = if (user.role == "admin") "user" else "admin"
                        scope.launch { api.setRole(session, user.id, next).onSuccess { onMessage("Đã đổi quyền thành $next"); reload() }.onFailure { onMessage(it.message ?: "Lỗi") } }
                    }) { Text(if (user.role == "admin") "Hạ quyền" else "Bổ nhiệm") }
                } }
            }
        }
    }
}

@Composable
private fun ProfileScreen(profile: Profile?, config: RemoteConfig, onLogout: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val updateAvailable = config.latestVersionCode > BuildConfig.VERSION_CODE
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Text(profile?.displayName?.ifBlank { profile.username } ?: "Đang tải", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("@${profile?.username ?: "..."}")
        AssistChip(onClick = {}, label = { Text(profile?.role ?: "user") })
        Text("Điểm: ${profile?.points ?: 0}")
        HorizontalDivider()
        Text("Cập nhật online", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Theme, banner, trạng thái duyệt và quyền Admin được đồng bộ trực tiếp từ Supabase.")
        if (updateAvailable) {
            Button(onClick = {
                if (config.updateUrl.isBlank()) onMessage("Admin chưa gắn link APK cập nhật")
                else runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(config.updateUrl))) }.onFailure { onMessage("Không mở được link cập nhật") }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SystemUpdate, null); Spacer(Modifier.width(8.dp)); Text("Cập nhật APK ${config.latestVersionName}")
            }
            if (config.updateMessage.isNotBlank()) Text(config.updateMessage)
        } else Text("Bạn đang dùng phiên bản mới nhất: ${BuildConfig.VERSION_NAME}")
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất") }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium)
    }
}

