package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { OnlineM4xApp() } }
    }
}

enum class AppTab { EXPLORE, UPLOAD, ADMIN, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineM4xApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val client = remember { SupabaseClient(context) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf(client.restoreSession()) }
    var profile by remember { mutableStateOf<Profile?>(null) }
    var tab by remember { mutableStateOf(AppTab.EXPLORE) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); message = null } }
    LaunchedEffect(session) {
        session?.let { s -> client.getProfile(s).onSuccess { profile = it }.onFailure { message = it.message } }
    }

    if (session == null) {
        AuthScreen(client, onSession = { client.saveSession(it); session = it }, onMessage = { message = it })
        return
    }

    val isAdmin = profile?.role in setOf("admin", "super_admin")
    Scaffold(
        topBar = { TopAppBar(title = { Text("M4X Theme Online") }, actions = { Text(profile?.username ?: "Đang tải…", modifier = Modifier.padding(end = 16.dp)) }) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == AppTab.EXPLORE, { tab = AppTab.EXPLORE }, { Icon(Icons.Default.Explore, null) }, { Text("Khám phá") })
                NavigationBarItem(tab == AppTab.UPLOAD, { tab = AppTab.UPLOAD }, { Icon(Icons.Default.Upload, null) }, { Text("Đăng theme") })
                if (isAdmin) NavigationBarItem(tab == AppTab.ADMIN, { tab = AppTab.ADMIN }, { Icon(Icons.Default.AdminPanelSettings, null) }, { Text("Admin") })
                NavigationBarItem(tab == AppTab.PROFILE, { tab = AppTab.PROFILE }, { Icon(Icons.Default.Person, null) }, { Text("Tài khoản") })
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                AppTab.EXPLORE -> ExploreScreen(client, session!!, onMessage = { message = it })
                AppTab.UPLOAD -> UploadScreen(client, session!!, onMessage = { message = it }, onDone = { tab = AppTab.PROFILE })
                AppTab.ADMIN -> AdminScreen(client, session!!, profile, onMessage = { message = it })
                AppTab.PROFILE -> ProfileScreen(profile, configured = SupabaseConfig.configured, onLogout = { client.signOut(); session = null; profile = null })
            }
        }
    }
}

@Composable
fun AuthScreen(client: SupabaseClient, onSession: (Session) -> Unit, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("M4X Theme", style = MaterialTheme.typography.headlineLarge)
            Text(if (register) "Tạo tài khoản online" else "Đăng nhập Supabase", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))
            if (register) {
                OutlinedTextField(username, { username = it.trim() }, label = { Text("Tên đăng nhập") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(display, { display = it }, label = { Text("Tên hiển thị") }, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(email, { email = it.trim() }, label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(enabled = !loading, onClick = {
                loading = true
                scope.launch {
                    val result = if (register) client.signUp(email, password, username, display) else client.signIn(email, password)
                    result.onSuccess(onSession).onFailure { onMessage(it.message ?: "Có lỗi") }
                    loading = false
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Đang xử lý…" else if (register) "Đăng ký" else "Đăng nhập") }
            TextButton(onClick = { register = !register }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(if (register) "Đã có tài khoản? Đăng nhập" else "Chưa có tài khoản? Đăng ký") }
            if (!SupabaseConfig.configured) Text("Chưa cấu hình khóa Supabase trong supabase.properties", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ExploreScreen(client: SupabaseClient, session: Session, onMessage: (String) -> Unit) {
    var list by remember { mutableStateOf<List<OnlineTheme>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    suspend fun load() { loading = true; client.getApprovedThemes(session).onSuccess { list = it }.onFailure { onMessage(it.message ?: "Lỗi") }; loading = false }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(query, { query = it }, leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Tìm theme") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        val shown = list.filter { it.name.contains(query, true) || it.category.contains(query, true) }
        if (!loading && shown.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Chưa có theme nào được Admin duyệt") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.id }) { t ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(t.name, style = MaterialTheme.typography.titleLarge)
                        Text(t.category.ifBlank { "Chưa phân loại" }, color = MaterialTheme.colorScheme.primary)
                        Text(t.description, maxLines = 3)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${t.downloads} lượt tải • ★ ${"%.1f".format(t.rating)}", modifier = Modifier.weight(1f))
                            val context = androidx.compose.ui.platform.LocalContext.current
                            Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(t.fileUrl))) }.onFailure { onMessage("Không mở được link tải") } }) { Text("Tải") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadScreen(client: SupabaseClient, session: Session, onMessage: (String) -> Unit, onDone: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var uri by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("HyperOS") }
    var loading by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selected ->
        uri = selected
        selected?.let { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Đăng theme mới", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Theme sẽ ở trạng thái chờ duyệt và chưa xuất hiện công khai.") }
        item { OutlinedTextField(name, { name = it }, label = { Text("Tên theme") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(desc, { desc = it }, label = { Text("Mô tả") }, minLines = 4, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(category, { category = it }, label = { Text("HyperOS / MIUI / danh mục") }, modifier = Modifier.fillMaxWidth()) }
        item {
            OutlinedButton(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text(uri?.let { SupabaseClient.queryName(context.contentResolver, it) } ?: "Chọn file .mtz hoặc .zip")
            }
        }
        item {
            Button(enabled = uri != null && !loading, onClick = {
                loading = true
                scope.launch {
                    client.uploadTheme(session, uri!!, name, desc, category)
                        .onSuccess { onMessage("Đã gửi theme, đang chờ Admin duyệt"); onDone() }
                        .onFailure { onMessage(it.message ?: "Upload thất bại") }
                    loading = false
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(if (loading) "Đang tải lên…" else "Gửi duyệt") }
        }
    }
}

@Composable
fun AdminScreen(client: SupabaseClient, session: Session, profile: Profile?, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<List<OnlineTheme>>(emptyList()) }
    var users by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var section by remember { mutableStateOf(0) }
    suspend fun reload() {
        client.getPendingThemes(session).onSuccess { pending = it }.onFailure { onMessage(it.message ?: "Lỗi") }
        if (profile?.role == "super_admin") client.listProfiles(session).onSuccess { users = it }
    }
    LaunchedEffect(Unit) { reload() }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row { FilterChip(section == 0, { section = 0 }, { Text("Chờ duyệt (${pending.size})") }); Spacer(Modifier.width(8.dp)); if (profile?.role == "super_admin") FilterChip(section == 1, { section = 1 }, { Text("Người dùng") }) }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (section == 0) items(pending, key = { it.id }) { t ->
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Text(t.name, style = MaterialTheme.typography.titleMedium); Text(t.description)
                    Row { Button(onClick = { scope.launch { client.approveTheme(session, t.id, true).onSuccess { onMessage("Đã duyệt"); reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }) { Text("Duyệt") }; Spacer(Modifier.width(8.dp)); OutlinedButton(onClick = { scope.launch { client.deleteTheme(session, t.id).onSuccess { onMessage("Đã từ chối và xóa"); reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }) { Text("Từ chối") } }
                } }
            } else items(users, key = { it.id }) { u ->
                ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(u.displayName.ifBlank { u.username }); Text("@${u.username} • ${u.role}") }
                    if (u.id != profile?.id) TextButton(onClick = { scope.launch { val role = if (u.role == "admin") "user" else "admin"; client.setRole(session, u.id, role).onSuccess { onMessage("Đã đổi quyền thành $role"); reload() }.onFailure { onMessage(it.message ?: "Lỗi") } } }) { Text(if (u.role == "admin") "Hạ quyền" else "Bổ nhiệm") }
                } }
            }
        }
    }
}

@Composable
fun ProfileScreen(profile: Profile?, configured: Boolean, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
        Text(profile?.displayName?.ifBlank { profile.username } ?: "Đang tải", style = MaterialTheme.typography.headlineMedium)
        Text("@${profile?.username ?: "..."}")
        AssistChip(onClick = {}, label = { Text(profile?.role ?: "user") })
        Text(if (configured) "Supabase Online: Đã cấu hình" else "Supabase Online: Chưa cấu hình", color = if (configured) Color(0xFF16A34A) else MaterialTheme.colorScheme.error)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Đăng xuất") }
    }
}
