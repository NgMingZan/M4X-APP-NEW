package com.m4xtheme.app

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val ARENA_WIDTH = 1200f
private const val ARENA_HEIGHT = 720f
private const val MATCH_SECONDS = 180f
private const val KILL_LIMIT = 20
private const val PLAYER_ID = 0

private enum class ArenaPage {
    LOBBY,
    MATCHMAKING,
    SHOP,
    MATCH,
    RESULT
}

private enum class BotMode {
    HUNT,
    STRAFE,
    FLANK,
    COVER,
    HEAL,
    RELOAD,
    DODGE
}

private enum class PickupType {
    MEDKIT,
    ARMOR,
    AMMO
}

private data class ArenaWeapon(
    val name: String,
    val damage: Float,
    val fireInterval: Float,
    val magazine: Int,
    val reserve: Int,
    val bulletSpeed: Float,
    val range: Float,
    val spread: Float
)

private data class ArenaLoadout(
    val weapon: ArenaWeapon,
    val maxArmor: Float,
    val moveSpeed: Float,
    val medkits: Int
)

private data class ArenaActor(
    val id: Int,
    val name: String,
    val isPlayer: Boolean,
    val position: Offset,
    val velocity: Offset = Offset.Zero,
    val aim: Offset = Offset(1f, 0f),
    val health: Float = 100f,
    val armor: Float = 50f,
    val maxArmor: Float = 50f,
    val moveSpeed: Float = 205f,
    val kills: Int = 0,
    val deaths: Int = 0,
    val ammo: Int = 30,
    val reserveAmmo: Int = 120,
    val magazine: Int = 30,
    val medkits: Int = 1,
    val fireCooldown: Float = 0f,
    val reloadRemaining: Float = 0f,
    val respawnRemaining: Float = 0f,
    val botMode: BotMode = BotMode.HUNT,
    val botModeTimer: Float = 0f,
    val botSkill: Float = 0.82f,
    val weapon: ArenaWeapon = defaultArenaWeapon(),
    val lastDamagedBy: Int = -1
) {
    val alive: Boolean
        get() = health > 0f && respawnRemaining <= 0f
}

private data class ArenaBullet(
    val ownerId: Int,
    val position: Offset,
    val previousPosition: Offset,
    val velocity: Offset,
    val damage: Float,
    val life: Float
)

private data class ArenaParticle(
    val position: Offset,
    val velocity: Offset,
    val life: Float,
    val radius: Float,
    val color: Color
)

private data class ArenaPickup(
    val id: Int,
    val type: PickupType,
    val position: Offset,
    val respawnRemaining: Float = 0f
)

private data class ArenaResult(
    val rank: Int,
    val kills: Int,
    val deaths: Int,
    val winner: String
)

private val arenaObstacles = listOf(
    Rect(110f, 95f, 285f, 175f),
    Rect(405f, 70f, 540f, 215f),
    Rect(785f, 85f, 1035f, 155f),
    Rect(230f, 285f, 380f, 440f),
    Rect(510f, 300f, 695f, 390f),
    Rect(825f, 270f, 965f, 445f),
    Rect(70f, 535f, 310f, 610f),
    Rect(440f, 505f, 565f, 665f),
    Rect(720f, 525f, 890f, 610f),
    Rect(1015f, 500f, 1140f, 650f)
)

private val spawnPoints = listOf(
    Offset(70f, 70f),
    Offset(600f, 55f),
    Offset(1125f, 70f),
    Offset(95f, 345f),
    Offset(1100f, 350f),
    Offset(75f, 670f),
    Offset(350f, 665f),
    Offset(660f, 660f),
    Offset(930f, 665f),
    Offset(1130f, 665f)
)

private fun defaultArenaWeapon() = ArenaWeapon(
    name = "M4X-R",
    damage = 20f,
    fireInterval = 0.145f,
    magazine = 30,
    reserve = 120,
    bulletSpeed = 790f,
    range = 580f,
    spread = 0.045f
)

private fun smgWeapon() = ArenaWeapon(
    name = "SMG-7",
    damage = 13f,
    fireInterval = 0.085f,
    magazine = 36,
    reserve = 144,
    bulletSpeed = 760f,
    range = 430f,
    spread = 0.075f
)

private fun p90Weapon() = ArenaWeapon(
    name = "P90-X",
    damage = 14f,
    fireInterval = 0.072f,
    magazine = 50,
    reserve = 150,
    bulletSpeed = 770f,
    range = 470f,
    spread = 0.065f
)

private fun sniperWeapon() = ArenaWeapon(
    name = "Viper-S",
    damage = 54f,
    fireInterval = 0.75f,
    magazine = 8,
    reserve = 32,
    bulletSpeed = 1080f,
    range = 900f,
    spread = 0.012f
)

@Composable
fun ArenaGameScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var page by remember { mutableStateOf(ArenaPage.LOBBY) }
    var balance by remember(profile?.points) {
        mutableLongStateOf(profile?.points ?: 0L)
    }
    var inventory by remember {
        mutableStateOf<List<InventoryItem>>(emptyList())
    }
    var shopItems by remember {
        mutableStateOf<List<ShopItem>>(emptyList())
    }
    var loading by remember { mutableStateOf(false) }
    var matchmakingSeconds by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ArenaResult?>(null) }
    val scope = rememberCoroutineScope()

    fun refreshStore() {
        scope.launch {
            loading = true
            val shopResult = api.shopItems(session)
            val inventoryResult = api.inventory(session)

            shopResult.onSuccess { items ->
                shopItems = items.filter {
                    it.type.startsWith("arena_")
                }
            }.onFailure {
                onMessage(it.message ?: "Không tải được cửa hàng Arena")
            }

            inventoryResult.onSuccess { inventory = it }
                .onFailure {
                    onMessage(it.message ?: "Không tải được kho Arena")
                }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshStore()
    }

    LaunchedEffect(page) {
        if (page == ArenaPage.MATCHMAKING) {
            matchmakingSeconds = 0
            repeat(4) {
                delay(1_000)
                matchmakingSeconds += 1
            }
            page = ArenaPage.MATCH
        }
    }

    BackHandler(enabled = page != ArenaPage.LOBBY) {
        page = when (page) {
            ArenaPage.MATCH -> ArenaPage.LOBBY
            ArenaPage.RESULT -> ArenaPage.LOBBY
            else -> ArenaPage.LOBBY
        }
    }

    when (page) {
        ArenaPage.LOBBY -> ArenaLobby(
            balance = balance,
            inventory = inventory,
            loading = loading,
            onBack = onBack,
            onPlay = { page = ArenaPage.MATCHMAKING },
            onShop = { page = ArenaPage.SHOP },
            onRefresh = { refreshStore() }
        )

        ArenaPage.MATCHMAKING -> ArenaMatchmaking(
            seconds = matchmakingSeconds,
            onCancel = { page = ArenaPage.LOBBY }
        )

        ArenaPage.SHOP -> ArenaShop(
            balance = balance,
            items = shopItems,
            inventory = inventory,
            loading = loading,
            onBack = { page = ArenaPage.LOBBY },
            onBuy = { item ->
                scope.launch {
                    loading = true
                    api.purchaseShopItem(session, item.id)
                        .onSuccess { newBalance ->
                            balance = newBalance
                            onCoinChanged(newBalance)
                            onMessage("Đã mua ${item.name}")
                            refreshStore()
                        }
                        .onFailure {
                            onMessage(it.message ?: "Không thể mua vật phẩm")
                            loading = false
                        }
                }
            },
            onEquip = { item ->
                scope.launch {
                    loading = true
                    api.equipInventoryItem(session, item.id)
                        .onSuccess {
                            onMessage("Đã trang bị ${item.name}")
                            refreshStore()
                        }
                        .onFailure {
                            onMessage(it.message ?: "Không thể trang bị")
                            loading = false
                        }
                }
            }
        )

        ArenaPage.MATCH -> ArenaMatch(
            profile = profile,
            inventory = inventory,
            onQuit = { page = ArenaPage.LOBBY },
            onFinished = {
                result = it
                page = ArenaPage.RESULT
            }
        )

        ArenaPage.RESULT -> ArenaResultScreen(
            result = result ?: ArenaResult(10, 0, 0, "Bot"),
            onReplay = { page = ArenaPage.MATCHMAKING },
            onLobby = { page = ArenaPage.LOBBY }
        )
    }
}

@Composable
private fun ArenaLobby(
    balance: Long,
    inventory: List<InventoryItem>,
    loading: Boolean,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onRefresh: () -> Unit
) {
    val equippedWeapon = inventory.firstOrNull {
        it.type == "arena_weapon" && it.equipped
    }?.name ?: "M4X-R mặc định"
    val equippedArmor = inventory.firstOrNull {
        it.type == "arena_armor" && it.equipped
    }?.name ?: "Giáp tiêu chuẩn"

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07101D),
                        Color(0xFF11152E),
                        Color(0xFF05070D)
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        "Quay lại",
                        tint = Color.White
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "M4X ARENA",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Bắn hay • sống lâu • leo bảng xếp hạng",
                        color = Color(0xFF81D4FA)
                    )
                }
                FilledIconButton(onClick = onRefresh) {
                    if (loading) {
                        CircularProgressIndicator(
                            Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Refresh, "Làm mới")
                    }
                }
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF111B2B)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF052B4B),
                                    Color(0xFF1A1744),
                                    Color(0xFF05070D)
                                )
                            )
                        )
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawLobbyCharacter()
                    }
                    Column(
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "ĐẤU TRƯỜNG 10 NGƯỜI",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Không đủ người sẽ tự thêm bot chiến thuật",
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text("1 bản đồ") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("3 phút") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("20 mạng") }
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArenaInfoCard(
                    modifier = Modifier.weight(1f),
                    title = "M4X COIN",
                    value = balance.toString(),
                    icon = Icons.Default.Paid
                )
                ArenaInfoCard(
                    modifier = Modifier.weight(1f),
                    title = "VŨ KHÍ",
                    value = equippedWeapon,
                    icon = Icons.Default.LocalFireDepartment
                )
            }
        }

        item {
            ArenaInfoCard(
                modifier = Modifier.fillMaxWidth(),
                title = "TRANG BỊ",
                value = equippedArmor,
                icon = Icons.Default.Security
            )
        }

        item {
            Button(
                onClick = onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC107),
                    contentColor = Color(0xFF171000)
                )
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "TÌM TRẬN",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        item {
            OutlinedButton(
                onClick = onShop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Storefront, null)
                Spacer(Modifier.width(8.dp))
                Text("CỬA HÀNG VŨ KHÍ & VẬT PHẨM")
            }
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF101827)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "AI BOT CHIẾN THUẬT",
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Bot biết né đạn, tìm vật cản, rút lui khi ít máu, " +
                            "nhặt cứu thương, thay đạn, flank và ngắm đón hướng chạy.",
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        "Bản thử nghiệm đang chạy 1 người thật + 9 bot. " +
                            "Online sẽ được nối sau khi gameplay ổn định.",
                        color = Color(0xFF80CBC4),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ArenaInfoCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color(0xFF111B2B)
        ),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFF193D60),
                shape = CircleShape
            ) {
                Icon(
                    icon,
                    null,
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    color = Color.White.copy(alpha = 0.58f),
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    value,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ArenaMatchmaking(
    seconds: Int,
    onCancel: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFF123B63),
                        Color(0xFF070914)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CircularProgressIndicator(
                Modifier.size(72.dp),
                strokeWidth = 7.dp,
                color = Color(0xFF00E5FF)
            )
            Text(
                "ĐANG TÌM TRẬN",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                "00:0$seconds",
                color = Color(0xFF69F0AE),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                "Đã tìm thấy: 1/10\nĐang thêm 9 bot chiến thuật…",
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onCancel) {
                Text("HỦY TÌM")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArenaShop(
    balance: Long,
    items: List<ShopItem>,
    inventory: List<InventoryItem>,
    loading: Boolean,
    onBack: () -> Unit,
    onBuy: (ShopItem) -> Unit,
    onEquip: (InventoryItem) -> Unit
) {
    val ownedByItemId = inventory.associateBy { it.itemId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "CỬA HÀNG ARENA",
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "$balance M4X COIN",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color(0xFF111B2B)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Vũ khí và vật phẩm chiến đấu",
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Mua một lần, lưu trong kho M4X và trang bị trước trận.",
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            if (loading && items.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (!loading && items.isEmpty()) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                        Column(
                            Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Chưa có vật phẩm Arena",
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Hãy chạy file SQL v3.3.0 trên Supabase rồi tải lại cửa hàng."
                            )
                        }
                    }
                }
            }

            items(items, key = { it.id }) { item ->
                val owned = ownedByItemId[item.id]
                ArenaShopItemCard(
                    item = item,
                    owned = owned,
                    enabled = !loading,
                    onBuy = { onBuy(item) },
                    onEquip = {
                        if (owned != null) {
                            onEquip(owned)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ArenaShopItemCard(
    item: ShopItem,
    owned: InventoryItem?,
    enabled: Boolean,
    onBuy: () -> Unit,
    onEquip: () -> Unit
) {
    val icon = when (item.type) {
        "arena_weapon" -> Icons.Default.LocalFireDepartment
        "arena_armor" -> Icons.Default.Security
        "arena_boost" -> Icons.Default.Bolt
        else -> Icons.Default.HealthAndSafety
    }

    ElevatedCard(
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF132F4C)
            ) {
                Icon(
                    icon,
                    null,
                    tint = Color(0xFF81D4FA),
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontWeight = FontWeight.Black
                )
                Text(
                    arenaItemDescription(item.name),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Paid,
                        null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFFFB300)
                    )
                    Text(
                        item.price.toString(),
                        fontWeight = FontWeight.Bold
                    )
                    if (owned?.equipped == true) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Đang dùng") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    null,
                                    Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
            when {
                owned == null -> Button(
                    enabled = enabled,
                    onClick = onBuy
                ) {
                    Text("Mua")
                }

                owned.equipped -> OutlinedButton(
                    enabled = false,
                    onClick = {}
                ) {
                    Text("Đã trang bị")
                }

                else -> OutlinedButton(
                    enabled = enabled,
                    onClick = onEquip
                ) {
                    Text("Trang bị")
                }
            }
        }
    }
}

private fun arenaItemDescription(name: String): String = when (name) {
    "SMG-7 Neon" -> "Bắn nhanh, cơ động tốt ở cự ly gần"
    "P90-X Plasma" -> "Băng 50 viên, tốc độ bắn rất cao"
    "Viper-S Sniper" -> "Sát thương lớn, ngắm xa chính xác"
    "Giáp MK-II" -> "Tăng giáp đầu trận lên 80"
    "Giày phản lực" -> "Tăng tốc độ di chuyển"
    "Túi cứu thương" -> "Bắt đầu trận với 3 medkit"
    else -> "Vật phẩm chiến đấu M4X Arena"
}

@Composable
private fun ArenaMatch(
    profile: Profile?,
    inventory: List<InventoryItem>,
    onQuit: () -> Unit,
    onFinished: (ArenaResult) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation =
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val controller = activity?.let {
            WindowCompat.getInsetsController(
                it.window,
                it.window.decorView
            )
        }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation =
                previousOrientation
                    ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val loadout = remember(inventory) {
        arenaLoadoutFromInventory(inventory)
    }
    val actors = remember { mutableStateListOf<ArenaActor>() }
    val bullets = remember { mutableStateListOf<ArenaBullet>() }
    val particles = remember { mutableStateListOf<ArenaParticle>() }
    val pickups = remember {
        mutableStateListOf(
            ArenaPickup(0, PickupType.MEDKIT, Offset(345f, 225f)),
            ArenaPickup(1, PickupType.ARMOR, Offset(750f, 210f)),
            ArenaPickup(2, PickupType.AMMO, Offset(610f, 465f)),
            ArenaPickup(3, PickupType.MEDKIT, Offset(1030f, 475f))
        )
    }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var moveInput by remember { mutableStateOf(Offset.Zero) }
    var joystickKnob by remember { mutableStateOf(Offset.Zero) }
    var firing by remember { mutableStateOf(false) }
    var matchTime by remember { mutableFloatStateOf(MATCH_SECONDS) }
    var running by remember { mutableStateOf(true) }
    var showQuitDialog by remember { mutableStateOf(false) }
    var announcement by remember { mutableStateOf("1 người thật • 9 bot") }
    var pickupSpawnTimer by remember { mutableFloatStateOf(7f) }
    val density = LocalDensity.current
    val joystickRadius = with(density) { 58.dp.toPx() }

    fun initializeMatch() {
        actors.clear()
        bullets.clear()
        particles.clear()
        matchTime = MATCH_SECONDS
        running = true
        announcement = "BẮT ĐẦU!"

        actors += ArenaActor(
            id = PLAYER_ID,
            name = profile?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: "M4X Hunter",
            isPlayer = true,
            position = spawnPoints.first(),
            armor = loadout.maxArmor,
            maxArmor = loadout.maxArmor,
            moveSpeed = loadout.moveSpeed,
            ammo = loadout.weapon.magazine,
            reserveAmmo = loadout.weapon.reserve,
            magazine = loadout.weapon.magazine,
            medkits = loadout.medkits,
            weapon = loadout.weapon,
            botSkill = 1f
        )

        val botNames = listOf(
            "ThiênPro",
            "Zeroo",
            "Bot_01",
            "Bot_02",
            "Shadow",
            "Nova",
            "Raptor",
            "Kira",
            "Viper"
        )

        botNames.forEachIndexed { index, name ->
            val botWeapon = when (index % 4) {
                0 -> defaultArenaWeapon()
                1 -> smgWeapon()
                2 -> p90Weapon()
                else -> sniperWeapon()
            }
            val skill = 0.74f + (index * 0.025f)
            actors += ArenaActor(
                id = index + 1,
                name = name,
                isPlayer = false,
                position = spawnPoints[index + 1],
                armor = 45f + index * 3f,
                maxArmor = 45f + index * 3f,
                moveSpeed = 190f + index * 3.5f,
                ammo = botWeapon.magazine,
                reserveAmmo = botWeapon.reserve,
                magazine = botWeapon.magazine,
                medkits = if (index % 3 == 0) 2 else 1,
                botSkill = skill.coerceAtMost(0.95f),
                weapon = botWeapon
            )
        }
    }

    LaunchedEffect(Unit) {
        initializeMatch()
        delay(900)
        announcement = ""
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var previous = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val dt = ((now - previous) / 1_000_000_000f)
                .coerceIn(0.012f, 0.045f)
            previous = now

            val nextActors = simulateArenaActors(
                actors = actors.toList(),
                bullets = bullets.toList(),
                pickups = pickups.toList(),
                moveInput = moveInput,
                firing = firing,
                dt = dt,
                obstacles = arenaObstacles,
                spawnPoints = spawnPoints,
                onBullet = { bullets += it },
                onParticle = { particles += it }
            )
            actors.clear()
            actors.addAll(nextActors)

            val bulletUpdate = simulateBullets(
                bullets = bullets.toList(),
                actors = actors.toList(),
                obstacles = arenaObstacles,
                dt = dt
            )
            bullets.clear()
            bullets.addAll(bulletUpdate.bullets)
            actors.clear()
            actors.addAll(bulletUpdate.actors)
            particles += bulletUpdate.particles

            val pickupUpdate = simulatePickups(
                actors = actors.toList(),
                pickups = pickups.toList(),
                dt = dt
            )
            actors.clear()
            actors.addAll(pickupUpdate.actors)
            pickups.clear()
            pickups.addAll(pickupUpdate.pickups)

            val updatedParticles = particles.map {
                it.copy(
                    position = it.position + it.velocity * dt,
                    velocity = it.velocity * 0.92f,
                    life = it.life - dt,
                    radius = max(0.5f, it.radius - dt * 5f)
                )
            }.filter { it.life > 0f }
            particles.clear()
            particles.addAll(updatedParticles)

            pickupSpawnTimer -= dt
            if (pickupSpawnTimer <= 0f) {
                pickupSpawnTimer = 8f
                val unavailable = pickups.filter {
                    it.respawnRemaining <= 0f
                }
                if (unavailable.size < pickups.size) {
                    val target = pickups.firstOrNull {
                        it.respawnRemaining > 0f
                    }
                    if (target != null) {
                        val index = pickups.indexOfFirst {
                            it.id == target.id
                        }
                        if (index >= 0) {
                            pickups[index] = target.copy(
                                respawnRemaining = 0f
                            )
                        }
                    }
                }
            }

            matchTime = max(0f, matchTime - dt)
            val best = actors.maxByOrNull { it.kills }
            if (
                matchTime <= 0f ||
                (best?.kills ?: 0) >= KILL_LIMIT
            ) {
                running = false
                val ranking = actors.sortedWith(
                    compareByDescending<ArenaActor> { it.kills }
                        .thenBy { it.deaths }
                )
                val player = actors.first { it.id == PLAYER_ID }
                val rank = ranking.indexOfFirst {
                    it.id == PLAYER_ID
                } + 1
                onFinished(
                    ArenaResult(
                        rank = rank,
                        kills = player.kills,
                        deaths = player.deaths,
                        winner = ranking.firstOrNull()?.name ?: "Bot"
                    )
                )
            }

            delay(16)
        }
    }

    BackHandler {
        showQuitDialog = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF060A0F))
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
        ) {
            drawArenaWorld(
                actors = actors,
                bullets = bullets,
                particles = particles,
                pickups = pickups,
                obstacles = arenaObstacles,
                canvasSize = canvasSize
            )
        }

        ArenaHud(
            actors = actors,
            matchTime = matchTime,
            onQuit = { showQuitDialog = true }
        )

        if (announcement.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp),
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    announcement,
                    modifier = Modifier.padding(
                        horizontal = 22.dp,
                        vertical = 10.dp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .size(132.dp)
                .background(
                    Color.Black.copy(alpha = 0.34f),
                    CircleShape
                )
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.22f),
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            joystickKnob = Offset.Zero
                            moveInput = Offset.Zero
                        },
                        onDragEnd = {
                            joystickKnob = Offset.Zero
                            moveInput = Offset.Zero
                        },
                        onDragCancel = {
                            joystickKnob = Offset.Zero
                            moveInput = Offset.Zero
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        joystickKnob = (
                            joystickKnob + dragAmount
                        ).limitLength(joystickRadius)
                        moveInput = if (
                            joystickKnob.getDistance() > 3f
                        ) {
                            joystickKnob / joystickRadius
                        } else {
                            Offset.Zero
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .background(
                        Color.White.copy(alpha = 0.75f),
                        CircleShape
                    )
                    .offsetByPixels(joystickKnob)
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
                .size(112.dp)
                .background(
                    Color(0xFFCC1E2C).copy(alpha = 0.82f),
                    CircleShape
                )
                .border(
                    3.dp,
                    Color.White.copy(alpha = 0.52f),
                    CircleShape
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            firing = true
                            tryAwaitRelease()
                            firing = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Text(
                    "BẮN",
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }

        ArenaActionButtons(
            actor = actors.firstOrNull { it.id == PLAYER_ID },
            onReload = {
                val index = actors.indexOfFirst {
                    it.id == PLAYER_ID
                }
                if (index >= 0) {
                    actors[index] = startReload(actors[index])
                }
            },
            onHeal = {
                val index = actors.indexOfFirst {
                    it.id == PLAYER_ID
                }
                if (index >= 0) {
                    val player = actors[index]
                    if (
                        player.medkits > 0 &&
                        player.health in 1f..94.9f
                    ) {
                        actors[index] = player.copy(
                            health = min(100f, player.health + 55f),
                            medkits = player.medkits - 1
                        )
                    }
                }
            }
        )
    }

    if (showQuitDialog) {
        AlertDialog(
            onDismissRequest = { showQuitDialog = false },
            title = { Text("Rời trận?") },
            text = {
                Text("Tiến trình trận thử nghiệm sẽ bị hủy.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        running = false
                        showQuitDialog = false
                        onQuit()
                    }
                ) {
                    Text("Rời trận")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showQuitDialog = false }
                ) {
                    Text("Tiếp tục")
                }
            }
        )
    }
}

@Composable
private fun ArenaHud(
    actors: List<ArenaActor>,
    matchTime: Float,
    onQuit: () -> Unit
) {
    val player = actors.firstOrNull { it.id == PLAYER_ID }
    val topFive = actors.sortedWith(
        compareByDescending<ArenaActor> { it.kills }
            .thenBy { it.deaths }
    ).take(5)

    Column(
        Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Black.copy(alpha = 0.62f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    topFive.forEachIndexed { index, actor ->
                        Text(
                            "${index + 1}. ${actor.name}  ${actor.kills}",
                            color = if (actor.isPlayer) {
                                Color(0xFFFFD54F)
                            } else {
                                Color.White
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (actor.isPlayer) {
                                FontWeight.Black
                            } else {
                                FontWeight.Medium
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.Black.copy(alpha = 0.62f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "%02d:%02d".format(
                            (matchTime.toInt() / 60),
                            (matchTime.toInt() % 60)
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = onQuit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            "Thoát",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (player != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 164.dp,
                        end = 148.dp,
                        bottom = 8.dp
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = Color.Black.copy(alpha = 0.66f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.HealthAndSafety,
                                null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                "${player.health.toInt()} HP",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.width(10.dp))
                            Icon(
                                Icons.Default.Security,
                                null,
                                tint = Color(0xFF40C4FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                player.armor.toInt().toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            "${player.weapon.name}  ${player.ammo}/${player.reserveAmmo}",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.ArenaActionButtons(
    actor: ArenaActor?,
    onReload: () -> Unit,
    onHeal: () -> Unit
) {
    Column(
        Modifier
            .align(Alignment.BottomEnd)
            .padding(
                end = 135.dp,
                bottom = 22.dp
            ),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        FilledIconButton(
            onClick = onReload,
            enabled = actor?.reloadRemaining == 0f
        ) {
            Icon(Icons.Default.Refresh, "Thay đạn")
        }
        FilledIconButton(
            onClick = onHeal,
            enabled = (actor?.medkits ?: 0) > 0
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.HealthAndSafety, "Cứu thương")
                Text(
                    "${actor?.medkits ?: 0}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun ArenaResultScreen(
    result: ArenaResult,
    onReplay: () -> Unit,
    onLobby: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E2840),
                        Color(0xFF090B15)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color(0xFF121B2A)
            ),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    Icons.Default.MilitaryTech,
                    null,
                    tint = Color(0xFFFFC107),
                    modifier = Modifier.size(72.dp)
                )
                Text(
                    "HẠNG ${result.rank}/10",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Người thắng: ${result.winner}",
                    color = Color(0xFF80CBC4)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ArenaResultStat("Hạ gục", result.kills.toString())
                    ArenaResultStat("Bị hạ", result.deaths.toString())
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                Text(
                    "Bản thử nghiệm chưa tự cộng Coin. " +
                        "Phần thưởng online sẽ do máy chủ xác minh.",
                    color = Color.White.copy(alpha = 0.68f),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onReplay,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CHƠI LẠI")
                }
                OutlinedButton(
                    onClick = onLobby,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VỀ SẢNH")
                }
            }
        }
    }
}

@Composable
private fun ArenaResultStat(
    label: String,
    value: String
) {
    Surface(
        color = Color(0xFF1C2A3E),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.padding(
                horizontal = 22.dp,
                vertical = 14.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                label,
                color = Color.White.copy(alpha = 0.58f)
            )
        }
    }
}

private data class BulletSimulation(
    val bullets: List<ArenaBullet>,
    val actors: List<ArenaActor>,
    val particles: List<ArenaParticle>
)

private data class PickupSimulation(
    val actors: List<ArenaActor>,
    val pickups: List<ArenaPickup>
)

private fun simulateArenaActors(
    actors: List<ArenaActor>,
    bullets: List<ArenaBullet>,
    pickups: List<ArenaPickup>,
    moveInput: Offset,
    firing: Boolean,
    dt: Float,
    obstacles: List<Rect>,
    spawnPoints: List<Offset>,
    onBullet: (ArenaBullet) -> Unit,
    onParticle: (ArenaParticle) -> Unit
): List<ArenaActor> {
    val snapshot = actors
    return actors.map { actor ->
        var next = actor.copy(
            fireCooldown = max(0f, actor.fireCooldown - dt),
            botModeTimer = max(0f, actor.botModeTimer - dt)
        )

        if (next.respawnRemaining > 0f) {
            val remaining = next.respawnRemaining - dt
            if (remaining <= 0f) {
                val spawn = chooseSafeSpawn(
                    actorId = next.id,
                    actors = snapshot,
                    spawnPoints = spawnPoints
                )
                return@map next.copy(
                    position = spawn,
                    velocity = Offset.Zero,
                    health = 100f,
                    armor = next.maxArmor,
                    ammo = next.magazine,
                    reserveAmmo = next.weapon.reserve,
                    medkits = max(1, next.medkits),
                    respawnRemaining = 0f,
                    reloadRemaining = 0f
                )
            }
            return@map next.copy(
                respawnRemaining = remaining,
                velocity = Offset.Zero
            )
        }

        if (next.reloadRemaining > 0f) {
            val remaining = next.reloadRemaining - dt
            next = if (remaining <= 0f) {
                finishReload(next)
            } else {
                next.copy(
                    reloadRemaining = remaining,
                    velocity = if (next.isPlayer) {
                        moveInput.normalizedOrZero() *
                            next.moveSpeed * 0.58f
                    } else {
                        next.velocity * 0.82f
                    }
                )
            }
        }

        if (next.isPlayer) {
            val velocity = moveInput
                .limitLength(1f) * next.moveSpeed
            val target = bestVisibleTarget(
                actor = next,
                actors = snapshot,
                obstacles = obstacles
            )
            val aim = target?.let {
                predictiveAim(
                    shooter = next,
                    target = it,
                    skill = 1f
                )
            } ?: next.aim

            next = next.copy(
                velocity = velocity,
                aim = aim
            )

            if (firing && next.reloadRemaining <= 0f) {
                next = fireActor(
                    actor = next,
                    onBullet = onBullet,
                    onParticle = onParticle
                )
            }
        } else {
            next = updateSmartBot(
                actor = next,
                actors = snapshot,
                bullets = bullets,
                pickups = pickups,
                obstacles = obstacles,
                dt = dt,
                onBullet = onBullet,
                onParticle = onParticle
            )
        }

        if (
            next.ammo <= 0 &&
            next.reserveAmmo > 0 &&
            next.reloadRemaining <= 0f
        ) {
            next = startReload(next)
        }

        val moved = resolveActorMovement(
            actor = next,
            desiredPosition = next.position + next.velocity * dt,
            obstacles = obstacles
        )

        moved.copy(
            velocity = moved.velocity * 0.94f
        )
    }
}

private fun updateSmartBot(
    actor: ArenaActor,
    actors: List<ArenaActor>,
    bullets: List<ArenaBullet>,
    pickups: List<ArenaPickup>,
    obstacles: List<Rect>,
    dt: Float,
    onBullet: (ArenaBullet) -> Unit,
    onParticle: (ArenaParticle) -> Unit
): ArenaActor {
    val target = chooseBotTarget(actor, actors, obstacles)
        ?: return actor.copy(velocity = Offset.Zero)
    val distance = actor.position.distanceTo(target.position)
    val hasLine = hasLineOfSight(
        actor.position,
        target.position,
        obstacles
    )
    val threat = dangerousBullet(actor, bullets)
    val usefulPickup = chooseUsefulPickup(actor, pickups)

    var mode = actor.botMode
    var modeTimer = actor.botModeTimer

    if (modeTimer <= 0f) {
        mode = when {
            threat != null -> BotMode.DODGE
            actor.health < 38f &&
                actor.medkits > 0 -> BotMode.HEAL
            actor.health < 48f &&
                usefulPickup?.type == PickupType.MEDKIT ->
                BotMode.HEAL
            actor.ammo <= max(2, actor.magazine / 6) ->
                BotMode.RELOAD
            actor.health < 42f -> BotMode.COVER
            !hasLine -> BotMode.FLANK
            distance < actor.weapon.range * 0.42f ->
                BotMode.STRAFE
            else -> BotMode.HUNT
        }
        modeTimer = 0.35f + Random.nextFloat() * 0.55f
    }

    var desired = Offset.Zero
    var next = actor.copy(
        botMode = mode,
        botModeTimer = modeTimer
    )

    val toTarget = (target.position - actor.position)
        .normalizedOrZero()
    val perpendicular = Offset(-toTarget.y, toTarget.x)

    when (mode) {
        BotMode.DODGE -> {
            val bullet = threat
            if (bullet != null) {
                val bulletDir = bullet.velocity.normalizedOrZero()
                val side = if (
                    actor.position.cross(
                        bullet.position + bulletDir * 30f
                    ) >= 0f
                ) 1f else -1f
                desired = Offset(
                    -bulletDir.y,
                    bulletDir.x
                ) * side
            }
        }

        BotMode.HEAL -> {
            if (
                actor.medkits > 0 &&
                actor.health < 62f &&
                distance > 220f
            ) {
                next = next.copy(
                    health = min(100f, actor.health + 48f),
                    medkits = actor.medkits - 1
                )
                desired = -toTarget
            } else if (usefulPickup != null) {
                desired = (
                    usefulPickup.position - actor.position
                ).normalizedOrZero()
            } else {
                desired = -toTarget
            }
        }

        BotMode.RELOAD -> {
            if (
                next.reloadRemaining <= 0f &&
                next.reserveAmmo > 0
            ) {
                next = startReload(next)
            }
            desired = bestCoverDirection(
                actor = actor,
                threat = target,
                obstacles = obstacles
            )
        }

        BotMode.COVER -> {
            desired = bestCoverDirection(
                actor = actor,
                threat = target,
                obstacles = obstacles
            )
        }

        BotMode.FLANK -> {
            val side = if (actor.id % 2 == 0) 1f else -1f
            desired = (
                toTarget * 0.45f +
                    perpendicular * side * 0.9f
                ).normalizedOrZero()
        }

        BotMode.STRAFE -> {
            val ideal = actor.weapon.range * 0.55f
            val rangeCorrection = when {
                distance < ideal * 0.68f -> -toTarget
                distance > ideal * 1.18f -> toTarget
                else -> Offset.Zero
            }
            val strafeSide = if (
                ((actor.id + (actor.kills * 3)) % 2) == 0
            ) 1f else -1f
            desired = (
                perpendicular * strafeSide * 0.82f +
                    rangeCorrection * 0.72f
                ).normalizedOrZero()
        }

        BotMode.HUNT -> {
            desired = toTarget
        }
    }

    desired += obstacleAvoidance(
        actor.position,
        obstacles
    ) * 0.85f
    desired = desired.limitLength(1f)

    val predictedAim = predictiveAim(
        shooter = actor,
        target = target,
        skill = actor.botSkill
    )
    val aimJitter = (1f - actor.botSkill) * 0.11f
    val jitteredAim = predictedAim.rotate(
        Random.nextFloat() * aimJitter * 2f - aimJitter
    )

    next = next.copy(
        velocity = desired * next.moveSpeed,
        aim = jitteredAim
    )

    val reactionReady =
        next.fireCooldown <= 0f &&
            next.reloadRemaining <= 0f &&
            next.ammo > 0
    val inRange = distance <= next.weapon.range
    val aimConfidence = next.botSkill -
        (distance / max(1f, next.weapon.range)) * 0.12f

    if (
        hasLine &&
        inRange &&
        reactionReady &&
        Random.nextFloat() < aimConfidence.coerceIn(0.38f, 0.94f)
    ) {
        next = fireActor(
            actor = next,
            onBullet = onBullet,
            onParticle = onParticle
        )
    }

    return next
}

private fun fireActor(
    actor: ArenaActor,
    onBullet: (ArenaBullet) -> Unit,
    onParticle: (ArenaParticle) -> Unit
): ArenaActor {
    if (
        actor.fireCooldown > 0f ||
        actor.reloadRemaining > 0f ||
        actor.ammo <= 0 ||
        !actor.alive
    ) {
        return actor
    }

    val spread = (
        Random.nextFloat() * 2f - 1f
    ) * actor.weapon.spread
    val direction = actor.aim
        .normalizedOrZero()
        .rotate(spread)
    val muzzle = actor.position + direction * 19f

    onBullet(
        ArenaBullet(
            ownerId = actor.id,
            position = muzzle,
            previousPosition = muzzle,
            velocity = direction * actor.weapon.bulletSpeed,
            damage = actor.weapon.damage,
            life = actor.weapon.range /
                actor.weapon.bulletSpeed
        )
    )

    repeat(5) {
        val angle = spread +
            (Random.nextFloat() - 0.5f) * 0.55f
        onParticle(
            ArenaParticle(
                position = muzzle,
                velocity = direction.rotate(angle) *
                    (65f + Random.nextFloat() * 95f),
                life = 0.16f + Random.nextFloat() * 0.12f,
                radius = 5f + Random.nextFloat() * 4f,
                color = Color(0xFFFFC44D)
            )
        )
    }

    return actor.copy(
        ammo = actor.ammo - 1,
        fireCooldown = actor.weapon.fireInterval
    )
}

private fun simulateBullets(
    bullets: List<ArenaBullet>,
    actors: List<ArenaActor>,
    obstacles: List<Rect>,
    dt: Float
): BulletSimulation {
    val mutableActors = actors.toMutableList()
    val nextBullets = mutableListOf<ArenaBullet>()
    val particles = mutableListOf<ArenaParticle>()

    bullets.forEach { bullet ->
        val nextPosition = bullet.position + bullet.velocity * dt
        val nextLife = bullet.life - dt
        var consumed = nextLife <= 0f

        if (!consumed) {
            consumed = obstacles.any {
                lineIntersectsRect(
                    bullet.position,
                    nextPosition,
                    it
                )
            }
        }

        if (!consumed) {
            val hitIndex = mutableActors.indexOfFirst { actor ->
                actor.id != bullet.ownerId &&
                    actor.alive &&
                    distancePointToSegment(
                        actor.position,
                        bullet.position,
                        nextPosition
                    ) <= 14f
            }

            if (hitIndex >= 0) {
                consumed = true
                val target = mutableActors[hitIndex]
                var remainingDamage = bullet.damage
                var armor = target.armor
                var health = target.health

                if (armor > 0f) {
                    val absorbed = min(
                        armor,
                        remainingDamage * 0.72f
                    )
                    armor -= absorbed
                    remainingDamage -= absorbed
                }

                health -= remainingDamage
                var updated = target.copy(
                    armor = max(0f, armor),
                    health = max(0f, health),
                    lastDamagedBy = bullet.ownerId
                )

                repeat(8) {
                    particles += ArenaParticle(
                        position = target.position,
                        velocity = Offset(
                            Random.nextFloat() * 150f - 75f,
                            Random.nextFloat() * 150f - 75f
                        ),
                        life = 0.28f + Random.nextFloat() * 0.2f,
                        radius = 4f + Random.nextFloat() * 4f,
                        color = if (armor > 0f) {
                            Color(0xFF40C4FF)
                        } else {
                            Color(0xFFFF5252)
                        }
                    )
                }

                if (updated.health <= 0f) {
                    updated = updated.copy(
                        deaths = updated.deaths + 1,
                        respawnRemaining = 3f,
                        velocity = Offset.Zero
                    )
                    val killerIndex = mutableActors.indexOfFirst {
                        it.id == bullet.ownerId
                    }
                    if (killerIndex >= 0) {
                        mutableActors[killerIndex] =
                            mutableActors[killerIndex].copy(
                                kills = mutableActors[killerIndex]
                                    .kills + 1
                            )
                    }
                }

                mutableActors[hitIndex] = updated
            }
        }

        if (!consumed) {
            nextBullets += bullet.copy(
                previousPosition = bullet.position,
                position = nextPosition,
                life = nextLife
            )
        } else {
            repeat(4) {
                particles += ArenaParticle(
                    position = nextPosition,
                    velocity = Offset(
                        Random.nextFloat() * 70f - 35f,
                        Random.nextFloat() * 70f - 35f
                    ),
                    life = 0.12f + Random.nextFloat() * 0.12f,
                    radius = 3f + Random.nextFloat() * 3f,
                    color = Color(0xFFFFD180)
                )
            }
        }
    }

    return BulletSimulation(
        bullets = nextBullets,
        actors = mutableActors,
        particles = particles
    )
}

private fun simulatePickups(
    actors: List<ArenaActor>,
    pickups: List<ArenaPickup>,
    dt: Float
): PickupSimulation {
    val mutableActors = actors.toMutableList()
    val nextPickups = pickups.map { pickup ->
        if (pickup.respawnRemaining > 0f) {
            return@map pickup.copy(
                respawnRemaining = max(
                    0f,
                    pickup.respawnRemaining - dt
                )
            )
        }

        val actorIndex = mutableActors.indexOfFirst {
            it.alive &&
                it.position.distanceTo(pickup.position) < 28f
        }

        if (actorIndex < 0) {
            pickup
        } else {
            val actor = mutableActors[actorIndex]
            mutableActors[actorIndex] = when (pickup.type) {
                PickupType.MEDKIT -> actor.copy(
                    health = min(100f, actor.health + 42f)
                )

                PickupType.ARMOR -> actor.copy(
                    armor = min(
                        actor.maxArmor,
                        actor.armor + 38f
                    )
                )

                PickupType.AMMO -> actor.copy(
                    reserveAmmo = actor.reserveAmmo +
                        actor.magazine
                )
            }
            pickup.copy(respawnRemaining = 12f)
        }
    }

    return PickupSimulation(
        actors = mutableActors,
        pickups = nextPickups
    )
}

private fun chooseBotTarget(
    actor: ArenaActor,
    actors: List<ArenaActor>,
    obstacles: List<Rect>
): ArenaActor? {
    return actors
        .asSequence()
        .filter {
            it.id != actor.id &&
                it.alive
        }
        .minByOrNull { target ->
            val distance = actor.position.distanceTo(
                target.position
            )
            val lowHealthBonus = (100f - target.health) * 1.8f
            val revengeBonus = if (
                actor.lastDamagedBy == target.id
            ) 110f else 0f
            val lineBonus = if (
                hasLineOfSight(
                    actor.position,
                    target.position,
                    obstacles
                )
            ) 90f else 0f
            distance - lowHealthBonus -
                revengeBonus - lineBonus
        }
}

private fun bestVisibleTarget(
    actor: ArenaActor,
    actors: List<ArenaActor>,
    obstacles: List<Rect>
): ArenaActor? {
    return actors
        .filter {
            it.id != actor.id &&
                it.alive &&
                hasLineOfSight(
                    actor.position,
                    it.position,
                    obstacles
                )
        }
        .minByOrNull {
            actor.position.distanceTo(it.position)
        }
}

private fun chooseUsefulPickup(
    actor: ArenaActor,
    pickups: List<ArenaPickup>
): ArenaPickup? {
    return pickups
        .filter { it.respawnRemaining <= 0f }
        .filter {
            when (it.type) {
                PickupType.MEDKIT -> actor.health < 68f
                PickupType.ARMOR -> actor.armor <
                    actor.maxArmor * 0.62f
                PickupType.AMMO -> actor.reserveAmmo <
                    actor.magazine
            }
        }
        .minByOrNull {
            actor.position.distanceTo(it.position)
        }
}

private fun dangerousBullet(
    actor: ArenaActor,
    bullets: List<ArenaBullet>
): ArenaBullet? {
    return bullets
        .asSequence()
        .filter { it.ownerId != actor.id }
        .filter {
            it.position.distanceTo(actor.position) < 145f
        }
        .filter {
            val direction = it.velocity.normalizedOrZero()
            val towardActor = (
                actor.position - it.position
            ).normalizedOrZero()
            direction.dot(towardActor) > 0.72f
        }
        .minByOrNull {
            it.position.distanceTo(actor.position)
        }
}

private fun predictiveAim(
    shooter: ArenaActor,
    target: ArenaActor,
    skill: Float
): Offset {
    val distance = shooter.position.distanceTo(
        target.position
    )
    val travelTime = distance /
        max(1f, shooter.weapon.bulletSpeed)
    val lead = target.velocity *
        travelTime *
        skill.coerceIn(0.55f, 1f)
    return (
        target.position + lead - shooter.position
    ).normalizedOrZero()
}

private fun bestCoverDirection(
    actor: ArenaActor,
    threat: ArenaActor,
    obstacles: List<Rect>
): Offset {
    val threatDirection = (
        actor.position - threat.position
    ).normalizedOrZero()

    val bestPoint = obstacles
        .flatMap { rect ->
            listOf(
                Offset(rect.left - 24f, rect.top - 24f),
                Offset(rect.right + 24f, rect.top - 24f),
                Offset(rect.left - 24f, rect.bottom + 24f),
                Offset(rect.right + 24f, rect.bottom + 24f)
            )
        }
        .filter {
            it.x in 24f..(ARENA_WIDTH - 24f) &&
                it.y in 24f..(ARENA_HEIGHT - 24f)
        }
        .minByOrNull { point ->
            val distance = actor.position.distanceTo(point)
            val coverQuality = (
                point - threat.position
            ).normalizedOrZero().dot(threatDirection)
            distance - coverQuality * 110f
        }

    return if (bestPoint != null) {
        (bestPoint - actor.position).normalizedOrZero()
    } else {
        threatDirection
    }
}

private fun obstacleAvoidance(
    position: Offset,
    obstacles: List<Rect>
): Offset {
    var force = Offset.Zero
    obstacles.forEach { rect ->
        val closest = Offset(
            position.x.coerceIn(rect.left, rect.right),
            position.y.coerceIn(rect.top, rect.bottom)
        )
        val delta = position - closest
        val distance = delta.getDistance()
        if (distance in 0.001f..54f) {
            force += delta.normalizedOrZero() *
                ((54f - distance) / 54f)
        }
    }
    return force.limitLength(1f)
}

private fun resolveActorMovement(
    actor: ArenaActor,
    desiredPosition: Offset,
    obstacles: List<Rect>
): ArenaActor {
    val radius = 15f
    var position = Offset(
        desiredPosition.x.coerceIn(
            radius,
            ARENA_WIDTH - radius
        ),
        desiredPosition.y.coerceIn(
            radius,
            ARENA_HEIGHT - radius
        )
    )

    obstacles.forEach { rect ->
        val expanded = Rect(
            rect.left - radius,
            rect.top - radius,
            rect.right + radius,
            rect.bottom + radius
        )
        if (expanded.contains(position)) {
            val leftDistance = abs(position.x - expanded.left)
            val rightDistance = abs(expanded.right - position.x)
            val topDistance = abs(position.y - expanded.top)
            val bottomDistance = abs(expanded.bottom - position.y)
            val minimum = min(
                min(leftDistance, rightDistance),
                min(topDistance, bottomDistance)
            )
            position = when (minimum) {
                leftDistance -> Offset(
                    expanded.left,
                    position.y
                )
                rightDistance -> Offset(
                    expanded.right,
                    position.y
                )
                topDistance -> Offset(
                    position.x,
                    expanded.top
                )
                else -> Offset(
                    position.x,
                    expanded.bottom
                )
            }
        }
    }

    return actor.copy(position = position)
}

private fun startReload(actor: ArenaActor): ArenaActor {
    if (
        actor.reloadRemaining > 0f ||
        actor.ammo >= actor.magazine ||
        actor.reserveAmmo <= 0
    ) {
        return actor
    }
    return actor.copy(
        reloadRemaining = when (actor.weapon.name) {
            "Viper-S" -> 1.65f
            "P90-X" -> 1.4f
            else -> 1.15f
        },
        botMode = if (actor.isPlayer) {
            actor.botMode
        } else {
            BotMode.RELOAD
        }
    )
}

private fun finishReload(actor: ArenaActor): ArenaActor {
    val needed = actor.magazine - actor.ammo
    val loaded = min(needed, actor.reserveAmmo)
    return actor.copy(
        ammo = actor.ammo + loaded,
        reserveAmmo = actor.reserveAmmo - loaded,
        reloadRemaining = 0f
    )
}

private fun chooseSafeSpawn(
    actorId: Int,
    actors: List<ArenaActor>,
    spawnPoints: List<Offset>
): Offset {
    return spawnPoints.maxByOrNull { spawn ->
        actors
            .filter {
                it.id != actorId &&
                    it.alive
            }
            .minOfOrNull {
                spawn.distanceTo(it.position)
            } ?: 9999f
    } ?: spawnPoints.first()
}

private fun arenaLoadoutFromInventory(
    inventory: List<InventoryItem>
): ArenaLoadout {
    val weaponItem = inventory.firstOrNull {
        it.type == "arena_weapon" && it.equipped
    }
    val armorItem = inventory.firstOrNull {
        it.type == "arena_armor" && it.equipped
    }
    val boostItem = inventory.firstOrNull {
        it.type == "arena_boost" && it.equipped
    }
    val utilityItem = inventory.firstOrNull {
        it.type == "arena_utility" && it.equipped
    }

    val weapon = when (weaponItem?.name) {
        "SMG-7 Neon" -> smgWeapon()
        "P90-X Plasma" -> p90Weapon()
        "Viper-S Sniper" -> sniperWeapon()
        else -> defaultArenaWeapon()
    }

    val armor = when (armorItem?.name) {
        "Giáp MK-II" -> 80f
        else -> 50f
    }

    val speed = when (boostItem?.name) {
        "Giày phản lực" -> 235f
        else -> 205f
    }

    val medkits = when (utilityItem?.name) {
        "Túi cứu thương" -> 3
        else -> 1
    }

    return ArenaLoadout(
        weapon = weapon,
        maxArmor = armor,
        moveSpeed = speed,
        medkits = medkits
    )
}

private fun DrawScope.drawLobbyCharacter() {
    val center = Offset(
        size.width * 0.73f,
        size.height * 0.48f
    )
    val body = Path().apply {
        moveTo(center.x - 62f, center.y + 92f)
        lineTo(center.x - 50f, center.y - 26f)
        quadraticBezierTo(
            center.x,
            center.y - 80f,
            center.x + 50f,
            center.y - 26f
        )
        lineTo(center.x + 68f, center.y + 92f)
        close()
    }
    drawPath(
        body,
        Brush.linearGradient(
            listOf(
                Color(0xFF071522),
                Color(0xFF163450),
                Color(0xFF05070A)
            )
        )
    )
    drawCircle(
        Color(0xFF081018),
        radius = 44f,
        center = center.copy(y = center.y - 55f)
    )
    drawLine(
        color = Color(0xFF00E5FF),
        start = center.copy(
            x = center.x - 24f,
            y = center.y - 58f
        ),
        end = center.copy(
            x = center.x - 6f,
            y = center.y - 47f
        ),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF00E5FF),
        start = center.copy(
            x = center.x + 24f,
            y = center.y - 58f
        ),
        end = center.copy(
            x = center.x + 6f,
            y = center.y - 47f
        ),
        strokeWidth = 6f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF102F4D),
        start = center.copy(
            x = center.x - 85f,
            y = center.y + 5f
        ),
        end = center.copy(
            x = center.x + 86f,
            y = center.y + 57f
        ),
        strokeWidth = 22f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF42A5F5),
        start = center.copy(
            x = center.x - 74f,
            y = center.y + 2f
        ),
        end = center.copy(
            x = center.x + 76f,
            y = center.y + 48f
        ),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawArenaWorld(
    actors: List<ArenaActor>,
    bullets: List<ArenaBullet>,
    particles: List<ArenaParticle>,
    pickups: List<ArenaPickup>,
    obstacles: List<Rect>,
    canvasSize: IntSize
) {
    if (canvasSize == IntSize.Zero) return

    val scaleX = size.width / ARENA_WIDTH
    val scaleY = size.height / ARENA_HEIGHT
    val scale = min(scaleX, scaleY)
    val offsetX = (size.width - ARENA_WIDTH * scale) / 2f
    val offsetY = (size.height - ARENA_HEIGHT * scale) / 2f

    fun world(point: Offset): Offset = Offset(
        offsetX + point.x * scale,
        offsetY + point.y * scale
    )

    drawRect(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFF1B2A22),
                Color(0xFF2D3327),
                Color(0xFF151C1A)
            )
        ),
        topLeft = Offset(offsetX, offsetY),
        size = Size(
            ARENA_WIDTH * scale,
            ARENA_HEIGHT * scale
        )
    )

    val grid = 40f * scale
    var x = offsetX
    while (x <= offsetX + ARENA_WIDTH * scale) {
        drawLine(
            Color.White.copy(alpha = 0.025f),
            Offset(x, offsetY),
            Offset(x, offsetY + ARENA_HEIGHT * scale),
            1f
        )
        x += grid
    }
    var y = offsetY
    while (y <= offsetY + ARENA_HEIGHT * scale) {
        drawLine(
            Color.White.copy(alpha = 0.025f),
            Offset(offsetX, y),
            Offset(offsetX + ARENA_WIDTH * scale, y),
            1f
        )
        y += grid
    }

    obstacles.forEachIndexed { index, rect ->
        val topLeft = world(Offset(rect.left, rect.top))
        val obstacleSize = Size(
            rect.width * scale,
            rect.height * scale
        )
        drawRoundRect(
            color = if (index % 2 == 0) {
                Color(0xFF3B3F39)
            } else {
                Color(0xFF4C463A)
            },
            topLeft = topLeft,
            size = obstacleSize,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                7f * scale,
                7f * scale
            )
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = topLeft + Offset(
                7f * scale,
                7f * scale
            ),
            size = Size(
                max(1f, obstacleSize.width - 14f * scale),
                max(1f, obstacleSize.height - 14f * scale)
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                5f * scale,
                5f * scale
            ),
            style = Stroke(width = 2f * scale)
        )
    }

    pickups.filter {
        it.respawnRemaining <= 0f
    }.forEach { pickup ->
        val position = world(pickup.position)
        val color = when (pickup.type) {
            PickupType.MEDKIT -> Color(0xFFFF5252)
            PickupType.ARMOR -> Color(0xFF40C4FF)
            PickupType.AMMO -> Color(0xFFFFD740)
        }
        drawCircle(
            color.copy(alpha = 0.18f),
            radius = 22f * scale,
            center = position
        )
        drawCircle(
            color,
            radius = 10f * scale,
            center = position
        )
        drawCircle(
            Color.White.copy(alpha = 0.6f),
            radius = 4f * scale,
            center = position
        )
    }

    bullets.forEach { bullet ->
        drawLine(
            color = Color(0xFFFFE082),
            start = world(bullet.previousPosition),
            end = world(bullet.position),
            strokeWidth = 3.3f * scale,
            cap = StrokeCap.Round
        )
    }

    particles.forEach { particle ->
        drawCircle(
            color = particle.color.copy(
                alpha = particle.life.coerceIn(0f, 1f)
            ),
            radius = particle.radius * scale,
            center = world(particle.position)
        )
    }

    actors.forEach { actor ->
        if (!actor.alive) {
            if (actor.respawnRemaining > 0f) {
                val position = world(actor.position)
                drawCircle(
                    Color.White.copy(alpha = 0.08f),
                    radius = 18f * scale,
                    center = position,
                    style = Stroke(width = 2f * scale)
                )
            }
            return@forEach
        }

        val position = world(actor.position)
        val aim = actor.aim.normalizedOrZero()
        val actorColor = arenaActorColor(actor.id)

        drawCircle(
            Color.Black.copy(alpha = 0.34f),
            radius = 17f * scale,
            center = position + Offset(
                3f * scale,
                5f * scale
            )
        )

        drawLine(
            color = Color(0xFF111820),
            start = position,
            end = position + aim * 25f * scale,
            strokeWidth = 8f * scale,
            cap = StrokeCap.Round
        )

        drawCircle(
            actorColor,
            radius = 14f * scale,
            center = position
        )

        drawCircle(
            if (actor.isPlayer) {
                Color(0xFFFFD54F)
            } else {
                Color.White.copy(alpha = 0.72f)
            },
            radius = 16f * scale,
            center = position,
            style = Stroke(width = 2f * scale)
        )

        val barWidth = 48f * scale
        val barTop = position.y - 30f * scale
        drawRect(
            Color.Black.copy(alpha = 0.58f),
            topLeft = Offset(
                position.x - barWidth / 2f,
                barTop
            ),
            size = Size(barWidth, 5f * scale)
        )
        drawRect(
            Color(0xFF4CAF50),
            topLeft = Offset(
                position.x - barWidth / 2f,
                barTop
            ),
            size = Size(
                barWidth * (
                    actor.health / 100f
                ).coerceIn(0f, 1f),
                5f * scale
            )
        )

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12f * scale
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = actor.isPlayer
        }
        drawContext.canvas.nativeCanvas.drawText(
            actor.name,
            position.x,
            position.y - 36f * scale,
            paint
        )
    }
}

private fun arenaActorColor(id: Int): Color {
    val colors = listOf(
        Color(0xFF00B8D4),
        Color(0xFFFF7043),
        Color(0xFFAB47BC),
        Color(0xFF66BB6A),
        Color(0xFFEF5350),
        Color(0xFF5C6BC0),
        Color(0xFFFFCA28),
        Color(0xFF26A69A),
        Color(0xFFEC407A),
        Color(0xFF8D6E63)
    )
    return colors[id % colors.size]
}

private fun hasLineOfSight(
    start: Offset,
    end: Offset,
    obstacles: List<Rect>
): Boolean {
    return obstacles.none {
        lineIntersectsRect(start, end, it)
    }
}

private fun lineIntersectsRect(
    start: Offset,
    end: Offset,
    rect: Rect
): Boolean {
    if (rect.contains(start) || rect.contains(end)) {
        return true
    }
    val topLeft = Offset(rect.left, rect.top)
    val topRight = Offset(rect.right, rect.top)
    val bottomLeft = Offset(rect.left, rect.bottom)
    val bottomRight = Offset(rect.right, rect.bottom)
    return segmentsIntersect(start, end, topLeft, topRight) ||
        segmentsIntersect(start, end, topRight, bottomRight) ||
        segmentsIntersect(start, end, bottomRight, bottomLeft) ||
        segmentsIntersect(start, end, bottomLeft, topLeft)
}

private fun segmentsIntersect(
    a: Offset,
    b: Offset,
    c: Offset,
    d: Offset
): Boolean {
    fun orientation(
        p: Offset,
        q: Offset,
        r: Offset
    ): Float = (q - p).cross(r - p)

    val o1 = orientation(a, b, c)
    val o2 = orientation(a, b, d)
    val o3 = orientation(c, d, a)
    val o4 = orientation(c, d, b)
    return o1 * o2 <= 0f && o3 * o4 <= 0f
}

private fun distancePointToSegment(
    point: Offset,
    start: Offset,
    end: Offset
): Float {
    val segment = end - start
    val lengthSquared = segment.dot(segment)
    if (lengthSquared <= 0.0001f) {
        return point.distanceTo(start)
    }
    val t = (
        (point - start).dot(segment) / lengthSquared
    ).coerceIn(0f, 1f)
    val projection = start + segment * t
    return point.distanceTo(projection)
}

private fun Offset.distanceTo(other: Offset): Float {
    return (this - other).getDistance()
}

private fun Offset.normalizedOrZero(): Offset {
    val length = getDistance()
    return if (length <= 0.0001f) {
        Offset.Zero
    } else {
        this / length
    }
}

private fun Offset.limitLength(maxLength: Float): Offset {
    val length = getDistance()
    return if (length <= maxLength || length <= 0.0001f) {
        this
    } else {
        this / length * maxLength
    }
}

private fun Offset.rotate(angle: Float): Offset {
    val cosine = cos(angle)
    val sine = sin(angle)
    return Offset(
        x * cosine - y * sine,
        x * sine + y * cosine
    )
}

private fun Offset.dot(other: Offset): Float {
    return x * other.x + y * other.y
}

private fun Offset.cross(other: Offset): Float {
    return x * other.y - y * other.x
}

private operator fun Offset.unaryMinus(): Offset {
    return Offset(-x, -y)
}

private fun Modifier.offsetByPixels(
    offset: Offset
): Modifier = this.then(
    Modifier.graphicsLayer {
        translationX = offset.x
        translationY = offset.y
    }
)
