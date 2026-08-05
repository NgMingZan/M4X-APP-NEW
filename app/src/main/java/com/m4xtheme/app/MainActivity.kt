package com.m4xtheme.app

import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets as AndroidWindowInsets
import android.widget.FrameLayout
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.m4xtheme.app.rust.RustThemeValidator
import com.m4xtheme.app.rust.ThemeValidationResult
import com.m4xtheme.app.ui.theme.M4XTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val title: String,
    val message: String,
    val forceUpdate: Boolean
)

private suspend fun fetchAppUpdate(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
    runCatching {
        val json = URL(BuildConfig.UPDATE_JSON_URL).openConnection().run {
            connectTimeout = 10_000
            readTimeout = 10_000
            getInputStream().bufferedReader().use { it.readText() }
        }
        val o = JSONObject(json)
        AppUpdateInfo(
            versionCode = o.getInt("versionCode"),
            versionName = o.optString("versionName", o.getInt("versionCode").toString()),
            apkUrl = o.getString("apkUrl"),
            title = o.optString("title", "Có bản cập nhật mới"),
            message = o.optString("message", "Cải thiện hiệu năng và sửa lỗi."),
            forceUpdate = o.optBoolean("forceUpdate", false)
        )
    }
}

private suspend fun downloadAndInstallUpdate(
    context: Context,
    update: AppUpdateInfo,
    onProgress: (Int) -> Unit
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        require(update.apkUrl.startsWith("https://")) { "Link APK phải sử dụng HTTPS" }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("M4X Theme ${update.versionName}")
            .setDescription("Đang tải bản cập nhật...")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "M4X-Theme-${update.versionName}.apk")
        val id = manager.enqueue(request)
        var done = false
        while (!done) {
            val query = DownloadManager.Query().setFilterById(id)
            manager.query(query).use { cursor ->
                if (!cursor.moveToFirst()) error("Không tìm thấy lượt tải cập nhật")
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> done = true
                    DownloadManager.STATUS_FAILED -> error("Tải APK thất bại")
                }
            }
            if (!done) delay(600)
        }
        val uri = manager.getUriForDownloadedFile(id) ?: error("Không mở được APK đã tải")
        withContext(Dispatchers.Main) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
            }
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}


private val themeDownloadHttp: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

private suspend fun downloadAndVerifyTheme(
    context: Context,
    theme: ThemeItem,
    onProgress: (Int) -> Unit
): Result<File> = withContext(Dispatchers.IO) {
    runCatching {
        require(theme.fileUrl.startsWith("https://")) {
            "Theme không có đường dẫn tải trực tiếp HTTPS"
        }
        val expectedSha256 = theme.approvedFileSha256
            .ifBlank { theme.clientFileSha256 }
        val expectedSizeBytes =
            theme.approvedFileSizeBytes
                .takeIf { it > 0L }
                ?: theme.clientFileSizeBytes

        require(
            expectedSha256.matches(
                Regex("^[0-9a-fA-F]{64}$")
            )
        ) {
            "Theme chưa có SHA-256 hợp lệ"
        }

        val downloadDirectory = File(
            context.getExternalFilesDir(
                Environment.DIRECTORY_DOWNLOADS
            ),
            "M4XThemes"
        ).apply {
            if (!exists() && !mkdirs()) {
                error("Không tạo được thư mục tải theme")
            }
        }

        val extension = Uri.parse(theme.fileUrl)
            .lastPathSegment
            ?.substringAfterLast('.', "mtz")
            ?.lowercase()
            ?.takeIf { it in setOf("mtz", "zip") }
            ?: "mtz"

        val safeTitle = theme.title
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
            .take(48)
            .ifBlank { "M4X_Theme" }

        val destination = File(
            downloadDirectory,
            "${safeTitle}_${expectedSha256.take(8)}.$extension"
        )
        val temporary = File(
            downloadDirectory,
            "${destination.name}.part"
        )

        runCatching { temporary.delete() }

        val request = Request.Builder()
            .url(theme.fileUrl)
            .header("User-Agent", "M4X-Theme/${BuildConfig.VERSION_NAME}")
            .get()
            .build()

        try {
            themeDownloadHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Tải theme thất bại (${response.code})")
                }

                val body = response.body
                    ?: error("Máy chủ không trả dữ liệu theme")
                val total = body.contentLength()
                val maximumBytes = 150L * 1024L * 1024L

                if (total > maximumBytes) {
                    error("File tải xuống vượt giới hạn 150 MB")
                }

                if (
                    expectedSizeBytes > 0L &&
                    total > 0L &&
                    total != expectedSizeBytes
                ) {
                    error(
                        "Dung lượng file không khớp bản được duyệt"
                    )
                }

                body.byteStream().use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        var lastProgress = -1

                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count == 0) continue

                            downloaded += count
                            if (downloaded > maximumBytes) {
                                error("File tải xuống vượt giới hạn 150 MB")
                            }

                            output.write(buffer, 0, count)

                            val progress = if (total > 0) {
                                ((downloaded * 100L) / total)
                                    .toInt()
                                    .coerceIn(0, 100)
                            } else {
                                0
                            }

                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main) {
                                    onProgress(progress)
                                }
                            }
                        }

                        output.fd.sync()
                    }
                }
            }

            val verification =
                RustThemeValidator.verifyDownloadedFile(
                    file = temporary,
                    expectedSha256 = expectedSha256
                ).getOrThrow()

            if (!verification.valid || !verification.matches) {
                error(verification.message)
            }

            if (
                expectedSizeBytes > 0L &&
                verification.sizeBytes != expectedSizeBytes
            ) {
                error(
                    "Dung lượng sau tải không khớp bản được duyệt"
                )
            }

            if (destination.exists() && !destination.delete()) {
                error("Không thể thay file theme cũ")
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
            }

            destination
        } catch (error: Throwable) {
            runCatching { temporary.delete() }
            throw error
        }
    }
}

private fun openVerifiedTheme(
    context: Context,
    file: File
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.files",
        file
    )

    val mimeTypes = listOf(
        "application/zip",
        "application/octet-stream",
        "*/*"
    )

    val viewIntent = mimeTypes
        .map { mime ->
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
        }
        .firstOrNull {
            it.resolveActivity(context.packageManager) != null
        }

    if (viewIntent != null) {
        context.startActivity(viewIntent)
        return
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            shareIntent,
            "Mở theme đã xác minh"
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

class MainActivity : ComponentActivity() {
    private var activeWebView: WebView? = null
    private var webViewerActive: Boolean = false
    private var fullscreenVideoView: View? = null
    private var fullscreenVideoCallback: WebChromeClient.CustomViewCallback? = null
    private var previousOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    val isVideoFullscreen: Boolean
        get() = fullscreenVideoView != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { M4XTheme { M4XApp() } }
    }

    fun setActiveWebView(webView: WebView?) {
        activeWebView = webView
        webViewerActive = webView != null
        updatePictureInPictureParams()
    }

    fun showFullscreenVideo(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullscreenVideoView != null) {
            callback.onCustomViewHidden()
            return
        }

        fullscreenVideoView = view
        fullscreenVideoCallback = callback
        previousOrientation = requestedOrientation

        val container = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            tag = "m4x_fullscreen_video_container"
        }

        addContentView(
            container,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(AndroidWindowInsets.Type.statusBars() or AndroidWindowInsets.Type.navigationBars())
            window.insetsController?.systemBarsBehavior =
                android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    fun hideFullscreenVideo() {
        val video = fullscreenVideoView ?: return
        val parent = video.parent
        if (parent is ViewGroup) {
            parent.removeView(video)
            (parent.parent as? ViewGroup)?.removeView(parent)
        }

        fullscreenVideoView = null
        fullscreenVideoCallback?.onCustomViewHidden()
        fullscreenVideoCallback = null
        requestedOrientation = previousOrientation
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(AndroidWindowInsets.Type.statusBars() or AndroidWindowInsets.Type.navigationBars())
        } else {
            @Suppress("DEPRECATION")
            run { window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE }
        }
    }

    private fun updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(webViewerActive)
            builder.setSeamlessResizeEnabled(true)
        }
        setPictureInPictureParams(builder.build())
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            webViewerActive &&
            activeWebView != null &&
            !isInPictureInPictureMode
        ) {
            runCatching {
                enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                )
            }
        }
    }
}

enum class Tab { HOME, QUEST, UPLOAD, WEB, PROFILE, SHOP, GAMES, ADMIN }

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
    var fullscreenWebUrl by remember { mutableStateOf<String?>(null) }
    var claimingAirdrop by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateProgress by remember { mutableIntStateOf(-1) }
    var arenaImmersive by remember { mutableStateOf(false) }
    var startGamesInFishing by remember { mutableStateOf(false) }
    var reviewNotifications by remember { mutableStateOf<List<ThemeReviewNotification>>(emptyList()) }
    var showReviewNotifications by remember { mutableStateOf(false) }
    val snack = remember { SnackbarHostState() }
    val appScope = rememberCoroutineScope()
    fun checkUpdate(showNoUpdate: Boolean = false) {
        if (checkingUpdate) return
        checkingUpdate = true
        appScope.launch {
            fetchAppUpdate()
                .onSuccess { info ->
                    if (info.versionCode > BuildConfig.VERSION_CODE) availableUpdate = info
                    else if (showNoUpdate) message = "Bạn đang dùng phiên bản mới nhất"
                }
                .onFailure { if (showNoUpdate) message = "Không thể kiểm tra cập nhật: ${it.message}" }
            checkingUpdate = false
        }
    }

    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); message = null } }
    LaunchedEffect(Unit) { checkUpdate(false) }
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

    LaunchedEffect(session?.userId) {
        val s = session ?: return@LaunchedEffect
        while (true) {
            delay(60_000L)
            api.recordAppUsage(s, 1)
        }
    }

    LaunchedEffect(session?.userId) {
        val s = session ?: return@LaunchedEffect
        while (true) {
            api.themeReviewNotifications(s).onSuccess { reviewNotifications = it }
            delay(60_000L)
        }
    }

    if (session == null) {
        AuthScreen(api, onSuccess = { api.saveSession(it); session = it }, onMessage = { message = it })
        return
    }

    fullscreenWebUrl?.let { url ->
        val activity = context as? MainActivity
        FullscreenWebViewer(
            url = url,
            onClose = { fullscreenWebUrl = null },
            onWebViewChanged = { activity?.setActiveWebView(it) }
        )
        return
    }

    LaunchedEffect(tab) {
        if (tab != Tab.GAMES) arenaImmersive = false
    }

    val isAdmin = profile?.role in setOf("admin", "super_admin")
    val isCreator = profile?.role == "creator"
    val canOpenControlCenter = isAdmin || isCreator
    Scaffold(
        contentWindowInsets = if (arenaImmersive) {
            WindowInsets(0, 0, 0, 0)
        } else {
            WindowInsets.safeDrawing
        },
        topBar = {
            if (!arenaImmersive) TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher),
                            contentDescription = "Logo M4X Theme",
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column { Text("M4X Theme", fontWeight = FontWeight.Black); Text("M4X COIN • Online v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary) }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        showReviewNotifications = true
                        appScope.launch {
                            api.themeReviewNotifications(session!!).onSuccess { reviewNotifications = it }
                        }
                    }) {
                        BadgedBox(
                            badge = {
                                val unread = reviewNotifications.count { it.readAt.isBlank() }
                                if (unread > 0) Badge { Text(unread.coerceAtMost(99).toString()) }
                            }
                        ) { Icon(Icons.Default.Notifications, "Thông báo theme") }
                    }
                    if (canOpenControlCenter) IconButton(onClick = { tab = Tab.ADMIN }) { Icon(Icons.Default.AdminPanelSettings, if (isCreator) "Duyệt theme" else "Admin") }
                    IconButton(onClick = { message = "Bạn đang có ${profile?.points ?: 0} M4X COIN" }) { Icon(Icons.Default.Paid, "M4X COIN", tint = Color(0xFFFFC857)) }
                }
            )
        },
        bottomBar = {
            if (!arenaImmersive) NavigationBar(
                windowInsets = WindowInsets.navigationBars
            ) {
                Nav(tab == Tab.HOME, Icons.Default.Home, "Khám phá") { tab = Tab.HOME }
                Nav(tab == Tab.QUEST, Icons.Default.Map, "Nhiệm vụ") { tab = Tab.QUEST }
                Nav(tab == Tab.UPLOAD, Icons.Default.AddCircle, "Đăng") { tab = Tab.UPLOAD }
                Nav(tab == Tab.WEB, Icons.Default.Public, "M4X WEB") { tab = Tab.WEB }
                Nav(tab in setOf(Tab.PROFILE, Tab.SHOP, Tab.GAMES), Icons.Default.Person, "Hồ sơ") { tab = Tab.PROFILE }
            }
        },
        snackbarHost = {
            if (!arenaImmersive) SnackbarHost(snack)
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    api = api,
                    session = session!!,
                    config = config,
                    profile = profile,
                    onOpenFishing = {
                        startGamesInFishing = true
                        tab = Tab.GAMES
                    },
                    onMessage = { message = it }
                )
                Tab.QUEST -> QuestHub(api, session!!, profile, onCoinChanged = { newBalance -> profile = profile?.copy(points = newBalance) }, onMessage = { message = it })
                Tab.UPLOAD -> UploadScreen(api, session!!, onMessage = { message = it }, onDone = { tab = Tab.PROFILE })
                Tab.WEB -> M4XWebScreen(config = config, isAdmin = isAdmin, onOpen = { fullscreenWebUrl = it })
                Tab.PROFILE -> ProfileScreen(
                    api = api,
                    session = session!!,
                    profile = profile,
                    config = config,
                    isAdmin = canOpenControlCenter,
                    checkingUpdate = checkingUpdate,
                    availableUpdate = availableUpdate,
                    onCheckUpdate = { checkUpdate(true) },
                    onOpenShop = { tab = Tab.SHOP },
                    onOpenGames = { tab = Tab.GAMES },
                    onProfileChanged = { profile = it },
                    onOpenAdmin = { tab = Tab.ADMIN },
                    onLogout = { api.signOut(); session = null; profile = null },
                    onMessage = { message = it }
                )
                Tab.SHOP -> ShopScreen(
                    api = api,
                    session = session!!,
                    profile = profile,
                    onBack = { tab = Tab.PROFILE },
                    onCoinChanged = { profile = profile?.copy(points = it) },
                    onMessage = { message = it }
                )
                Tab.GAMES -> M4XGamesHubScreen(
                    api = api,
                    session = session!!,
                    profile = profile,
                    onBack = { tab = Tab.PROFILE },
                    onCoinChanged = { profile = profile?.copy(points = it) },
                    onMessage = { message = it },
                    onImmersiveChanged = { arenaImmersive = it },
                    startInFishing = startGamesInFishing,
                    onStartInFishingHandled = {
                        startGamesInFishing = false
                    }
                )
                Tab.ADMIN -> AdminScreen(api, session!!, profile, config, onConfigChanged = { config = it }, onMessage = { message = it })
            }
            if (showAirdropChest && !arenaImmersive) {
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

    if (showReviewNotifications) {
        ThemeReviewNotificationsDialog(
            notifications = reviewNotifications,
            onDismiss = { showReviewNotifications = false },
            onMarkAllRead = {
                appScope.launch {
                    api.markAllThemeNotificationsRead(session!!)
                        .onSuccess {
                            reviewNotifications = reviewNotifications.map {
                                if (it.readAt.isBlank()) it.copy(readAt = "read") else it
                            }
                        }
                        .onFailure { message = it.message ?: "Không thể đánh dấu đã đọc" }
                }
            }
        )
    }

    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { if (!update.forceUpdate && updateProgress < 0) availableUpdate = null },
            icon = { Icon(Icons.Default.SystemUpdate, null) },
            title = { Text(update.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Phiên bản hiện tại: ${BuildConfig.VERSION_NAME}")
                    Text("Phiên bản mới: ${update.versionName}", fontWeight = FontWeight.Bold)
                    Text(update.message)
                    if (updateProgress >= 0) {
                        LinearProgressIndicator(progress = { updateProgress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("Đang tải: $updateProgress%")
                    }
                }
            },
            dismissButton = {
                if (!update.forceUpdate && updateProgress < 0) TextButton(onClick = { availableUpdate = null }) { Text("Để sau") }
            },
            confirmButton = {
                Button(
                    enabled = updateProgress < 0,
                    onClick = {
                        updateProgress = 0
                        appScope.launch {
                            downloadAndInstallUpdate(context, update) { updateProgress = it }
                                .onFailure { message = "Cập nhật thất bại: ${it.message}" }
                            updateProgress = -1
                        }
                    }
                ) { Text(if (updateProgress >= 0) "Đang tải..." else "Tải cập nhật") }
            }
        )
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
private fun HomeScreen(
    api: SupabaseApi,
    session: Session,
    config: RemoteConfig,
    profile: Profile?,
    onOpenFishing: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var themes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tất cả") }
    var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var downloadingThemeId by remember { mutableStateOf<String?>(null) }
    var purchasingThemeId by remember { mutableStateOf<String?>(null) }
    var purchasedThemeIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var coinBalance by remember(profile?.points) {
        mutableStateOf(profile?.points ?: 0L)
    }
    var selectedTheme by remember { mutableStateOf<ThemeItem?>(null) }

    LaunchedEffect(Unit) {
        api.approvedThemes(session)
            .onSuccess { themes = it }
            .onFailure { onMessage(it.message ?: "Lỗi tải theme") }
        api.activeEvents(session).onSuccess { events = it }
        api.purchasedThemeIds(session)
            .onSuccess { purchasedThemeIds = it }
            .onFailure {
                onMessage(it.message ?: "Không thể tải thư viện theme đã mua")
            }
    }

    val filtered = themes.filter {
        (query.isBlank() || it.title.contains(query, true)) &&
            (category == "Tất cả" || it.category.contains(category, true))
    }

    selectedTheme?.let { theme ->
        val purchased = theme.id in purchasedThemeIds
        ThemeDetailDialog(
            theme = theme,
            currentCoin = coinBalance,
            purchased = purchased,
            purchasing = purchasingThemeId == theme.id,
            downloading = downloadingThemeId == theme.id,
            onDismiss = {
                if (downloadingThemeId == null && purchasingThemeId == null) {
                    selectedTheme = null
                }
            },
            onBuy = {
                scope.launch {
                    if (purchased) {
                        onMessage("Theme này đã thuộc thư viện của bạn")
                        return@launch
                    }
                    if (coinBalance < theme.coinPrice) {
                        onMessage("Bạn chưa đủ ${theme.coinPrice} M4X COIN")
                        return@launch
                    }

                    purchasingThemeId = theme.id
                    api.purchaseTheme(session, theme.id)
                        .onSuccess {
                            purchasedThemeIds = purchasedThemeIds + theme.id
                            coinBalance = (coinBalance - theme.coinPrice)
                                .coerceAtLeast(0L)
                            onMessage(
                                if (theme.coinPrice > 0) {
                                    "Mua theme thành công. Bây giờ bạn có thể tải xuống."
                                } else {
                                    "Đã thêm theme miễn phí vào thư viện. Bây giờ bạn có thể tải xuống."
                                }
                            )
                        }
                        .onFailure {
                            onMessage(it.message ?: "Không thể mua theme")
                        }
                    purchasingThemeId = null
                }
            },
            onDownload = {
                scope.launch {
                    if (theme.id !in purchasedThemeIds) {
                        onMessage("Bạn cần mua hoặc nhận theme trước khi tải")
                        return@launch
                    }

                    if (theme.fileUrl.isBlank()) {
                        if (theme.driveUrl.isNotBlank()) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(theme.driveUrl)
                                )
                            )
                            api.incrementDownload(session, theme.id)
                            themes = themes.map {
                                if (it.id == theme.id) {
                                    it.copy(downloads = it.downloads + 1)
                                } else {
                                    it
                                }
                            }
                            onMessage(
                                "Đã mở nguồn tải Drive. File ngoài chưa thể xác minh SHA-256 tự động."
                            )
                        } else {
                            onMessage("Theme chưa có nguồn tải")
                        }
                        return@launch
                    }

                    val expectedSha256 =
                        theme.approvedFileSha256.ifBlank {
                            theme.clientFileSha256
                        }
                    val canVerify = expectedSha256.matches(
                        Regex("^[0-9a-fA-F]{64}$")
                    )

                    if (!canVerify) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(theme.fileUrl)
                            )
                        )
                        api.incrementDownload(session, theme.id)
                        themes = themes.map {
                            if (it.id == theme.id) {
                                it.copy(downloads = it.downloads + 1)
                            } else {
                                it
                            }
                        }
                        onMessage(
                            "Đã mở nguồn tải ngoài. Theme chưa có SHA-256 hợp lệ."
                        )
                        return@launch
                    }

                    downloadingThemeId = theme.id
                    downloadAndVerifyTheme(
                        context = context,
                        theme = theme,
                        onProgress = { }
                    ).onSuccess { verifiedFile ->
                        api.incrementDownload(session, theme.id)
                        themes = themes.map {
                            if (it.id == theme.id) {
                                it.copy(downloads = it.downloads + 1)
                            } else {
                                it
                            }
                        }
                        onMessage(
                            "Đã tải và xác minh đúng SHA-256 của bản được duyệt"
                        )
                        runCatching {
                            openVerifiedTheme(context, verifiedFile)
                        }.onFailure {
                            onMessage(
                                "File đã xác minh và lưu trong thư mục M4XThemes"
                            )
                        }
                    }.onFailure {
                        onMessage(
                            it.message ?: "Không thể xác minh file theme"
                        )
                    }
                    downloadingThemeId = null
                }
            }
        )
    }

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
                            MetricPill(Icons.Default.Paid, "$coinBalance COIN")
                            MetricPill(Icons.Default.Download, "${themes.sumOf { it.downloads }} lượt tải")
                        }
                    }
                }
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .clickable(onClick = onOpenFishing)
            ) {
                FishingArcadeHomeHero(
                    rod = null,
                    inventoryCount = 0,
                    inventoryValue = 0
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    color = Color.White.copy(alpha = 0.90f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFFBCEBFF)
                    )
                ) {
                    Column(
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        )
                    ) {
                        Text(
                            "MỚI • FISHING ARCADE V7",
                            color = Color(0xFF167EB5),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            "Chơi ngay",
                            color = Color(0xFF173F65),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(14.dp),
                    color = Color(0xFFFFC83D),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 10.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            null,
                            tint = Color(0xFF744300)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "VÀO GAME",
                            color = Color(0xFF744300),
                            fontWeight = FontWeight.Black
                        )
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Chủ đề",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Trên máy", "Ưa thích", "Thích", "Đặt hàng").forEachIndexed { index, tab ->
                        val selectedTab = index == 0
                        Surface(
                            color = if (selectedTab) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = .45f)
                            )
                        ) {
                            Text(
                                tab,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Tất cả", "HyperOS", "MIUI", "Lockscreen", "Icons", "Control Center").forEach {
                        FilterChip(
                            selected = category == it,
                            onClick = { category = it },
                            label = { Text(it) }
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Khám phá theme theo kiểu thư viện giống video: chạm để xem chi tiết, vuốt ảnh và xem tác giả.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (filtered.isEmpty()) item { EmptyState("Chưa có theme", "Theme được duyệt sẽ xuất hiện ở đây") }
        else items(filtered.chunked(3)) { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        purchased = theme.id in purchasedThemeIds,
                        modifier = Modifier.weight(1f),
                        onViewDetails = {
                            selectedTheme = theme
                            scope.launch {
                                api.recordThemeView(session, theme.id)
                            }
                        }
                    )
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable private fun MetricPill(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) { Surface(color = Color.White.copy(alpha = .16f), shape = CircleShape) { Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(text, fontWeight = FontWeight.Bold) } } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun EventBanner(event: EventItem) { ElevatedCard(shape = RoundedCornerShape(26.dp)) { Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFFF4E8A), Color(0xFF6E48FF)))).padding(20.dp)) { Column { Text(event.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(event.description); Spacer(Modifier.height(8.dp)); AssistChip(onClick = {}, label = { Text("${event.startAt.take(10)} → ${event.endAt.take(10)}") }) } } } }

@Composable
private fun ThemeCard(
    theme: ThemeItem,
    purchased: Boolean,
    modifier: Modifier = Modifier,
    onViewDetails: () -> Unit
) {
    val authorName = themeAuthorName(theme)
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .height(236.dp)
            .clickable(onClick = onViewDetails),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF101010)
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            if (theme.previewUrl.isNotBlank()) {
                AsyncImage(
                    theme.previewUrl,
                    theme.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1B1B1B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Palette,
                        null,
                        Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = .75f)
                            )
                        )
                    )
            )

            if (purchased) {
                Surface(
                    color = Color(0xFF20B486).copy(alpha = .94f),
                    shape = RoundedCornerShape(bottomStart = 14.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        "Đã sở hữu",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }

            Surface(
                color = Color.Black.copy(alpha = .58f),
                shape = RoundedCornerShape(bottomEnd = 14.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    if (theme.coinPrice > 0) "${theme.coinPrice} coin" else "Miễn phí",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    theme.title,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    authorName,
                    color = Color.White.copy(alpha = .85f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ThemeDetailDialog(
    theme: ThemeItem,
    currentCoin: Long,
    purchased: Boolean,
    purchasing: Boolean,
    downloading: Boolean,
    onDismiss: () -> Unit,
    onBuy: () -> Unit,
    onDownload: () -> Unit
) {
    val previews = remember(theme.id, theme.previewUrl, theme.previewUrls) {
        (listOf(theme.previewUrl) + theme.previewUrls)
            .filter { it.isNotBlank() }
            .distinct()
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { previews.size.coerceAtLeast(1) }
    )
    val busy = purchasing || downloading
    val authorName = themeAuthorName(theme)
    val authorSubtitle = themeAuthorSubtitle(theme)

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(
                        "Chủ đề",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !busy,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                            .background(
                                Color.White.copy(alpha = .10f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            null,
                            tint = Color.White
                        )
                    }
                }

                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("Trên máy", "Ưa thích", "Thích", "Đặt hàng").forEachIndexed { index, label ->
                        Surface(
                            color = if (index == 0) Color(0xFF2B2B2B) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .16f))
                        ) {
                            Text(
                                label,
                                color = Color.White.copy(alpha = if (index == 0) 1f else .75f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    if (previews.isNotEmpty()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 26.dp),
                            pageSpacing = 12.dp
                        ) { page ->
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color(0xFF151515))
                            ) {
                                AsyncImage(
                                    previews[page],
                                    "${theme.title} ${page + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 26.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color(0xFF161616)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                null,
                                Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (theme.category.isNotBlank()) {
                                AssistChip(
                                    onClick = { },
                                    label = { Text(theme.category) }
                                )
                            }
                            AssistChip(
                                onClick = { },
                                label = { Text(theme.osVersion.ifBlank { "MIUI/HyperOS" }) }
                            )
                            AssistChip(
                                onClick = { },
                                label = { Text("${"%.1f".format(theme.rating)} ★") }
                            )
                        }
                    }

                    item {
                        Text(
                            theme.title,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            theme.description.ifBlank { "Theme cộng đồng M4X." },
                            color = Color.White.copy(alpha = .78f)
                        )
                    }

                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF242424),
                                shape = CircleShape
                            ) {
                                Box(
                                    Modifier.size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        authorName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Tác giả",
                                    color = Color.White.copy(alpha = .56f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    authorName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                                if (authorSubtitle.isNotBlank()) {
                                    Text(
                                        authorSubtitle,
                                        color = Color.White.copy(alpha = .60f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = Color(0xFF141414)
                            ),
                            shape = RoundedCornerShape(22.dp)
                        ) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Thông tin bản được duyệt",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    "Hệ điều hành: ${theme.osVersion.ifBlank { "Chưa ghi rõ" }}",
                                    color = Color.White.copy(alpha = .88f)
                                )
                                Text(
                                    "Mức an toàn: ${theme.clientSafetyLevel.ifBlank { "Chưa đánh giá" }}",
                                    color = Color.White.copy(alpha = .88f)
                                )
                                Text(
                                    "Điểm an toàn: ${theme.clientSafetyScore}/100",
                                    color = Color.White.copy(alpha = .88f)
                                )
                                Text(
                                    "Lượt tải: ${theme.downloads}",
                                    color = Color.White.copy(alpha = .88f)
                                )
                                Text(
                                    if (theme.approvedFileSha256.isNotBlank()) {
                                        "Có xác minh SHA-256"
                                    } else {
                                        "Nguồn tải ngoài hoặc chưa có SHA-256"
                                    },
                                    color = Color.White.copy(alpha = .66f)
                                )
                            }
                        }
                    }

                    item {
                        if (purchased) {
                            ElevatedCard(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0xFF113528)
                                )
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        null,
                                        tint = Color(0xFF48D8A0)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Theme đã thuộc thư viện của bạn",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            "Bạn có thể tải lại bất cứ lúc nào.",
                                            color = Color.White.copy(alpha = .72f)
                                        )
                                    }
                                }
                            }
                        } else {
                            ElevatedCard(
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = Color(0xFF141414)
                                )
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Số dư: $currentCoin M4X COIN",
                                        color = if (currentCoin >= theme.coinPrice) Color(0xFFFFCF60) else Color(0xFFFF807B),
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Mua trước rồi mới tải theme.",
                                        color = Color.White.copy(alpha = .72f)
                                    )
                                }
                            }
                        }
                    }
                }

                Surface(
                    color = Color.Black,
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB13A))
                        ) {
                            Text(
                                "Tạo",
                                color = Color(0xFFFFB13A),
                                fontWeight = FontWeight.Black
                            )
                        }

                        Button(
                            onClick = if (purchased) onDownload else onBuy,
                            enabled = !busy && (purchased || currentCoin >= theme.coinPrice),
                            modifier = Modifier.weight(1.4f),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC34A),
                                contentColor = Color.Black
                            )
                        ) {
                            when {
                                purchasing -> {
                                    CircularProgressIndicator(
                                        Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text("Đang mua…", fontWeight = FontWeight.Black)
                                }
                                downloading -> {
                                    CircularProgressIndicator(
                                        Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.width(7.dp))
                                    Text("Đang tải…", fontWeight = FontWeight.Black)
                                }
                                purchased -> {
                                    Text("Tải theme", fontWeight = FontWeight.Black)
                                }
                                theme.coinPrice > 0 -> {
                                    Text("Mua bằng ${theme.coinPrice} coin", fontWeight = FontWeight.Black)
                                }
                                else -> {
                                    Text("Nhận miễn phí", fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun themeMetadata(theme: ThemeItem): JSONObject =
    runCatching { JSONObject(theme.clientThemeMetadata.ifBlank { "{}" }) }
        .getOrDefault(JSONObject())

private fun themeAuthorName(theme: ThemeItem): String {
    val meta = themeMetadata(theme)
    return meta.optString("author")
        .ifBlank { meta.optString("designer") }
        .ifBlank { meta.optString("creator") }
        .ifBlank { "Nhà sáng tạo M4X" }
}

private fun themeAuthorSubtitle(theme: ThemeItem): String {
    val meta = themeMetadata(theme)
    return meta.optString("author_contact")
        .ifBlank { meta.optString("publisher") }
        .ifBlank { if (theme.ownerId.isNotBlank()) "ID: ${theme.ownerId.take(8)}" else "" }
}

@Composable
private fun QuestHub
@Composable
private fun QuestHub(api: SupabaseApi, session: Session, profile: Profile?, onCoinChanged: (Long) -> Unit, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var quests by remember { mutableStateOf<List<QuestItem>>(emptyList()) }
    var claimed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var gift by remember { mutableStateOf("") }
    var leaderboard by remember { mutableStateOf<List<LeaderboardItem>>(emptyList()) }
    var openingChest by remember { mutableStateOf(false) }
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
        item { SectionTitle("Top đóng góp tuần", "Tính theo theme được duyệt, lượt tải hợp lệ và thời gian dùng app") }
        items(leaderboard.take(5)) { item -> LeaderRow(item) }
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Inventory2, null, tint = Color(0xFFFFC857))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Mở Rương M4X", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                            Text("Phí mở: 20 M4X COIN • Thưởng tối đa 3.500", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("“Cờ bạc là bác thằng bần”", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    Button(
                        enabled = !openingChest && (profile?.points ?: 0) >= 20,
                        onClick = {
                            openingChest = true
                            scope.launch {
                                api.openCoinChest(session).onSuccess { result ->
                                    onCoinChanged(result.balance)
                                    onMessage(if (result.reward > 0) "🎁 ${result.message}: +${result.reward} M4X COIN" else "🍀 ${result.message}")
                                }.onFailure { onMessage(it.message ?: "Không thể mở rương") }
                                openingChest = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text(if (openingChest) "Đang mở…" else "Mở rương • 20 coin", fontWeight = FontWeight.Black) }
                    Text("Giải dương thấp nhất 10 coin; có thể nhận ‘Chúc bạn may mắn lần sau’. Kết quả do máy chủ quyết định.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Inventory2, null, tint = Color(0xFFFFC857)); Spacer(Modifier.width(12.dp)); Column { Text("Rương Airdrop ngẫu nhiên", fontWeight = FontWeight.Black); Text("Rương miễn phí sẽ xuất hiện bất ngờ khi bạn đang sử dụng app") } } } }
    }
}

@Composable private fun LeaderRow(item: LeaderboardItem) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#${item.rank}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.displayName, fontWeight = FontWeight.Bold)
                Text("${item.approvedThemes} theme • ${item.downloadsReceived} lượt tải • ${item.activeMinutes} phút", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${item.score} điểm", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun UploadScreen(
    api: SupabaseApi,
    session: Session,
    onMessage: (String) -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var previewUris by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var driveUrl by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("0") }
    var uploading by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var validationReport by remember {
        mutableStateOf<ThemeValidationResult?>(null)
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        fileUri = selectedUri
        validationReport = null

        if (selectedUri != null) {
            validating = true
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    RustThemeValidator.validate(
                        context = context,
                        uri = selectedUri,
                        maxSizeBytes =
                            RustThemeValidator.DEFAULT_MAX_SIZE_BYTES
                    )
                }

                result.onSuccess { report ->
                    validationReport = report

                    if (
                        title.isBlank() &&
                        report.metadata.title.isNotBlank()
                    ) {
                        title = report.metadata.title
                    }

                    if (
                        description.isBlank() &&
                        report.metadata.description.isNotBlank()
                    ) {
                        description = report.metadata.description
                    }

                    onMessage(report.message)
                }.onFailure {
                    onMessage(
                        it.message ?: "Không thể kiểm tra file bằng Rust"
                    )
                }

                validating = false
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) {
        previewUris = it.take(5)
    }

    val fileValidationReady =
        fileUri == null || validationReport?.valid == true

    LazyColumn(
        Modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                "Đăng theme mới",
                "Rust tự đọc metadata, module và chấm điểm an toàn"
            )
        }

        item {
            FormCard("Thông tin theme", Icons.Default.Edit) {
                OutlinedTextField(
                    title,
                    { title = it },
                    label = { Text("Tên theme") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Mô tả") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    price,
                    { price = it.filter(Char::isDigit) },
                    label = { Text("Giá M4X COIN") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            FormCard("Ảnh xem trước", Icons.Default.PhotoLibrary) {
                OutlinedButton(
                    onClick = {
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chọn tối đa 5 ảnh " +
                            "(${previewUris.size}/5)"
                    )
                }

                Row(
                    Modifier.horizontalScroll(
                        rememberScrollState()
                    ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    previewUris.forEach {
                        AsyncImage(
                            it,
                            null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(92.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
        }

        item {
            FormCard("Nguồn tải", Icons.Default.CloudUpload) {
                OutlinedButton(
                    enabled = !validating,
                    onClick = {
                        filePicker.launch(arrayOf("*/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (validating) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Rust đang kiểm tra…")
                    } else {
                        Text(
                            fileUri?.let {
                                SupabaseApi.fileName(
                                    context.contentResolver,
                                    it
                                )
                            } ?: "Chọn file .mtz / .zip"
                        )
                    }
                }

                OutlinedTextField(
                    driveUrl,
                    { driveUrl = it.trim() },
                    label = {
                        Text("Hoặc link Google Drive")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        validationReport?.let { report ->
            item {
                FormCard(
                    "Báo cáo Rust ${report.safetyScore}/100",
                    Icons.Default.Security
                ) {
                    val scoreColor = when {
                        report.safetyScore >= 90 ->
                            MaterialTheme.colorScheme.primary
                        report.safetyScore >= 60 ->
                            MaterialTheme.colorScheme.tertiary
                        else ->
                            MaterialTheme.colorScheme.error
                    }

                    Text(
                        report.message,
                        color = scoreColor,
                        fontWeight = FontWeight.Bold
                    )

                    LinearProgressIndicator(
                        progress = {
                            report.safetyScore
                                .coerceIn(0, 100) / 100f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(9.dp)
                            .clip(CircleShape)
                    )

                    val metadata = report.metadata
                    val author = metadata.author
                        .ifBlank { metadata.designer }

                    if (metadata.title.isNotBlank()) {
                        Text(
                            "Tên đọc được: ${metadata.title}",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (author.isNotBlank()) {
                        Text("Tác giả: $author")
                    }
                    if (metadata.version.isNotBlank()) {
                        Text("Phiên bản theme: ${metadata.version}")
                    }
                    if (metadata.uiVersion.isNotBlank()) {
                        Text(
                            "UI/MIUI/HyperOS: " +
                                metadata.uiVersion
                        )
                    }

                    val presentModules = report.modules
                        .filter { it.present }

                    if (presentModules.isNotEmpty()) {
                        Text(
                            "Module nhận diện",
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            Modifier.horizontalScroll(
                                rememberScrollState()
                            ),
                            horizontalArrangement =
                                Arrangement.spacedBy(6.dp)
                        ) {
                            presentModules.forEach { module ->
                                AssistChip(
                                    onClick = {},
                                    label = {
                                        Text(module.label)
                                    }
                                )
                            }
                        }
                    }

                    report.warnings.take(4).forEach {
                        Text(
                            "⚠ $it",
                            color =
                                MaterialTheme.colorScheme.tertiary,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }

                    report.errors.take(4).forEach {
                        Text(
                            "✕ $it",
                            color =
                                MaterialTheme.colorScheme.error,
                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Button(
                enabled =
                    !uploading &&
                    !validating &&
                    title.isNotBlank() &&
                    (fileUri != null || driveUrl.isNotBlank()) &&
                    fileValidationReady,
                onClick = {
                    uploading = true
                    scope.launch {
                        api.uploadTheme(
                            session = session,
                            fileUri = fileUri,
                            previewUris = previewUris,
                            driveUrl = driveUrl,
                            title = title,
                            description = description,
                            category = "HyperOS",
                            osVersion =
                                validationReport
                                    ?.metadata
                                    ?.uiVersion
                                    .orEmpty(),
                            tags = "",
                            adminNote = "",
                            coinPrice =
                                price.toIntOrNull() ?: 0
                        ).onSuccess {
                            onMessage(
                                "Đã gửi Admin duyệt"
                            )
                            onDone()
                        }.onFailure {
                            onMessage(
                                it.message ?: "Lỗi upload"
                            )
                        }
                        uploading = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    when {
                        uploading -> "Đang tải…"
                        validating -> "Đang kiểm tra Rust…"
                        fileUri != null &&
                            validationReport?.valid != true ->
                            "File chưa đạt kiểm tra"
                        else -> "Gửi duyệt Admin"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable private fun FormCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) { ElevatedCard(shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.width(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }; content() } } }

@Composable
private fun M4XWebScreen(config: RemoteConfig, isAdmin: Boolean, onOpen: (String) -> Unit) {
    var showAdultConfirm by remember { mutableStateOf(false) }
    var pendingAdultUrl by remember { mutableStateOf("") }
    val tasks = listOf(
        Triple("Xem Đá Bóng", config.webFootballUrl, config.webFootballEnabled),
        Triple("Xem phim", config.webMovieUrl, config.webMovieEnabled),
        Triple("Xem 18+", config.webAdultUrl, config.webAdultEnabled)
    )
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionTitle("M4X WEB", "Mở nội dung web toàn màn hình ngay trong ứng dụng") }
        items(tasks.filter { it.third }) { task ->
            val (title, url, _) = task
            ElevatedCard(Modifier.fillMaxWidth().clickable {
                if (title == "Xem 18+") { pendingAdultUrl = url; showAdultConfirm = true } else onOpen(url)
            }, shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(when(title) { "Xem Đá Bóng" -> Icons.Default.SportsSoccer; "Xem phim" -> Icons.Default.Movie; else -> Icons.Default.Explicit }, null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge); Text("Mở toàn màn hình", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Icon(Icons.Default.ChevronRight, null)
                }
            }
        }
        if (tasks.none { it.third }) item { EmptyState("M4X WEB đang tạm tắt", "Admin chưa bật tác vụ web nào") }
        if (isAdmin) item { Text("Admin có thể đổi link hoặc tắt tác vụ trong Trung tâm điều hành → M4X WEB.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (showAdultConfirm) {
        AlertDialog(
            onDismissRequest = { showAdultConfirm = false },
            title = { Text("Xác nhận độ tuổi") },
            text = { Text("Nội dung này chỉ dành cho người từ 18 tuổi trở lên.") },
            confirmButton = { TextButton(onClick = { showAdultConfirm = false; onOpen(pendingAdultUrl) }) { Text("Tôi đủ 18 tuổi") } },
            dismissButton = { TextButton(onClick = { showAdultConfirm = false }) { Text("Hủy") } }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun FullscreenWebViewer(
    url: String,
    onClose: () -> Unit,
    onWebViewChanged: (WebView?) -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            (webView?.context as? MainActivity)?.hideFullscreenVideo()
            onWebViewChanged(null)
            webView?.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
    }
    val activity = LocalContext.current as? MainActivity
    BackHandler {
        when {
            activity?.isVideoFullscreen == true -> activity.hideFullscreenVideo()
            webView != null && webView?.canGoBack() == true -> webView?.goBack()
            else -> onClose()
        }
    }
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                webView = this
                onWebViewChanged(this)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.setSupportMultipleWindows(true)
                webChromeClient = object : WebChromeClient() {
                    override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                        if (view == null || callback == null) return
                        (ctx as? MainActivity)?.showFullscreenVideo(view, callback)
                            ?: callback.onCustomViewHidden()
                    }

                    override fun onHideCustomView() {
                        (ctx as? MainActivity)?.hideFullscreenVideo()
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val target = request?.url?.toString().orEmpty()
                        return if (target.startsWith("http://") || target.startsWith("https://")) false else {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target))) }
                            true
                        }
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminScreen(api: SupabaseApi, session: Session, profile: Profile?, config: RemoteConfig, onConfigChanged: (RemoteConfig) -> Unit, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val isCreator = profile?.role == "creator"
    val isAdmin = profile?.role in setOf("admin", "super_admin")
    val isSuperAdmin = profile?.role == "super_admin"
    var selected by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var allThemes by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var users by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var events by remember { mutableStateOf<List<EventItem>>(emptyList()) }
    var history by remember { mutableStateOf<List<ThemeReviewHistory>>(emptyList()) }
    var reputation by remember { mutableStateOf<CreatorReputation?>(null) }
    var reputations by remember { mutableStateOf<List<CreatorReputation>>(emptyList()) }

    fun reload() {
        scope.launch {
            api.pendingThemes(session)
                .onSuccess { pending = it }
                .onFailure { onMessage(it.message ?: "Không tải được theme chờ duyệt") }
            api.themeReviewHistory(session).onSuccess { history = it }
            if (isCreator) api.creatorReputation(session).onSuccess { reputation = it }
            if (isAdmin) {
                api.allThemes(session).onSuccess { allThemes = it }
                api.users(session).onSuccess { users = it }
                api.creatorReputations(session).onSuccess { reputations = it }
                api.allEvents(session).onSuccess { events = it }
            }
        }
    }

    LaunchedEffect(profile?.role) { reload() }

    if (!isCreator && !isAdmin) {
        EmptyState("Không có quyền truy cập", "Trung tâm này chỉ dành cho Nhà sáng tạo và Admin")
        return
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isCreator) {
                item { SectionTitle("Trung tâm Nhà sáng tạo", "Kiểm duyệt có checklist và điểm uy tín") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetric("Chờ duyệt", pending.size, Icons.Default.PendingActions, Modifier.weight(1f))
                        AdminMetric("Uy tín", reputation?.score ?: 50, Icons.Default.WorkspacePremium, Modifier.weight(1f))
                        AdminMetric("Đã duyệt", reputation?.totalReviews ?: 0, Icons.Default.FactCheck, Modifier.weight(1f))
                    }
                }
                item { CreatorReputationCard(reputation) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = selected == 0, onClick = { selected = 0 }, label = { Text("Chờ duyệt") })
                        FilterChip(selected = selected == 1, onClick = { selected = 1 }, label = { Text("Lịch sử của tôi") })
                    }
                }
                if (selected == 0) {
                    if (pending.isEmpty()) item { EmptyState("Không có theme chờ duyệt", "Theme mới sẽ xuất hiện tại đây") }
                    else items(pending, key = { it.id }) { theme ->
                        ReviewCard(
                            t = theme,
                            canEdit = false,
                            onApprove = { checklist ->
                                scope.launch {
                                    api.reviewTheme(session, theme.id, true, checklist = checklist)
                                        .onSuccess { onMessage("Đã duyệt ${theme.title}"); reload() }
                                        .onFailure { onMessage(it.message ?: "Không thể duyệt theme") }
                                }
                            },
                            onReject = { reason, checklist ->
                                scope.launch {
                                    api.reviewTheme(session, theme.id, false, reason, checklist)
                                        .onSuccess { onMessage("Đã từ chối ${theme.title}"); reload() }
                                        .onFailure { onMessage(it.message ?: "Không thể từ chối theme") }
                                }
                            },
                            onSave = { _, _, _, _, _, _ -> }
                        )
                    }
                } else {
                    if (history.isEmpty()) item { EmptyState("Chưa có lịch sử", "Các lượt kiểm duyệt của bạn sẽ hiện tại đây") }
                    else items(history, key = { it.id }) { review ->
                        ReviewHistoryCard(review, canRevoke = false, onRevoke = {})
                    }
                }
            } else {
                item { SectionTitle("Trung tâm điều hành", "Quản lý toàn bộ M4X Universe") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AdminMetric("Chờ duyệt", pending.size, Icons.Default.PendingActions, Modifier.weight(1f))
                        AdminMetric("Người dùng", users.size, Icons.Default.Groups, Modifier.weight(1f))
                        AdminMetric("Lịch sử", history.size, Icons.Default.History, Modifier.weight(1f))
                    }
                }
                item {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Tổng quan", "Theme", "Lịch sử duyệt", "Sự kiện", "Giftcode", "Người dùng", "M4X WEB").forEachIndexed { i, label ->
                            FilterChip(selected = selected == i, onClick = { selected = i }, label = { Text(label) })
                        }
                    }
                }
                when (selected) {
                    0 -> item {
                        FormCard("Công cụ nhanh", Icons.Default.Dashboard) {
                            AdminAction("Phát hành Airdrop", Icons.Default.RocketLaunch) {
                                scope.launch { api.createAirdrop(session).onSuccess { onMessage("Đã phát hành Airdrop") }.onFailure { onMessage(it.message ?: "Lỗi") } }
                            }
                            AdminAction("Tuần sinh nhật Admin 01/08–07/08", Icons.Default.Cake) {
                                scope.launch { api.publishBirthdayWeek(session).onSuccess { onMessage("Đã phát hành tuần sinh nhật") }.onFailure { onMessage(it.message ?: "Lỗi") } }
                            }
                            AdminAction("Mở Boss cộng đồng", Icons.Default.SportsEsports) { onMessage("Boss cộng đồng đã được xếp lịch online") }
                        }
                    }
                    1 -> if (allThemes.isEmpty()) item { EmptyState("Chưa có theme", "Theme người dùng đăng sẽ xuất hiện tại đây") }
                    else items(allThemes, key = { it.id }) { theme ->
                        ReviewCard(
                            t = theme,
                            onApprove = { checklist ->
                                scope.launch { api.reviewTheme(session, theme.id, true, checklist = checklist).onSuccess { reload() }.onFailure { onMessage(it.message ?: "Không thể duyệt theme") } }
                            },
                            onReject = { reason, checklist ->
                                scope.launch { api.reviewTheme(session, theme.id, false, reason, checklist).onSuccess { reload() }.onFailure { onMessage(it.message ?: "Không thể từ chối theme") } }
                            },
                            onSave = { title, desc, drive, price, status, previewUris ->
                                scope.launch {
                                    api.updateThemeByAdmin(session, theme.id, title, desc, drive, price, status, previewUris)
                                        .onSuccess { onMessage("Đã cập nhật theme online"); reload() }
                                        .onFailure { onMessage(it.message ?: "Không thể sửa theme") }
                                }
                            }
                        )
                    }
                    2 -> if (history.isEmpty()) item { EmptyState("Chưa có lịch sử", "Các lượt duyệt sẽ xuất hiện tại đây") }
                    else items(history, key = { it.id }) { review ->
                        ReviewHistoryCard(
                            review = review,
                            canRevoke = isSuperAdmin && review.decision == "approved",
                            onRevoke = { reason ->
                                scope.launch {
                                    api.revokeThemeApproval(session, review.themeId, reason)
                                        .onSuccess { onMessage("Đã thu hồi ${review.themeTitle}"); reload() }
                                        .onFailure { onMessage(it.message ?: "Không thể thu hồi theme") }
                                }
                            }
                        )
                    }
                    3 -> {
                        item { AdminCreateEvent(api, session, onMessage) }
                        items(events) { EventBanner(it) }
                    }
                    4 -> item { AdminGiftCode(api, session, onMessage) }
                    5 -> items(users, key = { it.id }) { user ->
                        UserAdminRow(
                            u = user,
                            reputation = reputations.firstOrNull { it.userId == user.id },
                            canEdit = isSuperAdmin,
                            onRoleChange = { newRole ->
                                scope.launch {
                                    api.setRole(session, user.id, newRole)
                                        .onSuccess { onMessage("Đã đổi quyền ${user.displayName.ifBlank { user.username }} thành $newRole"); reload() }
                                        .onFailure { onMessage(it.message ?: "Không thể đổi quyền") }
                                }
                            }
                        )
                    }
                    6 -> item { AdminWebSettings(api, session, config, onSaved = onConfigChanged, onMessage = onMessage) }
                }
            }
        }
    }
}

@Composable
private fun AdminWebSettings(api: SupabaseApi, session: Session, config: RemoteConfig, onSaved: (RemoteConfig) -> Unit, onMessage: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var football by remember(config) { mutableStateOf(config.webFootballUrl) }
    var movie by remember(config) { mutableStateOf(config.webMovieUrl) }
    var adult by remember(config) { mutableStateOf(config.webAdultUrl) }
    var footballEnabled by remember(config) { mutableStateOf(config.webFootballEnabled) }
    var movieEnabled by remember(config) { mutableStateOf(config.webMovieEnabled) }
    var adultEnabled by remember(config) { mutableStateOf(config.webAdultEnabled) }
    var saving by remember { mutableStateOf(false) }
    FormCard("Cấu hình M4X WEB", Icons.Default.Public) {
        WebSettingRow("Xem Đá Bóng", football, footballEnabled, { football = it }, { footballEnabled = it })
        WebSettingRow("Xem phim", movie, movieEnabled, { movie = it }, { movieEnabled = it })
        WebSettingRow("Xem 18+", adult, adultEnabled, { adult = it }, { adultEnabled = it })
        Button(enabled = !saving, onClick = {
            saving = true
            scope.launch {
                api.updateWebConfig(session, football, movie, adult, footballEnabled, movieEnabled, adultEnabled)
                    .onSuccess { updated -> onSaved(updated); onMessage("Đã cập nhật M4X WEB online") }
                    .onFailure { onMessage(it.message ?: "Không thể lưu cấu hình web") }
                saving = false
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Đang lưu…" else "Lưu thay đổi online") }
    }
}

@Composable
private fun WebSettingRow(title: String, value: String, enabled: Boolean, onValue: (String) -> Unit, onEnabled: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold); Switch(checked = enabled, onCheckedChange = onEnabled) }
        OutlinedTextField(value, onValue, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = enabled, label = { Text("Đường link") })
    }
}

@Composable private fun AdminMetric(label: String, value: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) { ElevatedCard(modifier, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(14.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary); Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text(label, style = MaterialTheme.typography.labelMedium) } } }
@Composable private fun AdminAction(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) { ListItem(headlineContent = { Text(title, fontWeight = FontWeight.Bold) }, leadingContent = { Icon(icon, null) }, trailingContent = { Icon(Icons.Default.ChevronRight, null) }, modifier = Modifier.clickable(onClick = click)) }
@Composable
private fun AdminSafetyReport(t: ThemeItem) {
    var expanded by remember(t.id) { mutableStateOf(false) }
    val metadata = remember(t.clientThemeMetadata) {
        runCatching { JSONObject(t.clientThemeMetadata) }
            .getOrElse { JSONObject() }
    }
    val modules = remember(t.clientModuleReport) {
        runCatching { JSONArray(t.clientModuleReport) }
            .getOrElse { JSONArray() }
    }
    val report = remember(t.clientValidationReport) {
        runCatching { JSONObject(t.clientValidationReport) }
            .getOrElse { JSONObject() }
    }
    val safetyColor = when (t.clientSafetyLevel) {
        "excellent" -> MaterialTheme.colorScheme.primary
        "good" -> MaterialTheme.colorScheme.secondary
        "caution" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val safetyLabel = when (t.clientSafetyLevel) {
        "excellent" -> "Rất an toàn"
        "good" -> "Tốt"
        "caution" -> "Cần kiểm tra"
        else -> "Nguy hiểm"
    }

    Surface(
        color = safetyColor.copy(alpha = 0.10f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = safetyColor)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Điểm an toàn",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Black
                )
                Text(
                    if (t.clientSafetyScore > 0) {
                        "${t.clientSafetyScore}/100 • $safetyLabel"
                    } else {
                        "Chưa có báo cáo"
                    },
                    color = safetyColor,
                    fontWeight = FontWeight.Black
                )
            }
            if (t.clientSafetyScore > 0) {
                LinearProgressIndicator(
                    progress = {
                        t.clientSafetyScore.coerceIn(0, 100) / 100f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                )
            }

            val detectedTitle = metadata.optString("title")
            val creator = metadata.optString("author")
                .ifBlank { metadata.optString("designer") }
            val version = metadata.optString("version")
            val platform = metadata.optString("platform")
            if (detectedTitle.isNotBlank()) {
                Text(
                    "Metadata: $detectedTitle",
                    fontWeight = FontWeight.Bold
                )
            }
            if (creator.isNotBlank()) Text("Tác giả: $creator")
            if (version.isNotBlank() || platform.isNotBlank()) {
                Text(
                    listOfNotNull(
                        version.takeIf { it.isNotBlank() }
                            ?.let { "Phiên bản $it" },
                        platform.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val moduleNames = buildList {
                repeat(modules.length()) { index ->
                    val module = modules.optJSONObject(index)
                    if (module?.optBoolean("present") == true) {
                        add(module.optString("label"))
                    }
                }
            }.filter { it.isNotBlank() }
            if (moduleNames.isNotEmpty()) {
                Text(
                    "Module: ${moduleNames.joinToString()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(onClick = { expanded = !expanded }) {
                Text(
                    if (expanded) {
                        "Ẩn báo cáo chi tiết"
                    } else {
                        "Xem báo cáo chi tiết"
                    }
                )
            }

            if (expanded) {
                val findings = report.optJSONArray("findings")
                val warnings = report.optJSONArray("warnings")
                val errors = report.optJSONArray("errors")
                findings?.let { array ->
                    repeat(array.length()) { index ->
                        Text(
                            "✓ ${array.optString(index)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                warnings?.let { array ->
                    repeat(array.length()) { index ->
                        Text(
                            "⚠ ${array.optString(index)}",
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                errors?.let { array ->
                    repeat(array.length()) { index ->
                        Text(
                            "✕ ${array.optString(index)}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (t.approvedFileSha256.isNotBlank()) {
                    Text(
                        "SHA đã khóa khi duyệt: ${t.approvedFileSha256}",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(
    t: ThemeItem,
    onApprove: (ThemeReviewChecklist) -> Unit,
    onReject: (String, ThemeReviewChecklist) -> Unit,
    onSave: (String, String, String, Int, String, List<Uri>) -> Unit,
    canEdit: Boolean = true
) {
    var editing by remember { mutableStateOf(false) }
    var showRejectDialog by remember(t.id) { mutableStateOf(false) }
    var rejectReason by remember(t.id) { mutableStateOf("") }
    var checklist by remember(t.id) { mutableStateOf(ThemeReviewChecklist()) }
    var newPreviewUris by remember(t.id) { mutableStateOf<List<Uri>>(emptyList()) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) {
        newPreviewUris = it.take(5)
    }

    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column {
            if (t.previewUrl.isNotBlank()) {
                AsyncImage(t.previewUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(150.dp))
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(t.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("${t.status.uppercase()} • ${t.coinPrice} M4X COIN")

                val validationColor = when (t.clientValidationStatus) {
                    "passed" -> MaterialTheme.colorScheme.primary
                    "warning" -> MaterialTheme.colorScheme.tertiary
                    "failed" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val validationLabel = when (t.clientValidationStatus) {
                    "passed" -> "Đã qua kiểm tra Rust trên thiết bị"
                    "warning" -> "Rust phát hiện cảnh báo"
                    "failed" -> "Rust từ chối file"
                    else -> "Chưa kiểm tra Rust (có thể là link Drive)"
                }
                Text(validationLabel, color = validationColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                if (t.clientValidationMessage.isNotBlank()) Text(t.clientValidationMessage, style = MaterialTheme.typography.bodySmall)
                if (t.clientFileSizeBytes > 0L) {
                    val sizeMb = t.clientFileSizeBytes.toDouble() / 1024.0 / 1024.0
                    Text(
                        "Dung lượng: ${java.lang.String.format(java.util.Locale.US, "%.2f", sizeMb)} MB" +
                            if (t.clientFileSha256.isNotBlank()) " • SHA-256: ${t.clientFileSha256.take(12)}…" else "",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                AdminSafetyReport(t)
                Text("Checklist bắt buộc trước khi duyệt", fontWeight = FontWeight.Black)
                ReviewChecklistRow("Ảnh xem trước đầy đủ, rõ ràng", checklist.previewOk) { checklist = checklist.copy(previewOk = it) }
                ReviewChecklistRow("Link tải hoặc file hoạt động", checklist.downloadOk) { checklist = checklist.copy(downloadOk = it) }
                ReviewChecklistRow("Đúng phiên bản MIUI/HyperOS", checklist.compatibilityOk) { checklist = checklist.copy(compatibilityOk = it) }
                ReviewChecklistRow("Không chứa nội dung vi phạm", checklist.safeContent) { checklist = checklist.copy(safeContent = it) }
                ReviewChecklistRow("Không trùng theme đã có", checklist.notDuplicate) { checklist = checklist.copy(notDuplicate = it) }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApprove(checklist) },
                        enabled = checklist.completed &&
                            t.clientValidationStatus != "failed" &&
                            (t.clientSafetyScore == 0 || t.clientSafetyScore >= 60),
                        modifier = Modifier.weight(1f)
                    ) { Text("Duyệt") }
                    OutlinedButton(onClick = { showRejectDialog = true }, modifier = Modifier.weight(1f)) { Text("Từ chối") }
                    if (canEdit) IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, "Sửa") }
                }
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon = { Icon(Icons.Default.ReportProblem, null) },
            title = { Text("Lý do từ chối") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Người đăng sẽ nhận thông báo này và có thể sửa để gửi lại.")
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it.take(500) },
                        label = { Text("Nội dung cần sửa") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = rejectReason.trim().length >= 5,
                    onClick = {
                        onReject(rejectReason.trim(), checklist)
                        rejectReason = ""
                        showRejectDialog = false
                    }
                ) { Text("Xác nhận từ chối") }
            },
            dismissButton = { TextButton(onClick = { showRejectDialog = false }) { Text("Hủy") } }
        )
    }

    if (editing && canEdit) {
        var title by remember(t.id) { mutableStateOf(t.title) }
        var desc by remember(t.id) { mutableStateOf(t.description) }
        var drive by remember(t.id) { mutableStateOf(t.driveUrl) }
        var price by remember(t.id) { mutableStateOf(t.coinPrice.toString()) }
        var status by remember(t.id) { mutableStateOf(t.status) }

        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Sửa theme") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(title, { title = it }, label = { Text("Tên theme") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Mô tả") })
                    OutlinedTextField(drive, { drive = it }, label = { Text("Link Drive") })
                    OutlinedTextField(price, { price = it.filter(Char::isDigit) }, label = { Text("Giá M4X COIN") })
                    Text("Ảnh hiện tại", fontWeight = FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val currentImages = if (t.previewUrls.isNotEmpty()) t.previewUrls else listOfNotNull(t.previewUrl.takeIf { it.isNotBlank() })
                        currentImages.forEach { url ->
                            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp)))
                        }
                    }
                    OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PhotoLibrary, null); Text(" Thay ảnh xem trước (${newPreviewUris.size}/5)")
                    }
                    if (newPreviewUris.isNotEmpty()) {
                        Text("Ảnh mới sẽ thay toàn bộ ảnh cũ", color = MaterialTheme.colorScheme.secondary)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            newPreviewUris.forEach { uri -> AsyncImage(uri, null, contentScale = ContentScale.Crop, modifier = Modifier.size(82.dp).clip(RoundedCornerShape(14.dp))) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("pending", "approved", "rejected").forEach { st -> FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) }) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(title, desc, drive, price.toIntOrNull() ?: 0, status, newPreviewUris)
                    newPreviewUris = emptyList(); editing = false
                }) { Text("Lưu online") }
            },
            dismissButton = { TextButton(onClick = { newPreviewUris = emptyList(); editing = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun ReviewChecklistRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onChecked(!checked) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CreatorReputationCard(reputation: CreatorReputation?) {
    val rep = reputation ?: CreatorReputation("", 50, 0, 0, 0, 0)
    FormCard("Điểm uy tín Nhà sáng tạo", Icons.Default.WorkspacePremium) {
        Text("${rep.score}/100 • ${rep.level}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        LinearProgressIndicator(progress = { rep.score.coerceIn(0, 100) / 100f }, modifier = Modifier.fillMaxWidth())
        Text("Duyệt: ${rep.approvedCount} • Từ chối: ${rep.rejectedCount} • Bị thu hồi: ${rep.revokedCount}")
        Text("Duyệt đúng được cộng điểm; theme bị Super Admin thu hồi sẽ bị trừ điểm.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ReviewHistoryCard(
    review: ThemeReviewHistory,
    canRevoke: Boolean,
    onRevoke: (String) -> Unit
) {
    var showRevoke by remember(review.id) { mutableStateOf(false) }
    var reason by remember(review.id) { mutableStateOf("") }
    val color = when (review.decision) {
        "approved" -> MaterialTheme.colorScheme.primary
        "revoked" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(review.themeTitle, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Text("Người đăng: ${review.ownerName}", style = MaterialTheme.typography.bodySmall)
                }
                Text(review.decision.uppercase(), color = color, fontWeight = FontWeight.Black)
            }
            Text("Người duyệt: ${review.reviewerName} • ${review.reviewerRole}")
            Text(review.createdAt.replace('T', ' ').take(19), style = MaterialTheme.typography.labelSmall)
            if (review.reason.isNotBlank()) Text("Lý do: ${review.reason}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Checklist: ${listOf(review.checklist.previewOk, review.checklist.downloadOk, review.checklist.compatibilityOk, review.checklist.safeContent, review.checklist.notDuplicate).count { it }}/5",
                style = MaterialTheme.typography.labelMedium
            )
            if (canRevoke) OutlinedButton(onClick = { showRevoke = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.RemoveCircle, null); Text(" Thu hồi theme")
            }
        }
    }
    if (showRevoke) {
        AlertDialog(
            onDismissRequest = { showRevoke = false },
            title = { Text("Thu hồi theme đã duyệt") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(500) },
                    label = { Text("Lý do thu hồi") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(enabled = reason.trim().length >= 5, onClick = { onRevoke(reason.trim()); showRevoke = false; reason = "" }) { Text("Thu hồi") }
            },
            dismissButton = { TextButton(onClick = { showRevoke = false }) { Text("Hủy") } }
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
@Composable
private fun UserAdminRow(
    u: Profile,
    reputation: CreatorReputation? = null,
    canEdit: Boolean,
    onRoleChange: (String) -> Unit
) {
    val roleLabel = when (u.role) {
        "super_admin" -> "Super Admin"
        "admin" -> "Admin"
        "creator" -> "Nhà sáng tạo"
        "banned" -> "Đã khóa"
        else -> "Người dùng"
    }
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(u.displayName.take(1).uppercase().ifBlank { "M" }, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.displayName.ifBlank { u.username }, fontWeight = FontWeight.Bold)
                Text("@${u.username} • $roleLabel • ${u.points} coin", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (u.role == "creator") {
                    Text(
                        "Uy tín: ${reputation?.score ?: 50}/100 • ${reputation?.level ?: "Đang phát triển"}",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (canEdit && u.role != "super_admin") {
                Column(horizontalAlignment = Alignment.End) {
                    when (u.role) {
                        "user", "banned" -> {
                            TextButton(onClick = { onRoleChange("creator") }) { Text("Lên Sáng tạo") }
                            TextButton(onClick = { onRoleChange("admin") }) { Text("Lên Admin") }
                        }
                        "creator" -> {
                            TextButton(onClick = { onRoleChange("user") }) { Text("Hạ quyền") }
                            TextButton(onClick = { onRoleChange("admin") }) { Text("Lên Admin") }
                        }
                        "admin" -> {
                            TextButton(onClick = { onRoleChange("creator") }) { Text("Xuống Sáng tạo") }
                            TextButton(onClick = { onRoleChange("user") }) { Text("Hạ Admin") }
                        }
                    }
                }
            }
        }
    }
}

private fun inventoryMetadata(item: InventoryItem?, key: String): String {
    if (item == null) return ""
    return runCatching { JSONObject(item.metadata).optString(key) }.getOrDefault("")
}

private fun inventoryColor(item: InventoryItem?, fallback: Color): Color {
    val value = inventoryMetadata(item, "color")
    return if (value.isBlank()) fallback
    else runCatching { Color(android.graphics.Color.parseColor(value)) }.getOrDefault(fallback)
}

private fun profileBackgroundBrush(item: InventoryItem?): Brush {
    return when (inventoryMetadata(item, "background")) {
        "ocean" -> Brush.linearGradient(listOf(Color(0xFF003B73), Color(0xFF008B9A)))
        "sunset" -> Brush.linearGradient(listOf(Color(0xFF7A1F5C), Color(0xFFE96B3C)))
        else -> Brush.linearGradient(listOf(Color(0xFF4A247B), Color(0xFF075D72)))
    }
}

@Composable
private fun ProfileScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    config: RemoteConfig,
    isAdmin: Boolean,
    checkingUpdate: Boolean,
    availableUpdate: AppUpdateInfo?,
    onCheckUpdate: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenGames: () -> Unit,
    onProfileChanged: (Profile) -> Unit,
    onOpenAdmin: () -> Unit,
    onLogout: () -> Unit,
    onMessage: (String) -> Unit
) {
    var mine by remember { mutableStateOf<List<ThemeItem>>(emptyList()) }
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var avatarLoading by remember { mutableStateOf(false) }
    var equipLoadingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reloadInventory() {
        scope.launch {
            api.inventory(session)
                .onSuccess { inventory = it }
                .onFailure { onMessage(it.message ?: "Không tải được kho vật phẩm") }
        }
    }

    fun toggleInventoryItem(item: InventoryItem) {
        if (equipLoadingId != null) return
        equipLoadingId = item.id
        scope.launch {
            api.equipInventoryItem(session, item.id)
                .onSuccess { equipped ->
                    onMessage(if (equipped) "Đã sử dụng ${item.name}" else "Đã bỏ sử dụng ${item.name}")
                    api.inventory(session).onSuccess { inventory = it }
                }
                .onFailure { onMessage(it.message ?: "Không thể sử dụng vật phẩm") }
            equipLoadingId = null
        }
    }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && !avatarLoading) {
            scope.launch {
                avatarLoading = true
                api.updateAvatar(session, uri)
                    .onSuccess {
                        onProfileChanged(it)
                        onMessage("Đã cập nhật ảnh đại diện")
                    }
                    .onFailure { onMessage(it.message ?: "Không thể đổi ảnh đại diện") }
                avatarLoading = false
            }
        }
    }

    fun reloadMine() {
        scope.launch {
            api.myThemes(session)
                .onSuccess { mine = it }
                .onFailure { onMessage(it.message ?: "Không tải được theme của bạn") }
        }
    }

    LaunchedEffect(Unit) {
        reloadMine()
        reloadInventory()
    }
    val downloads = mine.sumOf { it.downloads }
    val roleText = when (profile?.role) {
        "super_admin" -> "SUPER ADMIN"
        "admin" -> "ADMIN"
        "creator" -> "NHÀ SÁNG TẠO"
        else -> "THÀNH VIÊN"
    }
    val equippedFrame = inventory.firstOrNull { it.type == "avatar_frame" && it.equipped }
    val equippedNameColor = inventory.firstOrNull { it.type == "name_color" && it.equipped }
    val equippedBackground = inventory.firstOrNull { it.type == "profile_background" && it.equipped }
    val equippedEffect = inventory.firstOrNull { it.type == "profile_effect" && it.equipped }
    val equippedBadge = inventory.firstOrNull { it.type == "badge" && it.equipped }
    val displayNameColor = inventoryColor(equippedNameColor, Color.White)
    val avatarFrameColor = if (equippedFrame != null) Color(0xFF2CEBFF) else Color.White.copy(alpha = .22f)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(30.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(profileBackgroundBrush(equippedBackground))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    if (equippedEffect != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            Text("✦  ✧  ☄", color = Color.White.copy(alpha = .88f), fontWeight = FontWeight.Black)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(92.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.White.copy(alpha = .14f),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (equippedFrame != null) 4.dp else 2.dp,
                                    avatarFrameColor
                                )
                            ) {
                                if (profile?.avatarUrl.isNullOrBlank()) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, null, Modifier.size(52.dp))
                                    }
                                } else {
                                    AsyncImage(
                                        model = profile?.avatarUrl,
                                        contentDescription = "Ảnh đại diện",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                            FilledIconButton(
                                onClick = { avatarPicker.launch("image/*") },
                                enabled = !avatarLoading,
                                modifier = Modifier.size(34.dp).align(Alignment.BottomEnd),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (avatarLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                else Icon(Icons.Default.PhotoCamera, "Đổi ảnh đại diện", Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    profile?.displayName?.ifBlank { "M4X Member" } ?: "M4X Member",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                    color = displayNameColor
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Verified, null, tint = Color(0xFF32D6FF), modifier = Modifier.size(22.dp))
                            }
                            Text("@${profile?.username.orEmpty()}", color = Color.White.copy(alpha = .82f))
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                ProfileBadge("VIP 1")
                                ProfileBadge("LV.3")
                                ProfileBadge(roleText)
                                if (equippedBadge != null) ProfileBadge(equippedBadge.name)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileStat("M4X COIN", "${profile?.points ?: 0}", Modifier.weight(1f))
                        ProfileStat("Theme", mine.size.toString(), Modifier.weight(1f))
                        ProfileStat("Lượt tải", downloads.toString(), Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickFeatureCard(
                    title = "Cửa hàng M4X",
                    subtitle = "Dùng coin mua vật phẩm",
                    icon = Icons.Default.Storefront,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenShop
                )
                QuickFeatureCard(
                    title = "Minigame",
                    subtitle = "Chơi để kiếm coin",
                    icon = Icons.Default.SportsEsports,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenGames
                )
            }
        }

        item { SectionTitle("Kho vật phẩm cá nhân", "Khung avatar, màu tên, hiệu ứng và vật phẩm đã mua") }
        item {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (inventory.isEmpty()) {
                    InventoryCard("Chưa có vật phẩm", "Mở Cửa hàng M4X")
                } else {
                    inventory.forEach { item ->
                        EquippedInventoryCard(
                            item = item,
                            loading = equipLoadingId == item.id,
                            onToggle = { toggleInventoryItem(item) }
                        )
                    }
                }
            }
        }
        val rejectedThemes = mine.filter { it.status == "rejected" }
        if (rejectedThemes.isNotEmpty()) {
            item { SectionTitle("Theme cần sửa", "Xem lý do, chỉnh thông tin và gửi lại xét duyệt") }
            items(rejectedThemes, key = { "rejected-${it.id}" }) { theme ->
                RejectedThemeResubmitCard(
                    theme = theme,
                    onResubmit = { title, description, driveUrl, coinPrice ->
                        scope.launch {
                            api.resubmitTheme(session, theme.id, title, description, driveUrl, coinPrice)
                                .onSuccess { onMessage("Đã gửi lại ${theme.title} để xét duyệt"); reloadMine() }
                                .onFailure { onMessage(it.message ?: "Không thể gửi lại theme") }
                        }
                    }
                )
            }
        }

        item {
            FormCard("Thành tích", Icons.Default.EmojiEvents) {
                Text("Theme đã đăng: ${mine.size}")
                Text("Tổng lượt tải: $downloads")
                Text("Vai trò: $roleText")
                Text("Xếp hạng tuần: Đang cập nhật online")
            }
        }
        item {
            FormCard("Cập nhật ứng dụng", Icons.Default.SystemUpdate) {
                Text("Phiên bản hiện tại: ${BuildConfig.VERSION_NAME}")
                Text(
                    availableUpdate?.let { "Có bản mới ${it.versionName}" }
                        ?: "Kiểm tra bản phát hành mới của M4X Theme",
                    color = if (availableUpdate != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onCheckUpdate, enabled = !checkingUpdate, modifier = Modifier.fillMaxWidth()) {
                    if (checkingUpdate) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, null)
                    Text(if (checkingUpdate) " Đang kiểm tra..." else " Kiểm tra cập nhật")
                }
            }
        }
        if (isAdmin) {
            item {
                Button(
                    onClick = onOpenAdmin,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null)
                    Text(if (profile?.role == "creator") " Mở khu duyệt Theme" else " Mở trung tâm Admin")
                }
            }
        }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Icon(Icons.Default.Logout, null)
                Text(" Đăng xuất")
            }
        }
    }
}

@Composable
private fun ThemeReviewNotificationsDialog(
    notifications: List<ThemeReviewNotification>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Notifications, null) },
        title = { Text("Thông báo xét duyệt") },
        text = {
            if (notifications.isEmpty()) {
                Text("Chưa có thông báo theme.")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { n ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (n.readAt.isBlank()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(n.title, fontWeight = FontWeight.Black)
                                Text(n.message, style = MaterialTheme.typography.bodySmall)
                                if (n.reason.isNotBlank()) Text("Lý do: ${n.reason}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                Text(n.createdAt.replace('T', ' ').take(19), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onMarkAllRead(); onDismiss() }) { Text("Đánh dấu đã đọc") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } }
    )
}

@Composable
private fun RejectedThemeResubmitCard(
    theme: ThemeItem,
    onResubmit: (String, String, String, Int) -> Unit
) {
    var editing by remember(theme.id) { mutableStateOf(false) }
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(theme.title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("Bị từ chối", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            Text(theme.rejectReason.ifBlank { "Theme cần được chỉnh sửa trước khi gửi lại." })
            Button(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.EditNote, null); Text(" Sửa và gửi lại")
            }
        }
    }
    if (editing) {
        var title by remember(theme.id) { mutableStateOf(theme.title) }
        var description by remember(theme.id) { mutableStateOf(theme.description) }
        var driveUrl by remember(theme.id) { mutableStateOf(theme.driveUrl) }
        var coinPrice by remember(theme.id) { mutableStateOf(theme.coinPrice.toString()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Sửa và gửi lại theme") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Lý do cần sửa: ${theme.rejectReason}", color = MaterialTheme.colorScheme.error)
                    OutlinedTextField(title, { title = it }, label = { Text("Tên theme") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text("Mô tả") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(driveUrl, { driveUrl = it }, label = { Text("Link tải/Drive") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(coinPrice, { coinPrice = it.filter(Char::isDigit) }, label = { Text("Giá M4X Coin") }, modifier = Modifier.fillMaxWidth())
                    Text("Để thay file hoặc ảnh xem trước, hãy đăng bản mới trong mục Đăng.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(enabled = title.isNotBlank(), onClick = {
                    onResubmit(title, description, driveUrl, coinPrice.toIntOrNull() ?: 0)
                    editing = false
                }) { Text("Gửi lại xét duyệt") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun ProfileBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = .10f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = .24f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun QuickFeatureCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            Text(title, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ShopScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var products by remember { mutableStateOf<List<ShopItem>>(emptyList()) }
    var inventory by remember { mutableStateOf<List<InventoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var buyingId by remember { mutableStateOf<String?>(null) }
    var balance by remember(profile?.points) { mutableLongStateOf(profile?.points ?: 0L) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            api.shopItems(session).onSuccess { products = it }.onFailure { onMessage(it.message ?: "Không tải được cửa hàng") }
            api.inventory(session).onSuccess { inventory = it }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Quay lại") }
                Column(Modifier.weight(1f)) {
                    Text("Cửa hàng M4X", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Dùng M4X COIN để sở hữu vật phẩm", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Paid, null, tint = Color(0xFFFFB800), modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Số dư hiện tại", style = MaterialTheme.typography.labelLarge)
                        Text("$balance M4X COIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        if (loading) {
            item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (products.isEmpty()) {
            item { EmptyState("Cửa hàng đang trống", "Admin chưa đăng vật phẩm mới") }
        } else {
            items(products, key = { it.id }) { product ->
                val ownedItem = inventory.firstOrNull { it.itemId == product.id }
                val owned = ownedItem != null
                ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            if (product.imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(product.name, fontWeight = FontWeight.Black)
                            Text(friendlyItemType(product.type), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (product.limited) Text("Vật phẩm giới hạn", color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                        }
                        val consumable = product.type == "rescue_card"
                        Button(
                            enabled = buyingId == null && (!owned && balance >= product.price || owned && !consumable),
                            onClick = {
                                buyingId = product.id
                                scope.launch {
                                    if (ownedItem == null) {
                                        api.purchaseShopItem(session, product.id)
                                            .onSuccess { newBalance ->
                                                balance = newBalance
                                                onCoinChanged(newBalance)
                                                onMessage(
                                                    if (consumable) "Đã mua ${product.name}. Dùng trong Bản đồ kho báu."
                                                    else "Đã mua ${product.name}. Bấm Dùng để kích hoạt."
                                                )
                                                api.inventory(session).onSuccess { inventory = it }
                                            }
                                            .onFailure { onMessage(it.message ?: "Không thể mua vật phẩm") }
                                    } else if (!consumable) {
                                        api.equipInventoryItem(session, ownedItem.id)
                                            .onSuccess { equipped ->
                                                onMessage(
                                                    if (equipped) "Đã sử dụng ${product.name}"
                                                    else "Đã bỏ sử dụng ${product.name}"
                                                )
                                                api.inventory(session).onSuccess { inventory = it }
                                            }
                                            .onFailure { onMessage(it.message ?: "Không thể sử dụng vật phẩm") }
                                    }
                                    buyingId = null
                                }
                            }
                        ) {
                            if (buyingId == product.id) {
                                CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    when {
                                        consumable && owned -> Icons.Default.Map
                                        ownedItem?.equipped == true -> Icons.Default.CheckCircle
                                        owned -> Icons.Default.PlayArrow
                                        else -> Icons.Default.Paid
                                    },
                                    null,
                                    Modifier.size(18.dp)
                                )
                            }
                            Text(
                                when {
                                    consumable && owned -> " Đã có"
                                    ownedItem?.equipped == true -> " Bỏ dùng"
                                    owned -> " Dùng"
                                    else -> " ${product.price}"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniGamesScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var balance by remember(profile?.points) { mutableLongStateOf(profile?.points ?: 0L) }
    var selectedNumber by remember { mutableIntStateOf(1) }
    var loadingGame by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<MiniGameResult?>(null) }
    val scope = rememberCoroutineScope()

    fun play(game: String, choice: Int) {
        if (loadingGame != null) return
        loadingGame = game
        scope.launch {
            api.playMiniGame(session, game, choice)
                .onSuccess {
                    result = it
                    balance = it.balance
                    onCoinChanged(it.balance)
                }
                .onFailure { onMessage(it.message ?: "Không thể chơi minigame") }
            loadingGame = null
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Quay lại") }
                Column(Modifier.weight(1f)) {
                    Text("Minigame M4X", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Chơi miễn phí để kiếm M4X COIN", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Paid, null, tint = Color(0xFFFFB800))
                    Spacer(Modifier.width(10.dp))
                    Text("Số dư: $balance M4X COIN", fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            FormCard("Đoán số M4X", Icons.Default.LooksOne) {
                Text("Chọn một số từ 1 đến 5. Đoán đúng nhận 50 coin, chưa đúng vẫn nhận 5 coin.")
                Text("Tối đa 5 lượt/ngày", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { number ->
                        if (selectedNumber == number) {
                            Button(onClick = { selectedNumber = number }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("$number") }
                        } else {
                            OutlinedButton(onClick = { selectedNumber = number }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("$number") }
                        }
                    }
                }
                Button(
                    onClick = { play("number_guess", selectedNumber) },
                    enabled = loadingGame == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loadingGame == "number_guess") CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Casino, null)
                    Text(" Đoán số $selectedNumber")
                }
            }
        }
        item {
            FormCard("Lật thẻ may mắn", Icons.Default.Style) {
                Text("Chọn một thẻ để nhận ngẫu nhiên từ 5 đến 50 M4X COIN.")
                Text("Tối đa 3 lượt/ngày", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (1..3).forEach { card ->
                        ElevatedButton(
                            onClick = { play("lucky_card", card) },
                            enabled = loadingGame == null,
                            modifier = Modifier.weight(1f).height(86.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Style, null)
                                Text("Thẻ $card", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (loadingGame == "lucky_card") LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
        item {
            Text(
                "Kết quả được quyết định trên máy chủ Supabase. Mỗi minigame có giới hạn lượt để tránh gian lận.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
        }
    }

    result?.let { gameResult ->
        AlertDialog(
            onDismissRequest = { result = null },
            icon = { Icon(Icons.Default.AutoAwesome, null) },
            title = { Text(if (gameResult.reward > 0) "+${gameResult.reward} M4X COIN" else "Kết quả minigame") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(gameResult.message)
                    Text("Còn ${gameResult.remaining} lượt hôm nay")
                    Text("Số dư mới: ${gameResult.balance} coin", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { Button(onClick = { result = null }) { Text("Tiếp tục") } }
        )
    }
}

private fun friendlyItemType(type: String): String = when (type) {
    "avatar_frame" -> "Khung avatar"
    "name_color" -> "Màu tên"
    "profile_effect" -> "Hiệu ứng hồ sơ"
    "badge" -> "Huy hiệu"
    "profile_background" -> "Nền hồ sơ"
    "rescue_card" -> "Vật phẩm dùng một lần"
    else -> type.replace('_', ' ').ifBlank { "Vật phẩm M4X" }
}

@Composable
private fun ProfileStat(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = Color.White.copy(alpha = .1f), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
@Composable
private fun EquippedInventoryCard(
    item: InventoryItem,
    loading: Boolean,
    onToggle: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(180.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                shape = RoundedCornerShape(16.dp),
                color = if (item.equipped) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer
            ) {
                if (item.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (item.equipped) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                            null,
                            Modifier.size(32.dp),
                            tint = if (item.equipped) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            Text(item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(friendlyItemType(item.type), style = MaterialTheme.typography.labelSmall)
            FilledTonalButton(
                onClick = onToggle,
                enabled = !loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (item.equipped) Icons.Default.Close else Icons.Default.PlayArrow,
                        null,
                        Modifier.size(17.dp)
                    )
                }
                Text(if (item.equipped) " Bỏ dùng" else " Dùng")
            }
        }
    }
}

@Composable private fun InventoryCard(name: String, type: String) { ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.width(150.dp)) { Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AutoAwesome, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(8.dp)); Text(name, fontWeight = FontWeight.Bold, maxLines = 1); Text(type, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun EmptyState(title: String, subtitle: String) { Column(Modifier.fillMaxWidth().padding(vertical = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Inbox, null, Modifier.size(60.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(14.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
