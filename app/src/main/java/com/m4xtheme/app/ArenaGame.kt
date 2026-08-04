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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private const val ARENA_WIDTH = 1200f
private const val ARENA_HEIGHT = 720f
private const val MATCH_SECONDS = 180f
private const val KILL_LIMIT = 20

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
    val userId: String = "",
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
    val spawnShieldRemaining: Float = 0f,
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
    val winner: String,
    val reward: Int = 0,
    val balance: Long = 0L,
    val matchId: String = "",
    val allResultsJson: String = "[]"
)

private data class ArenaControl(
    val move: Offset = Offset.Zero,
    val firing: Boolean = false,
    val reload: Boolean = false,
    val heal: Boolean = false
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
    onMessage: (String) -> Unit,
    onImmersiveChanged: (Boolean) -> Unit = {}
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
    var onlineTicket by remember {
        mutableStateOf<ArenaMatchTicket?>(null)
    }
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
        onImmersiveChanged(page == ArenaPage.MATCH)

        if (page == ArenaPage.MATCHMAKING) {
            matchmakingSeconds = 0
            onlineTicket = null

            val name = profile?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: profile?.username
                ?.takeIf { it.isNotBlank() }
                ?: "M4X Hunter"

            val joined = api.joinArenaMatch(session, name)
            if (joined.isFailure) {
                onMessage(
                    joined.exceptionOrNull()?.message
                        ?: "Không thể tìm trận online"
                )
                page = ArenaPage.LOBBY
                return@LaunchedEffect
            }

            var ticket = joined.getOrThrow()
            onlineTicket = ticket

            while (
                page == ArenaPage.MATCHMAKING &&
                ticket.status == "waiting"
            ) {
                delay(1_000)
                matchmakingSeconds += 1

                api.arenaMatchStatus(session, ticket.matchId)
                    .onSuccess {
                        ticket = it
                        onlineTicket = it
                    }
                    .onFailure {
                        onMessage(
                            it.message ?: "Mất kết nối ghép trận"
                        )
                    }
            }

            if (
                page == ArenaPage.MATCHMAKING &&
                ticket.status == "playing"
            ) {
                page = ArenaPage.MATCH
            } else if (page == ArenaPage.MATCHMAKING) {
                onMessage("Phòng đã đóng, hãy tìm trận lại")
                page = ArenaPage.LOBBY
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { onImmersiveChanged(false) }
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
            playersFound = onlineTicket?.players?.size ?: 1,
            onCancel = {
                val matchId = onlineTicket?.matchId
                page = ArenaPage.LOBBY
                if (!matchId.isNullOrBlank()) {
                    scope.launch {
                        api.leaveArenaMatch(session, matchId)
                    }
                }
            }
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

        ArenaPage.MATCH -> {
            val ticket = onlineTicket
            if (ticket == null) {
                LaunchedEffect(Unit) {
                    onMessage("Không có thông tin phòng online")
                    page = ArenaPage.LOBBY
                }
            } else {
                ArenaMatch(
                    api = api,
                    session = session,
                    ticket = ticket,
                    profile = profile,
                    inventory = inventory,
                    onMessage = onMessage,
                    onQuit = {
                        page = ArenaPage.LOBBY
                        scope.launch {
                            api.leaveArenaMatch(
                                session,
                                ticket.matchId
                            )
                        }
                    },
                    onFinished = { arenaResult ->
                        scope.launch {
                            var finalResult = arenaResult

                            if (
                                ticket.hostUserId == session.userId &&
                                arenaResult.allResultsJson.isNotBlank()
                            ) {
                                api.finishArenaMatch(
                                    session = session,
                                    matchId = ticket.matchId,
                                    durationSeconds = MATCH_SECONDS.toInt(),
                                    results = runCatching {
                                        org.json.JSONArray(
                                            arenaResult.allResultsJson
                                        )
                                    }.getOrDefault(org.json.JSONArray())
                                ).onFailure {
                                    onMessage(
                                        it.message
                                            ?: "Không xác nhận được kết quả"
                                    )
                                }
                            }

                            var claim: ArenaRewardClaim? = null
                            for (attempt in 0 until 6) {
                                if (claim != null) break
                                delay(if (attempt == 0) 450 else 900)
                                api.claimArenaReward(
                                    session,
                                    ticket.matchId
                                ).onSuccess { claim = it }
                            }

                            claim?.let {
                                finalResult = arenaResult.copy(
                                    reward = it.reward,
                                    balance = it.balance
                                )
                                balance = it.balance
                                onCoinChanged(it.balance)
                                onMessage(it.message)
                            }

                            result = finalResult
                            page = ArenaPage.RESULT
                        }
                    }
                )
            }
        }

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
                        "Online Beta: tìm người thật trong 8 giây, " +
                            "sau đó tự thêm bot để đủ 10 vị trí. " +
                            "M4X Coin được cộng bằng máy chủ.",
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
    playersFound: Int,
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
                "00:${seconds.toString().padStart(2, '0')}",
                color = Color(0xFF69F0AE),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            Text(
                "Đã tìm thấy: $playersFound/10 người thật\n" +
                    "Còn thiếu ${10 - playersFound} vị trí sẽ thêm bot",
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
                            verticalArrangement = Arrangement.spacedBy(5.dp)
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


private fun arenaWeaponByName(name: String): ArenaWeapon = when (name) {
    smgWeapon().name -> smgWeapon()
    p90Weapon().name -> p90Weapon()
    sniperWeapon().name -> sniperWeapon()
    else -> defaultArenaWeapon()
}

private fun ArenaActor.toNetJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("human", isPlayer)
    .put("userId", userId)
    .put("x", position.x)
    .put("y", position.y)
    .put("vx", velocity.x)
    .put("vy", velocity.y)
    .put("ax", aim.x)
    .put("ay", aim.y)
    .put("health", health)
    .put("armor", armor)
    .put("maxArmor", maxArmor)
    .put("moveSpeed", moveSpeed)
    .put("kills", kills)
    .put("deaths", deaths)
    .put("ammo", ammo)
    .put("reserve", reserveAmmo)
    .put("magazine", magazine)
    .put("medkits", medkits)
    .put("fireCooldown", fireCooldown)
    .put("reload", reloadRemaining)
    .put("respawn", respawnRemaining)
    .put("shield", spawnShieldRemaining)
    .put("weapon", weapon.name)
    .put("lastDamagedBy", lastDamagedBy)

private fun arenaActorFromJson(json: JSONObject): ArenaActor = ArenaActor(
    id = json.optInt("id"),
    name = json.optString("name", "M4X"),
    isPlayer = json.optBoolean("human"),
    userId = json.optString("userId"),
    position = Offset(
        json.optDouble("x").toFloat(),
        json.optDouble("y").toFloat()
    ),
    velocity = Offset(
        json.optDouble("vx").toFloat(),
        json.optDouble("vy").toFloat()
    ),
    aim = Offset(
        json.optDouble("ax", 1.0).toFloat(),
        json.optDouble("ay").toFloat()
    ),
    health = json.optDouble("health", 100.0).toFloat(),
    armor = json.optDouble("armor", 50.0).toFloat(),
    maxArmor = json.optDouble("maxArmor", 50.0).toFloat(),
    moveSpeed = json.optDouble("moveSpeed", 205.0).toFloat(),
    kills = json.optInt("kills"),
    deaths = json.optInt("deaths"),
    ammo = json.optInt("ammo", 30),
    reserveAmmo = json.optInt("reserve", 120),
    magazine = json.optInt("magazine", 30),
    medkits = json.optInt("medkits", 1),
    fireCooldown = json.optDouble("fireCooldown").toFloat(),
    reloadRemaining = json.optDouble("reload").toFloat(),
    respawnRemaining = json.optDouble("respawn").toFloat(),
    spawnShieldRemaining = json.optDouble("shield").toFloat(),
    weapon = arenaWeaponByName(json.optString("weapon")),
    lastDamagedBy = json.optInt("lastDamagedBy", -1)
)

private fun ArenaBullet.toNetJson(): JSONObject = JSONObject()
    .put("ownerId", ownerId)
    .put("x", position.x)
    .put("y", position.y)
    .put("px", previousPosition.x)
    .put("py", previousPosition.y)
    .put("vx", velocity.x)
    .put("vy", velocity.y)
    .put("damage", damage)
    .put("life", life)

private fun arenaBulletFromJson(json: JSONObject) = ArenaBullet(
    ownerId = json.optInt("ownerId"),
    position = Offset(
        json.optDouble("x").toFloat(),
        json.optDouble("y").toFloat()
    ),
    previousPosition = Offset(
        json.optDouble("px").toFloat(),
        json.optDouble("py").toFloat()
    ),
    velocity = Offset(
        json.optDouble("vx").toFloat(),
        json.optDouble("vy").toFloat()
    ),
    damage = json.optDouble("damage").toFloat(),
    life = json.optDouble("life").toFloat()
)

private fun ArenaPickup.toNetJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("type", type.name)
    .put("x", position.x)
    .put("y", position.y)
    .put("respawn", respawnRemaining)

private fun arenaPickupFromJson(json: JSONObject) = ArenaPickup(
    id = json.optInt("id"),
    type = runCatching {
        PickupType.valueOf(json.optString("type"))
    }.getOrDefault(PickupType.AMMO),
    position = Offset(
        json.optDouble("x").toFloat(),
        json.optDouble("y").toFloat()
    ),
    respawnRemaining = json.optDouble("respawn").toFloat()
)

private fun arenaResultsJson(
    actors: List<ArenaActor>
): JSONArray {
    val ranking = actors.sortedWith(
        compareByDescending<ArenaActor> { it.kills }
            .thenBy { it.deaths }
    )
    return JSONArray().apply {
        ranking.forEachIndexed { index, actor ->
            put(
                JSONObject()
                    .put(
                        "userId",
                        actor.userId.ifBlank {
                            "bot:${actor.id}"
                        }
                    )
                    .put("rank", index + 1)
                    .put("kills", actor.kills)
                    .put("deaths", actor.deaths)
            )
        }
    }
}

@Composable
private fun ArenaMatch(
    api: SupabaseApi,
    session: Session,
    ticket: ArenaMatchTicket,
    profile: Profile?,
    inventory: List<InventoryItem>,
    onMessage: (String) -> Unit,
    onQuit: () -> Unit,
    onFinished: (ArenaResult) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val localActorId = ticket.slot
    val isHost = ticket.hostUserId == session.userId
    val controls = remember {
        ConcurrentHashMap<Int, ArenaControl>()
    }
    var realtimeConnected by remember { mutableStateOf(false) }
    var lastRemoteSnapshotAt by remember {
        mutableLongStateOf(System.currentTimeMillis())
    }
    var matchCompleted by remember { mutableStateOf(false) }
    var reloadPulse by remember { mutableStateOf(false) }
    var healPulse by remember { mutableStateOf(false) }
    var room by remember {
        mutableStateOf<ArenaRealtimeRoom?>(null)
    }

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
    var announcement by remember {
        mutableStateOf(
            "${ticket.players.size} người thật • " +
                "${10 - ticket.players.size} bot"
        )
    }
    var pickupSpawnTimer by remember { mutableFloatStateOf(7f) }
    val density = LocalDensity.current
    val joystickTravel = with(density) { 35.dp.toPx() }
    val joystickDeadZone = with(density) { 5.5.dp.toPx() }

    fun updateJoystick(pointerPosition: Offset, controlSize: IntSize) {
        val center = Offset(
            controlSize.width / 2f,
            controlSize.height / 2f
        )
        val displacement = (pointerPosition - center)
            .limitLength(joystickTravel)
        joystickKnob = displacement
        val distance = displacement.getDistance()
        moveInput = if (distance <= joystickDeadZone) {
            Offset.Zero
        } else {
            displacement / joystickTravel
        }
    }

    fun initializeMatch() {
        actors.clear()
        bullets.clear()
        particles.clear()
        matchTime = MATCH_SECONDS
        running = true
        announcement = "BẮT ĐẦU ONLINE!"

        val playersBySlot = ticket.players.associateBy { it.slot }
        val botNames = listOf(
            "ThiênPro",
            "Zeroo",
            "Bot_01",
            "Bot_02",
            "Shadow",
            "Nova",
            "Raptor",
            "Kira",
            "Viper",
            "M4X Bot"
        )

        repeat(10) { slot ->
            val onlinePlayer = playersBySlot[slot]
            if (onlinePlayer != null) {
                val actorLoadout = if (slot == localActorId) {
                    loadout
                } else {
                    ArenaLoadout(
                        weapon = defaultArenaWeapon(),
                        maxArmor = 50f,
                        moveSpeed = 205f,
                        medkits = 1
                    )
                }
                actors += ArenaActor(
                    id = slot,
                    name = onlinePlayer.displayName,
                    isPlayer = true,
                    userId = onlinePlayer.userId,
                    position = spawnPoints[slot],
                    armor = actorLoadout.maxArmor,
                    maxArmor = actorLoadout.maxArmor,
                    moveSpeed = actorLoadout.moveSpeed,
                    ammo = actorLoadout.weapon.magazine,
                    reserveAmmo = actorLoadout.weapon.reserve,
                    magazine = actorLoadout.weapon.magazine,
                    medkits = actorLoadout.medkits,
                    weapon = actorLoadout.weapon,
                    botSkill = 1f,
                    spawnShieldRemaining = 2.4f
                )
            } else {
                val botWeapon = when (slot % 4) {
                    0 -> defaultArenaWeapon()
                    1 -> smgWeapon()
                    2 -> p90Weapon()
                    else -> sniperWeapon()
                }
                actors += ArenaActor(
                    id = slot,
                    name = botNames[slot % botNames.size],
                    isPlayer = false,
                    position = spawnPoints[slot],
                    armor = 45f + slot * 3f,
                    maxArmor = 45f + slot * 3f,
                    moveSpeed = 190f + slot * 3.5f,
                    ammo = botWeapon.magazine,
                    reserveAmmo = botWeapon.reserve,
                    magazine = botWeapon.magazine,
                    medkits = if (slot % 3 == 0) 2 else 1,
                    botSkill = (0.74f + slot * 0.025f)
                        .coerceAtMost(0.95f),
                    weapon = botWeapon,
                    spawnShieldRemaining = 1.8f
                )
            }
        }

        ticket.players.forEach { player ->
            controls[player.slot] = ArenaControl()
        }
    }

    LaunchedEffect(Unit) {
        initializeMatch()
        delay(900)
        announcement = ""
    }

    DisposableEffect(ticket.matchId) {
        val realtime = ArenaRealtimeRoom(
            session = session,
            matchId = ticket.matchId,
            onConnected = {
                scope.launch {
                    realtimeConnected = true
                    announcement = if (isHost) {
                        "BẠN LÀ CHỦ PHÒNG"
                    } else {
                        "ĐÃ VÀO PHÒNG ONLINE"
                    }
                    delay(900)
                    announcement = ""
                }
            },
            onInput = { json ->
                if (isHost) {
                    val slot = json.optInt("slot", -1)
                    if (slot in 0..9) {
                        controls[slot] = ArenaControl(
                            move = Offset(
                                json.optDouble("moveX").toFloat(),
                                json.optDouble("moveY").toFloat()
                            ).limitLength(1f),
                            firing = json.optBoolean("firing"),
                            reload = json.optBoolean("reload"),
                            heal = json.optBoolean("heal")
                        )
                    }
                }
            },
            onSnapshot = { json ->
                if (!isHost && !matchCompleted) {
                    scope.launch {
                        val actorJson = json.optJSONArray("actors")
                            ?: JSONArray()
                        val bulletJson = json.optJSONArray("bullets")
                            ?: JSONArray()
                        val pickupJson = json.optJSONArray("pickups")
                            ?: JSONArray()

                        lastRemoteSnapshotAt =
                            System.currentTimeMillis()
                        actors.clear()
                        repeat(actorJson.length()) { index ->
                            actors += arenaActorFromJson(
                                actorJson.getJSONObject(index)
                            )
                        }
                        bullets.clear()
                        repeat(bulletJson.length()) { index ->
                            bullets += arenaBulletFromJson(
                                bulletJson.getJSONObject(index)
                            )
                        }
                        pickups.clear()
                        repeat(pickupJson.length()) { index ->
                            pickups += arenaPickupFromJson(
                                pickupJson.getJSONObject(index)
                            )
                        }
                        matchTime = json.optDouble(
                            "time",
                            matchTime.toDouble()
                        ).toFloat()
                    }
                }
            },
            onEvent = { event, json ->
                if (
                    event == "match_finished" &&
                    !isHost &&
                    !matchCompleted
                ) {
                    scope.launch {
                        matchCompleted = true
                        running = false
                        val results = json.optJSONArray("results")
                            ?: JSONArray()
                        var own = JSONObject()
                        repeat(results.length()) { index ->
                            val item = results.getJSONObject(index)
                            if (
                                item.optString("userId") ==
                                session.userId
                            ) {
                                own = item
                            }
                        }
                        onFinished(
                            ArenaResult(
                                rank = own.optInt("rank", 10),
                                kills = own.optInt("kills"),
                                deaths = own.optInt("deaths"),
                                winner = json.optString(
                                    "winner",
                                    "M4X"
                                ),
                                matchId = ticket.matchId,
                                allResultsJson = results.toString()
                            )
                        )
                    }
                }
            },
            onError = { message ->
                scope.launch {
                    realtimeConnected = false
                    announcement = message
                }
            }
        )
        room = realtime
        realtime.connect()

        onDispose {
            realtime.close()
            room = null
        }
    }

    LaunchedEffect(ticket.matchId) {
        delay(8_000)
        if (!realtimeConnected && !matchCompleted) {
            running = false
            onMessage(
                "Không kết nối được phòng Arena Online. " +
                    "Hãy kiểm tra mạng rồi tìm trận lại."
            )
            onQuit()
        }
    }

    LaunchedEffect(
        realtimeConnected,
        isHost,
        matchCompleted
    ) {
        if (!realtimeConnected || isHost || matchCompleted) {
            return@LaunchedEffect
        }

        lastRemoteSnapshotAt = System.currentTimeMillis()
        while (running && !matchCompleted) {
            delay(2_000)
            if (
                System.currentTimeMillis() -
                    lastRemoteSnapshotAt > 6_000
            ) {
                running = false
                onMessage(
                    "Chủ phòng đã mất kết nối. " +
                        "Trận bị hủy và không cộng Coin."
                )
                onQuit()
                break
            }
        }
    }

    LaunchedEffect(realtimeConnected, isHost) {
        if (!realtimeConnected || isHost) return@LaunchedEffect

        while (running && !matchCompleted) {
            room?.sendInput(
                JSONObject()
                    .put("slot", localActorId)
                    .put("moveX", moveInput.x)
                    .put("moveY", moveInput.y)
                    .put("firing", firing)
                    .put("reload", reloadPulse)
                    .put("heal", healPulse)
                    .put("at", System.currentTimeMillis())
            )
            reloadPulse = false
            healPulse = false
            delay(100)
        }
    }

    LaunchedEffect(running, isHost, realtimeConnected) {
        if (
            !running ||
            !isHost ||
            !realtimeConnected
        ) return@LaunchedEffect
        var previous = System.nanoTime()
        var lastSnapshotAt = 0L

        while (running) {
            val now = System.nanoTime()
            val dt = ((now - previous) / 1_000_000_000f)
                .coerceIn(0.012f, 0.045f)
            previous = now

            controls[localActorId] = ArenaControl(
                move = moveInput,
                firing = firing,
                reload = reloadPulse,
                heal = healPulse
            )
            reloadPulse = false
            healPulse = false

            val nextActors = simulateArenaActors(
                actors = actors.toList(),
                bullets = bullets.toList(),
                pickups = pickups.toList(),
                controls = controls,
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

            controls.entries.forEach { entry ->
                if (entry.value.reload || entry.value.heal) {
                    controls[entry.key] = entry.value.copy(
                        reload = false,
                        heal = false
                    )
                }
            }

            if (
                realtimeConnected &&
                System.currentTimeMillis() - lastSnapshotAt >= 100L
            ) {
                lastSnapshotAt = System.currentTimeMillis()
                room?.sendSnapshot(
                    JSONObject()
                        .put("time", matchTime)
                        .put(
                            "actors",
                            JSONArray().apply {
                                actors.forEach {
                                    put(it.toNetJson())
                                }
                            }
                        )
                        .put(
                            "bullets",
                            JSONArray().apply {
                                bullets.forEach {
                                    put(it.toNetJson())
                                }
                            }
                        )
                        .put(
                            "pickups",
                            JSONArray().apply {
                                pickups.forEach {
                                    put(it.toNetJson())
                                }
                            }
                        )
                )
            }

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
                val player = actors.first {
                    it.id == localActorId
                }
                val rank = ranking.indexOfFirst {
                    it.id == localActorId
                } + 1
                val resultsJson = arenaResultsJson(actors)
                val winnerName =
                    ranking.firstOrNull()?.name ?: "Bot"

                matchCompleted = true
                room?.sendEvent(
                    "match_finished",
                    JSONObject()
                        .put("winner", winnerName)
                        .put("results", resultsJson)
                )
                onFinished(
                    ArenaResult(
                        rank = rank,
                        kills = player.kills,
                        deaths = player.deaths,
                        winner = winnerName,
                        matchId = ticket.matchId,
                        allResultsJson = resultsJson.toString()
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
            localActorId = localActorId,
            online = realtimeConnected,
            isHost = isHost,
            onQuit = { showQuitDialog = true }
        )

        if (announcement.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp),
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    announcement,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 6.dp
                    ),
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = 8.dp, bottom = 7.dp)
                .size(124.dp)
                .pointerInput(joystickTravel) {
                    detectDragGestures(
                        onDragStart = { start ->
                            updateJoystick(start, size)
                        },
                        onDragEnd = {
                            joystickKnob = Offset.Zero
                            moveInput = Offset.Zero
                        },
                        onDragCancel = {
                            joystickKnob = Offset.Zero
                            moveInput = Offset.Zero
                        }
                    ) { change, _ ->
                        change.consume()
                        updateJoystick(change.position, size)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val center = this.center
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x4429B6F6),
                            Color(0x33111B2B),
                            Color(0x22000000)
                        ),
                        center = center,
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f - 3f,
                    center = center
                )
                drawCircle(
                    color = Color(0xAA52D7FF),
                    radius = size.minDimension / 2f - 4f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.13f),
                    radius = size.minDimension * 0.27f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
                drawLine(
                    Color.White.copy(alpha = 0.09f),
                    Offset(center.x, 13f),
                    Offset(center.x, size.height - 13f),
                    1.5f
                )
                drawLine(
                    Color.White.copy(alpha = 0.09f),
                    Offset(13f, center.y),
                    Offset(size.width - 13f, center.y),
                    1.5f
                )
            }

            Box(
                Modifier
                    .size(48.dp)
                    .offset {
                        IntOffset(
                            joystickKnob.x.roundToInt(),
                            joystickKnob.y.roundToInt()
                        )
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xDDF5FBFF),
                                Color(0xCC80DEEA),
                                Color(0xBB1565C0)
                            )
                        ),
                        CircleShape
                    )
                    .border(
                        2.dp,
                        Color.White.copy(alpha = 0.86f),
                        CircleShape
                    )
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(
                        Color.White.copy(alpha = 0.26f),
                        radius = size.minDimension * 0.34f,
                        center = center.copy(
                            x = center.x - size.minDimension * 0.08f,
                            y = center.y - size.minDimension * 0.1f
                        )
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 9.dp, bottom = 8.dp)
                .size(82.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF8E5BFF),
                            Color(0xFF4A1F8C),
                            Color(0xDD0A1020)
                        )
                    ),
                    CircleShape
                )
                .border(
                    3.dp,
                    Color(0xFFD7C8FF).copy(alpha = 0.75f),
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
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.38f,
                    center = center
                )
                drawCircle(
                    Color(0xFFB388FF).copy(alpha = 0.24f),
                    radius = size.minDimension * 0.48f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.MyLocation,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
                Text(
                    "BẮN",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        ArenaActionButtons(
            actor = actors.firstOrNull {
                it.id == localActorId
            },
            onReload = {
                reloadPulse = true
            },
            onHeal = {
                healPulse = true
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
    localActorId: Int,
    online: Boolean,
    isHost: Boolean,
    onQuit: () -> Unit
) {
    val player = actors.firstOrNull {
        it.id == localActorId
    }
    val topThree = actors.sortedWith(
        compareByDescending<ArenaActor> { it.kills }
            .thenBy { it.deaths }
    ).take(3)

    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 5.dp, vertical = 4.dp)
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(112.dp),
            color = Color(0x88030A13),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFF29B6F6).copy(alpha = 0.28f)
            )
        ) {
            Column(
                Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                topThree.forEachIndexed { index, actor ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(5.dp)
                                .background(
                                    arenaActorColor(actor.id),
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${index + 1}. ${actor.name}",
                            color = when {
                                actor.id == localActorId ->
                                    Color(0xFFFFD740)
                                actor.isPlayer ->
                                    Color(0xFF80DEEA)
                                else ->
                                    Color.White.copy(alpha = 0.84f)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (
                                actor.id == localActorId
                            ) {
                                FontWeight.Black
                            } else {
                                FontWeight.Medium
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            actor.kills.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopCenter),
            color = Color(0x88030A13),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFF7C4DFF).copy(alpha = 0.3f)
            )
        ) {
            Text(
                "%02d:%02d".format(
                    matchTime.toInt() / 60,
                    matchTime.toInt() % 60
                ),
                modifier = Modifier.padding(
                    horizontal = 14.dp,
                    vertical = 4.dp
                ),
                color = Color.White,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 35.dp),
            color = if (online) {
                Color(0xAA123D2B)
            } else {
                Color(0xAA5B1E28)
            },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                when {
                    !online -> "ĐANG KẾT NỐI"
                    isHost -> "ONLINE • HOST"
                    else -> "ONLINE"
                },
                modifier = Modifier.padding(
                    horizontal = 7.dp,
                    vertical = 2.dp
                ),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ArenaMiniMap(actors = actors)
            FilledIconButton(
                onClick = onQuit,
                modifier = Modifier.size(30.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color(0x88030A13)
                )
            ) {
                Icon(
                    Icons.Default.Tune,
                    "Cài đặt và thoát",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (player != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(268.dp)
                    .padding(bottom = 2.dp),
                color = Color(0x99030A13),
                shape = RoundedCornerShape(13.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF29B6F6).copy(alpha = 0.26f)
                )
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.HealthAndSafety,
                        null,
                        tint = Color(0xFFFF5C6C),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${player.health.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(5.dp))
                    LinearProgressIndicator(
                        progress = {
                            (player.health / 100f).coerceIn(0f, 1f)
                        },
                        modifier = Modifier
                            .width(52.dp)
                            .height(5.dp),
                        color = Color(0xFF69F0AE),
                        trackColor = Color.White.copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Default.Security,
                        null,
                        tint = Color(0xFF40C4FF),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${player.armor.toInt()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            player.weapon.name,
                            color = Color(0xFF80DEEA),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            "${player.ammo}/${player.reserveAmmo}",
                            color = if (player.ammo <= 5) {
                                Color(0xFFFF5252)
                            } else {
                                Color(0xFFFFD740)
                            },
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (player.respawnRemaining > 0f) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 54.dp),
                    color = Color(0xAA03111E),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFF80DEEA).copy(alpha = 0.58f)
                    )
                ) {
                    Text(
                        "HỒI SINH ${max(1, kotlin.math.ceil(player.respawnRemaining).toInt())}",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 5.dp
                        ),
                        color = Color(0xFFB2FFFF),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun ArenaMiniMap(
    actors: List<ArenaActor>
) {
    Surface(
        color = Color(0x88030A13),
        shape = RoundedCornerShape(9.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFF29B6F6).copy(alpha = 0.35f)
        )
    ) {
        Canvas(
            Modifier
                .width(78.dp)
                .height(48.dp)
                .padding(4.dp)
        ) {
            drawRoundRect(
                Color(0xFF0B1827),
                cornerRadius =
                    androidx.compose.ui.geometry.CornerRadius(
                        7f,
                        7f
                    )
            )
            arenaObstacles.forEach { rect ->
                drawRoundRect(
                    Color.White.copy(alpha = 0.12f),
                    topLeft = Offset(
                        rect.left / ARENA_WIDTH * size.width,
                        rect.top / ARENA_HEIGHT * size.height
                    ),
                    size = Size(
                        rect.width / ARENA_WIDTH * size.width,
                        rect.height / ARENA_HEIGHT * size.height
                    ),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            2f,
                            2f
                        )
                )
            }
            actors.filter { it.alive }.forEach { actor ->
                val position = Offset(
                    actor.position.x / ARENA_WIDTH * size.width,
                    actor.position.y / ARENA_HEIGHT * size.height
                )
                drawCircle(
                    if (actor.isPlayer) {
                        Color.White.copy(alpha = 0.35f)
                    } else {
                        arenaActorColor(actor.id).copy(alpha = 0.2f)
                    },
                    radius = if (actor.isPlayer) 6f else 4.5f,
                    center = position
                )
                drawCircle(
                    if (actor.isPlayer) {
                        Color(0xFFFFD740)
                    } else {
                        arenaActorColor(actor.id)
                    },
                    radius = if (actor.isPlayer) 3.4f else 2.6f,
                    center = position
                )
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
                end = 96.dp,
                bottom = 9.dp
            ),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        FilledIconButton(
            onClick = onReload,
            enabled = actor?.reloadRemaining == 0f,
            modifier = Modifier.size(36.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xD91A2740)
            )
        ) {
            Icon(
                Icons.Default.Refresh,
                "Thay đạn",
                tint = Color(0xFF80DEEA)
            )
        }
        FilledIconButton(
            onClick = onHeal,
            enabled = (actor?.medkits ?: 0) > 0,
            modifier = Modifier.size(39.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color(0xD91A2740)
            )
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.HealthAndSafety,
                    "Cứu thương",
                    tint = Color(0xFFFF6E7D),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "${actor?.medkits ?: 0}",
                    color = Color.White,
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
                Surface(
                    color = if (result.reward > 0) {
                        Color(0xFF193D2B)
                    } else {
                        Color(0xFF2B2438)
                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        Modifier.padding(
                            horizontal = 22.dp,
                            vertical = 12.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "+${result.reward} M4X COIN",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            if (result.reward > 0) {
                                "Đã được máy chủ cộng vào tài khoản"
                            } else {
                                "Chưa có thưởng hoặc đã đạt giới hạn"
                            },
                            color = Color.White.copy(alpha = 0.72f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
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
    controls: Map<Int, ArenaControl>,
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
            botModeTimer = max(0f, actor.botModeTimer - dt),
            spawnShieldRemaining = max(
                0f,
                actor.spawnShieldRemaining - dt
            )
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
                    reloadRemaining = 0f,
                    spawnShieldRemaining = 2.4f
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
                        (controls[next.id]?.move ?: Offset.Zero)
                            .normalizedOrZero() *
                            next.moveSpeed * 0.58f
                    } else {
                        next.velocity * 0.82f
                    }
                )
            }
        }

        if (next.isPlayer) {
            val control = controls[next.id] ?: ArenaControl()

            if (
                control.reload &&
                next.reloadRemaining <= 0f
            ) {
                next = startReload(next)
            }

            if (
                control.heal &&
                next.medkits > 0 &&
                next.health in 1f..94.9f
            ) {
                next = next.copy(
                    health = min(100f, next.health + 55f),
                    medkits = next.medkits - 1
                )
            }

            val velocity = control.move
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

            if (
                control.firing &&
                next.reloadRemaining <= 0f
            ) {
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
        fireCooldown = actor.weapon.fireInterval,
        spawnShieldRemaining = 0f
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

                if (target.spawnShieldRemaining > 0f) {
                    repeat(10) {
                        particles += ArenaParticle(
                            position = target.position,
                            velocity = Offset(
                                Random.nextFloat() * 180f - 90f,
                                Random.nextFloat() * 180f - 90f
                            ),
                            life = 0.22f + Random.nextFloat() * 0.22f,
                            radius = 3f + Random.nextFloat() * 4f,
                            color = Color(0xFF80DEEA)
                        )
                    }
                    return@forEach
                }

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
                        spawnShieldRemaining = 0f,
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
    val offsetX = 0f
    val offsetY = 0f
    val worldSize = size

    // Vị trí dùng hai tỉ lệ riêng để bản đồ phủ kín mọi màn hình 18:9–22:9.
    // Kích thước nhân vật vẫn dùng tỉ lệ nhỏ hơn để không bị kéo méo.
    fun world(point: Offset): Offset = Offset(
        point.x * scaleX,
        point.y * scaleY
    )

    drawRect(Color(0xFF03050B))

    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF081224),
                Color(0xFF11162A),
                Color(0xFF07151D),
                Color(0xFF060914)
            ),
            start = Offset(offsetX, offsetY),
            end = Offset(
                offsetX + worldSize.width,
                offsetY + worldSize.height
            )
        ),
        topLeft = Offset(offsetX, offsetY),
        size = worldSize
    )

    // Các tấm sàn kim loại giúp bản đồ có chiều sâu mà không cần ảnh nền nặng.
    val panel = 90f * scale
    var panelY = offsetY
    var row = 0
    while (panelY < offsetY + worldSize.height) {
        var panelX = offsetX
        var column = 0
        while (panelX < offsetX + worldSize.width) {
            val alternate = (row + column) % 2 == 0
            drawRect(
                color = if (alternate) {
                    Color.White.copy(alpha = 0.018f)
                } else {
                    Color.Black.copy(alpha = 0.08f)
                },
                topLeft = Offset(panelX + 1f, panelY + 1f),
                size = Size(
                    min(panel - 2f, offsetX + worldSize.width - panelX),
                    min(panel - 2f, offsetY + worldSize.height - panelY)
                )
            )
            panelX += panel
            column += 1
        }
        panelY += panel
        row += 1
    }

    val gridX = 45f * scaleX
    val gridY = 45f * scaleY
    var x = offsetX
    while (x <= offsetX + worldSize.width) {
        drawLine(
            Color(0xFF4FC3F7).copy(alpha = 0.055f),
            Offset(x, offsetY),
            Offset(x, offsetY + worldSize.height),
            max(0.7f, scale)
        )
        x += gridX
    }
    var y = offsetY
    while (y <= offsetY + worldSize.height) {
        drawLine(
            Color(0xFF7E57C2).copy(alpha = 0.05f),
            Offset(offsetX, y),
            Offset(offsetX + worldSize.width, y),
            max(0.7f, scale)
        )
        y += gridY
    }

    // Viền neon của đấu trường.
    drawRoundRect(
        color = Color(0xFF00B8D4).copy(alpha = 0.28f),
        topLeft = Offset(offsetX + 4f, offsetY + 4f),
        size = Size(
            max(1f, worldSize.width - 8f),
            max(1f, worldSize.height - 8f)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            18f * scale,
            18f * scale
        ),
        style = Stroke(width = max(2f, 3f * scale))
    )

    spawnPoints.forEachIndexed { index, spawn ->
        val position = world(spawn)
        val color = arenaActorColor(index)
        drawCircle(
            color.copy(alpha = 0.07f),
            radius = 28f * scale,
            center = position
        )
        drawCircle(
            color.copy(alpha = 0.28f),
            radius = 20f * scale,
            center = position,
            style = Stroke(width = max(1f, 2f * scale))
        )
        drawLine(
            color.copy(alpha = 0.18f),
            position - Offset(12f * scale, 0f),
            position + Offset(12f * scale, 0f),
            max(1f, scale)
        )
        drawLine(
            color.copy(alpha = 0.18f),
            position - Offset(0f, 12f * scale),
            position + Offset(0f, 12f * scale),
            max(1f, scale)
        )
    }

    obstacles.forEachIndexed { index, rect ->
        drawSciFiObstacle(
            rect = rect,
            index = index,
            scaleX = scaleX,
            scaleY = scaleY,
            shapeScale = scale,
            world = { point -> world(point) }
        )
    }

    pickups
        .filter { it.respawnRemaining <= 0f }
        .forEach { pickup ->
            drawArenaPickup(
                pickup = pickup,
                position = world(pickup.position),
                scale = scale
            )
        }

    bullets.forEach { bullet ->
        val start = world(bullet.previousPosition)
        val end = world(bullet.position)
        drawLine(
            color = Color(0xFFFF8A00).copy(alpha = 0.18f),
            start = start,
            end = end,
            strokeWidth = max(5f, 7f * scale),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFFFF3B0),
            start = start,
            end = end,
            strokeWidth = max(1.5f, 2.6f * scale),
            cap = StrokeCap.Round
        )
    }

    particles.forEach { particle ->
        val alpha = particle.life.coerceIn(0f, 1f)
        val position = world(particle.position)
        drawCircle(
            color = particle.color.copy(alpha = alpha * 0.18f),
            radius = particle.radius * scale * 2.6f,
            center = position
        )
        drawCircle(
            color = particle.color.copy(alpha = alpha),
            radius = max(0.8f, particle.radius * scale),
            center = position
        )
    }

    actors.forEach { actor ->
        if (!actor.alive) {
            if (actor.respawnRemaining > 0f) {
                val position = world(actor.position)
                val progress = (
                    1f - actor.respawnRemaining / 3f
                ).coerceIn(0f, 1f)
                drawCircle(
                    Color(0xFF80DEEA).copy(alpha = 0.13f),
                    radius = 29f * scale,
                    center = position
                )
                drawCircle(
                    Color(0xFF80DEEA).copy(alpha = 0.42f),
                    radius = 22f * scale,
                    center = position,
                    style = Stroke(width = max(1.5f, 2.4f * scale))
                )
                drawArc(
                    color = Color(0xFF80DEEA),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = position - Offset(
                        21f * scale,
                        21f * scale
                    ),
                    size = Size(42f * scale, 42f * scale),
                    style = Stroke(width = max(1.5f, 3f * scale))
                )
            }
            return@forEach
        }

        drawArenaFighter(
            actor = actor,
            position = world(actor.position),
            scale = scale
        )
    }
}

private fun DrawScope.drawSciFiObstacle(
    rect: Rect,
    index: Int,
    scaleX: Float,
    scaleY: Float,
    shapeScale: Float,
    world: (Offset) -> Offset
) {
    val scale = shapeScale
    val topLeft = world(Offset(rect.left, rect.top))
    val obstacleSize = Size(
        rect.width * scaleX,
        rect.height * scaleY
    )
    val accent = if (index % 3 == 0) {
        Color(0xFF00B8D4)
    } else if (index % 3 == 1) {
        Color(0xFF7C4DFF)
    } else {
        Color(0xFFFF8F00)
    }

    drawRoundRect(
        color = Color.Black.copy(alpha = 0.48f),
        topLeft = topLeft + Offset(7f * scale, 9f * scale),
        size = obstacleSize,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f * scale,
            10f * scale
        )
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(
                Color(0xFF263244),
                Color(0xFF111A29),
                Color(0xFF303847)
            ),
            start = topLeft,
            end = topLeft + Offset(
                obstacleSize.width,
                obstacleSize.height
            )
        ),
        topLeft = topLeft,
        size = obstacleSize,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            10f * scale,
            10f * scale
        )
    )
    drawRoundRect(
        color = accent.copy(alpha = 0.45f),
        topLeft = topLeft + Offset(3f * scale, 3f * scale),
        size = Size(
            max(1f, obstacleSize.width - 6f * scale),
            max(1f, obstacleSize.height - 6f * scale)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            8f * scale,
            8f * scale
        ),
        style = Stroke(width = max(1f, 2f * scale))
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.09f),
        topLeft = topLeft + Offset(12f * scale, 12f * scale),
        size = Size(
            max(1f, obstacleSize.width - 24f * scale),
            max(1f, obstacleSize.height - 24f * scale)
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            5f * scale,
            5f * scale
        ),
        style = Stroke(width = max(1f, 1.4f * scale))
    )

    val boltRadius = max(1.2f, 2.1f * scale)
    listOf(
        Offset(10f, 10f),
        Offset(rect.width - 10f, 10f),
        Offset(10f, rect.height - 10f),
        Offset(rect.width - 10f, rect.height - 10f)
    ).forEach { local ->
        drawCircle(
            Color(0xFF90A4AE),
            boltRadius,
            world(
                Offset(
                    rect.left + local.x,
                    rect.top + local.y
                )
            )
        )
    }

    val stripY = topLeft.y + obstacleSize.height * 0.78f
    repeat(4) { stripe ->
        val stripeX = topLeft.x +
            obstacleSize.width * (0.12f + stripe * 0.18f)
        drawLine(
            accent.copy(alpha = 0.42f),
            Offset(stripeX, stripY),
            Offset(
                stripeX + obstacleSize.width * 0.09f,
                stripY
            ),
            max(1f, 3f * scale),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawArenaPickup(
    pickup: ArenaPickup,
    position: Offset,
    scale: Float
) {
    val color = when (pickup.type) {
        PickupType.MEDKIT -> Color(0xFFFF4D67)
        PickupType.ARMOR -> Color(0xFF29B6F6)
        PickupType.AMMO -> Color(0xFFFFC400)
    }

    drawCircle(
        color.copy(alpha = 0.1f),
        radius = 28f * scale,
        center = position
    )
    drawCircle(
        color.copy(alpha = 0.2f),
        radius = 20f * scale,
        center = position,
        style = Stroke(width = max(1f, 2f * scale))
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color.White.copy(alpha = 0.9f),
                color,
                color.copy(alpha = 0.7f)
            ),
            center = position,
            radius = 12f * scale
        ),
        radius = 11f * scale,
        center = position
    )

    when (pickup.type) {
        PickupType.MEDKIT -> {
            drawLine(
                Color.White,
                position - Offset(5f * scale, 0f),
                position + Offset(5f * scale, 0f),
                max(1.5f, 3f * scale),
                StrokeCap.Round
            )
            drawLine(
                Color.White,
                position - Offset(0f, 5f * scale),
                position + Offset(0f, 5f * scale),
                max(1.5f, 3f * scale),
                StrokeCap.Round
            )
        }
        PickupType.ARMOR -> {
            val shield = Path().apply {
                moveTo(position.x, position.y - 6f * scale)
                lineTo(position.x + 6f * scale, position.y - 3f * scale)
                lineTo(position.x + 4f * scale, position.y + 5f * scale)
                lineTo(position.x, position.y + 8f * scale)
                lineTo(position.x - 4f * scale, position.y + 5f * scale)
                lineTo(position.x - 6f * scale, position.y - 3f * scale)
                close()
            }
            drawPath(shield, Color.White)
        }
        PickupType.AMMO -> {
            repeat(3) { index ->
                val dx = (index - 1) * 4.5f * scale
                drawRoundRect(
                    Color.White,
                    topLeft = Offset(
                        position.x + dx - 1.5f * scale,
                        position.y - 6f * scale
                    ),
                    size = Size(3f * scale, 12f * scale),
                    cornerRadius =
                        androidx.compose.ui.geometry.CornerRadius(
                            1.5f * scale,
                            1.5f * scale
                        )
                )
            }
        }
    }
}

private fun DrawScope.drawArenaFighter(
    actor: ArenaActor,
    position: Offset,
    scale: Float
) {
    val forward = actor.aim.normalizedOrZero().let {
        if (it == Offset.Zero) Offset(1f, 0f) else it
    }
    val side = Offset(-forward.y, forward.x)
    fun local(front: Float, lateral: Float): Offset {
        return position +
            forward * (front * scale) +
            side * (lateral * scale)
    }

    val accent = arenaActorColor(actor.id)
    val stride = if (actor.velocity.getDistance() > 12f) {
        sin((actor.position.x + actor.position.y) * 0.12f) * 3.5f
    } else {
        0f
    }

    drawOval(
        color = Color.Black.copy(alpha = 0.5f),
        topLeft = position + Offset(
            -18f * scale,
            7f * scale
        ),
        size = Size(39f * scale, 18f * scale)
    )

    if (actor.spawnShieldRemaining > 0f) {
        val pulse = 0.5f + 0.5f * sin(
            actor.spawnShieldRemaining * 10f
        )
        val shieldColor = if (actor.isPlayer) {
            Color(0xFF66FFFF)
        } else {
            accent
        }

        drawLine(
            shieldColor.copy(alpha = 0.16f + pulse * 0.12f),
            start = position - Offset(0f, 62f * scale),
            end = position + Offset(0f, 34f * scale),
            strokeWidth = max(8f, 15f * scale),
            cap = StrokeCap.Round
        )
        drawCircle(
            shieldColor.copy(alpha = 0.12f + pulse * 0.09f),
            radius = (31f + pulse * 5f) * scale,
            center = position
        )
        drawCircle(
            shieldColor.copy(alpha = 0.9f),
            radius = (26f + pulse * 3f) * scale,
            center = position,
            style = Stroke(width = max(2f, 3.2f * scale))
        )
        drawArc(
            color = Color.White.copy(alpha = 0.72f),
            startAngle = -90f + actor.spawnShieldRemaining * 110f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = position - Offset(31f * scale, 31f * scale),
            size = Size(62f * scale, 62f * scale),
            style = Stroke(
                width = max(1.5f, 2.4f * scale),
                cap = StrokeCap.Round
            )
        )
    }

    if (actor.armor > 0f) {
        drawCircle(
            accent.copy(
                alpha = 0.1f +
                    0.12f * (
                        actor.armor / actor.maxArmor
                    ).coerceIn(0f, 1f)
            ),
            radius = 23f * scale,
            center = position
        )
        drawCircle(
            accent.copy(alpha = 0.34f),
            radius = 20f * scale,
            center = position,
            style = Stroke(width = max(1f, 1.8f * scale))
        )
    }

    // Chân robot chuyển động ngược pha.
    drawLine(
        color = Color(0xFF0A1019),
        start = local(-6f, -7f),
        end = local(-15f + stride, -9f),
        strokeWidth = max(3f, 7f * scale),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF0A1019),
        start = local(-6f, 7f),
        end = local(-15f - stride, 9f),
        strokeWidth = max(3f, 7f * scale),
        cap = StrokeCap.Round
    )
    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = local(-9f, -7f),
        end = local(-14f + stride, -9f),
        strokeWidth = max(1.2f, 2.2f * scale),
        cap = StrokeCap.Round
    )
    drawLine(
        color = accent.copy(alpha = 0.72f),
        start = local(-9f, 7f),
        end = local(-14f - stride, 9f),
        strokeWidth = max(1.2f, 2.2f * scale),
        cap = StrokeCap.Round
    )

    val body = Path().apply {
        val a = local(-10f, -10f)
        val b = local(7f, -12f)
        val c = local(15f, -7f)
        val d = local(15f, 7f)
        val e = local(7f, 12f)
        val f = local(-10f, 10f)
        moveTo(a.x, a.y)
        lineTo(b.x, b.y)
        lineTo(c.x, c.y)
        lineTo(d.x, d.y)
        lineTo(e.x, e.y)
        lineTo(f.x, f.y)
        close()
    }
    drawPath(
        body,
        Brush.linearGradient(
            listOf(
                Color(0xFF28384D),
                Color(0xFF0A111C),
                accent.copy(alpha = 0.62f)
            ),
            start = local(-12f, -12f),
            end = local(16f, 12f)
        )
    )
    drawPath(
        body,
        Color.White.copy(alpha = 0.22f),
        style = Stroke(width = max(1f, 1.3f * scale))
    )

    // Vai, tay và súng.
    drawCircle(
        accent,
        radius = 5.2f * scale,
        center = local(3f, -12f)
    )
    drawCircle(
        accent,
        radius = 5.2f * scale,
        center = local(3f, 12f)
    )
    drawLine(
        color = Color(0xFF101824),
        start = local(3f, 9f),
        end = local(20f, 9f),
        strokeWidth = max(3f, 7f * scale),
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF05080D),
        start = local(10f, 7f),
        end = local(33f, 7f),
        strokeWidth = max(3f, 6f * scale),
        cap = StrokeCap.Round
    )
    drawLine(
        color = accent.copy(alpha = 0.9f),
        start = local(12f, 7f),
        end = local(29f, 7f),
        strokeWidth = max(1f, 1.7f * scale),
        cap = StrokeCap.Round
    )

    // Mũ nhìn từ trên xuống với kính ngắm neon.
    drawCircle(
        brush = Brush.radialGradient(
            listOf(
                Color(0xFF455A75),
                Color(0xFF101722),
                Color(0xFF05080D)
            ),
            center = local(13f, 0f),
            radius = 10f * scale
        ),
        radius = 9.5f * scale,
        center = local(13f, 0f)
    )
    drawLine(
        color = if (actor.isPlayer) {
            Color(0xFF7CFFFA)
        } else {
            accent.copy(alpha = 0.9f)
        },
        start = local(17f, -5f),
        end = local(20f, 5f),
        strokeWidth = max(1.4f, 2.5f * scale),
        cap = StrokeCap.Round
    )

    if (
        actor.fireCooldown >
        actor.weapon.fireInterval * 0.72f
    ) {
        val muzzle = local(36f, 7f)
        drawCircle(
            Color(0xFFFFA000).copy(alpha = 0.24f),
            radius = 12f * scale,
            center = muzzle
        )
        drawCircle(
            Color(0xFFFFF59D),
            radius = 4f * scale,
            center = muzzle
        )
        repeat(4) { index ->
            val angle = index * 1.5708f
            drawLine(
                Color(0xFFFFC107),
                muzzle,
                muzzle + Offset(
                    cos(angle),
                    sin(angle)
                ) * (10f * scale),
                max(1f, 1.5f * scale),
                StrokeCap.Round
            )
        }
    }

    if (actor.isPlayer) {
        drawCircle(
            if (actor.spawnShieldRemaining > 0f) {
                Color(0xFF66FFFF)
            } else {
                Color(0xFFFFD740).copy(alpha = 0.82f)
            },
            radius = if (actor.spawnShieldRemaining > 0f) {
                29f * scale
            } else {
                24f * scale
            },
            center = position,
            style = Stroke(width = max(1.5f, 2.5f * scale))
        )
        val marker = local(0f, 0f) - Offset(0f, 31f * scale)
        val arrow = Path().apply {
            moveTo(marker.x, marker.y)
            lineTo(
                marker.x - 6f * scale,
                marker.y - 9f * scale
            )
            lineTo(
                marker.x + 6f * scale,
                marker.y - 9f * scale
            )
            close()
        }
        drawPath(arrow, Color(0xFFFFD740))
    }

    val barWidth = 46f * scale
    val barTop = position.y - 31f * scale
    drawRoundRect(
        Color.Black.copy(alpha = 0.72f),
        topLeft = Offset(
            position.x - barWidth / 2f,
            barTop
        ),
        size = Size(barWidth, 5f * scale),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            3f * scale,
            3f * scale
        )
    )
    drawRoundRect(
        brush = Brush.horizontalGradient(
            listOf(
                Color(0xFFFF5252),
                Color(0xFFFFCA28),
                Color(0xFF69F0AE)
            )
        ),
        topLeft = Offset(
            position.x - barWidth / 2f,
            barTop
        ),
        size = Size(
            barWidth * (
                actor.health / 100f
            ).coerceIn(0f, 1f),
            5f * scale
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(
            3f * scale,
            3f * scale
        )
    )

    val paint = android.graphics.Paint().apply {
        color = if (actor.isPlayer) {
            android.graphics.Color.rgb(255, 224, 92)
        } else {
            android.graphics.Color.WHITE
        }
        textSize = max(9f, 11f * scale)
        textAlign = android.graphics.Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(
            4f * scale,
            0f,
            2f * scale,
            android.graphics.Color.BLACK
        )
    }
    drawContext.canvas.nativeCanvas.drawText(
        actor.name,
        position.x,
        position.y - 37f * scale,
        paint
    )
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

