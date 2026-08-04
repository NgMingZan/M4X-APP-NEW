package com.m4xtheme.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private enum class FishingPage {
    HOME,
    MAPS,
    SHOP,
    INVENTORY,
    PLAY
}

private enum class FishingPhase {
    LOADING,
    CASTING,
    REELING,
    CAUGHT,
    ESCAPED,
    ERROR
}

private data class FishingPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val waterTop: Color,
    val waterBottom: Color,
    val accent: Color,
    val land: Color
)

private fun fishingPalette(theme: String): FishingPalette = when (theme) {
    "swamp" -> FishingPalette(
        skyTop = Color(0xFF335C45),
        skyBottom = Color(0xFF132D24),
        waterTop = Color(0xFF315B46),
        waterBottom = Color(0xFF102A25),
        accent = Color(0xFFB7F36B),
        land = Color(0xFF293B25)
    )
    "coral" -> FishingPalette(
        skyTop = Color(0xFF57C7FF),
        skyBottom = Color(0xFF0C69A6),
        waterTop = Color(0xFF13A6D7),
        waterBottom = Color(0xFF003E70),
        accent = Color(0xFFFFD166),
        land = Color(0xFFF2B36F)
    )
    "ice" -> FishingPalette(
        skyTop = Color(0xFFDDF6FF),
        skyBottom = Color(0xFF7AA7C7),
        waterTop = Color(0xFF72D7EA),
        waterBottom = Color(0xFF164768),
        accent = Color(0xFFB9F6FF),
        land = Color(0xFFD8F1FA)
    )
    "legend" -> FishingPalette(
        skyTop = Color(0xFF3A246B),
        skyBottom = Color(0xFF0C102D),
        waterTop = Color(0xFF253A84),
        waterBottom = Color(0xFF07122F),
        accent = Color(0xFFFFD740),
        land = Color(0xFF402F62)
    )
    else -> FishingPalette(
        skyTop = Color(0xFF8BE0FF),
        skyBottom = Color(0xFF4C9FD4),
        waterTop = Color(0xFF39B9D7),
        waterBottom = Color(0xFF075A79),
        accent = Color(0xFFFFD54F),
        land = Color(0xFF4A8E53)
    )
}

private fun rarityColor(rarity: String): Color = when (rarity) {
    "legendary" -> Color(0xFFFFD740)
    "epic" -> Color(0xFFC77DFF)
    "rare" -> Color(0xFF40C4FF)
    "uncommon" -> Color(0xFF69F0AE)
    else -> Color(0xFFE0E0E0)
}

private fun rarityLabel(rarity: String): String = when (rarity) {
    "legendary" -> "Huyền thoại"
    "epic" -> "Sử thi"
    "rare" -> "Hiếm"
    "uncommon" -> "Khá hiếm"
    else -> "Thường"
}

private fun vibratePattern(
    context: Context,
    pattern: LongArray,
    amplitudes: IntArray? = null
) {
    val vibrator = context.getSystemService(
        Context.VIBRATOR_SERVICE
    ) as? Vibrator ?: return

    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = if (amplitudes != null) {
                VibrationEffect.createWaveform(
                    pattern,
                    amplitudes,
                    -1
                )
            } else {
                VibrationEffect.createWaveform(pattern, -1)
            }
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}

@Composable
fun FishingGameScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit,
    onImmersiveChanged: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(FishingPage.HOME) }
    var state by remember { mutableStateOf<FishingGameState?>(null) }
    var selectedMap by remember { mutableStateOf<FishingMapInfo?>(null) }
    var loading by remember { mutableStateOf(true) }
    var actionKey by remember { mutableStateOf<String?>(null) }

    fun refreshState() {
        scope.launch {
            loading = true
            api.fishingState(session)
                .onSuccess {
                    state = it
                    onCoinChanged(it.balance)
                }
                .onFailure {
                    onMessage(
                        it.message ?: "Không tải được M4X Fishing"
                    )
                }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshState()
    }

    BackHandler(enabled = page != FishingPage.HOME) {
        if (page == FishingPage.PLAY) {
            onImmersiveChanged(false)
        }
        page = FishingPage.HOME
    }

    DisposableEffect(Unit) {
        onDispose {
            onImmersiveChanged(false)
        }
    }

    when {
        loading && state == null -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }

        state == null -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Không tải được dữ liệu câu cá")
                Button(onClick = ::refreshState) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tải lại")
                }
            }
        }

        else -> {
            val current = state!!
            when (page) {
                FishingPage.HOME -> FishingHome(
                    profile = profile,
                    state = current,
                    onBack = onBack,
                    onMaps = { page = FishingPage.MAPS },
                    onShop = { page = FishingPage.SHOP },
                    onInventory = {
                        page = FishingPage.INVENTORY
                    }
                )

                FishingPage.MAPS -> FishingMapSelect(
                    state = current,
                    actionKey = actionKey,
                    onBack = { page = FishingPage.HOME },
                    onPlay = { map ->
                        selectedMap = map
                        onImmersiveChanged(true)
                        page = FishingPage.PLAY
                    },
                    onUnlock = { map ->
                        actionKey = "map:${map.code}"
                        scope.launch {
                            api.unlockFishingMap(
                                session,
                                map.code
                            ).onSuccess {
                                onMessage(it.message)
                                onCoinChanged(it.balance)
                                refreshState()
                            }.onFailure {
                                onMessage(
                                    it.message
                                        ?: "Không mở được map"
                                )
                            }
                            actionKey = null
                        }
                    }
                )

                FishingPage.SHOP -> FishingShop(
                    state = current,
                    actionKey = actionKey,
                    onBack = { page = FishingPage.HOME },
                    onBuy = { rod ->
                        actionKey = "rod:${rod.code}"
                        scope.launch {
                            api.buyFishingRod(
                                session,
                                rod.code
                            ).onSuccess {
                                onMessage(it.message)
                                onCoinChanged(it.balance)
                                refreshState()
                            }.onFailure {
                                onMessage(
                                    it.message
                                        ?: "Không mua được cần câu"
                                )
                            }
                            actionKey = null
                        }
                    },
                    onEquip = { rod ->
                        actionKey = "equip:${rod.code}"
                        scope.launch {
                            api.equipFishingRod(
                                session,
                                rod.code
                            ).onSuccess {
                                onMessage(it.message)
                                refreshState()
                            }.onFailure {
                                onMessage(
                                    it.message
                                        ?: "Không trang bị được"
                                )
                            }
                            actionKey = null
                        }
                    }
                )

                FishingPage.INVENTORY -> FishingInventory(
                    state = current,
                    actionKey = actionKey,
                    onBack = { page = FishingPage.HOME },
                    onSellOne = { fish ->
                        actionKey = "sell:${fish.id}"
                        scope.launch {
                            api.sellFishingCatch(
                                session,
                                fish.id
                            ).onSuccess {
                                onMessage(it.message)
                                onCoinChanged(it.balance)
                                refreshState()
                            }.onFailure {
                                onMessage(
                                    it.message
                                        ?: "Không bán được cá"
                                )
                            }
                            actionKey = null
                        }
                    },
                    onSellAll = {
                        actionKey = "sell:all"
                        scope.launch {
                            api.sellAllFishingCatches(session)
                                .onSuccess {
                                    onMessage(it.message)
                                    onCoinChanged(it.balance)
                                    refreshState()
                                }
                                .onFailure {
                                    onMessage(
                                        it.message
                                            ?: "Không bán được cá"
                                    )
                                }
                            actionKey = null
                        }
                    }
                )

                FishingPage.PLAY -> {
                    val map = selectedMap
                    if (map == null) {
                        LaunchedEffect(Unit) {
                            onImmersiveChanged(false)
                            page = FishingPage.MAPS
                        }
                    } else {
                        FishingPlay(
                            api = api,
                            session = session,
                            map = map,
                            mapIndex = current.maps.indexOfFirst {
                                it.code == map.code
                            }.coerceAtLeast(0),
                            mapCount = current.maps.size,
                            balance = current.balance,
                            rod = current.rods.firstOrNull {
                                it.equipped
                            },
                            onMessage = onMessage,
                            onBack = {
                                onImmersiveChanged(false)
                                refreshState()
                                page = FishingPage.HOME
                            },
                            onShop = {
                                onImmersiveChanged(false)
                                refreshState()
                                page = FishingPage.SHOP
                            },
                            onInventory = {
                                onImmersiveChanged(false)
                                refreshState()
                                page = FishingPage.INVENTORY
                            },
                            onInventoryChanged = {
                                refreshState()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingHome(
    profile: Profile?,
    state: FishingGameState,
    onBack: () -> Unit,
    onMaps: () -> Unit,
    onShop: () -> Unit,
    onInventory: () -> Unit
) {
    val equipped = state.rods.firstOrNull { it.equipped }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF8FE3FF),
                        Color(0xFFDDF8FF),
                        Color(0xFFF4FCFF)
                    )
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Color.White.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            "Quay lại",
                            tint = Color(0xFF1F4C75)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "M4X FISHING",
                        color = Color(0xFF173F65),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Chào ${profile?.displayName?.takeIf { it.isNotBlank() } ?: profile?.username ?: "ngư thủ"}",
                        color = Color(0xFF6687A5)
                    )
                }
                Surface(
                    color = Color(0xFFFFE58B),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFFFD35D)
                    )
                ) {
                    Text(
                        "🪙 ${state.balance}",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        color = Color(0xFF885400),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        item {
            FishingHeroCard(
                rod = equipped,
                inventoryCount = state.catches.size,
                inventoryValue = state.inventoryValue
            )
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FishingMenuButton(
                    title = "Đi câu",
                    subtitle = "${state.maps.count { it.unlocked }} map đã mở",
                    icon = Icons.Default.Map,
                    modifier = Modifier.weight(1f),
                    onClick = onMaps
                )
                FishingMenuButton(
                    title = "Cửa hàng",
                    subtitle = "Mua cần và đổi diện mạo",
                    icon = Icons.Default.ShoppingCart,
                    modifier = Modifier.weight(1f),
                    onClick = onShop
                )
            }
        }

        item {
            FishingMenuButton(
                title = "Kho cá",
                subtitle = "${state.catches.size} con • dự kiến ${state.inventoryValue} M4X",
                icon = Icons.Default.Inventory2,
                modifier = Modifier.fillMaxWidth(),
                onClick = onInventory
            )
        }

        item {
            Surface(
                color = Color.White.copy(alpha = 0.90f),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFCDEBFF)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Cách chơi",
                        color = Color(0xFF173F65),
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Bấm KÉO CẦN để gây sát thương, quay tay thu dây và giữ độ căng dưới 120. " +
                            "Dùng kỹ năng đúng lúc để hạ thủy quái.",
                        color = Color(0xFF5D7C99)
                    )
                    Text(
                        "Cá chỉ đổi thành M4X Coin khi bán trong kho. " +
                            "Mở map và mua cần đều được xác nhận bởi Supabase.",
                        color = Color(0xFF1684B5)
                    )
                }
            }
        }
    }
}

@Composable
private fun FishingHeroCard(
    rod: FishingRodInfo?,
    inventoryCount: Int,
    inventoryValue: Int
) {
    FishingArcadeHomeHero(
        rod = rod,
        inventoryCount = inventoryCount,
        inventoryValue = inventoryValue
    )
}

@Composable
private fun FishingMenuButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White.copy(alpha = 0.92f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFCDEBFF)
        )
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color(0xFFDDF5FF),
                shape = CircleShape
            ) {
                Icon(
                    icon,
                    null,
                    tint = Color(0xFF238ED8),
                    modifier = Modifier.padding(11.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    title,
                    color = Color(0xFF173F65),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    color = Color(0xFF6A88A4),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FishingMapSelect(
    state: FishingGameState,
    actionKey: String?,
    onBack: () -> Unit,
    onPlay: (FishingMapInfo) -> Unit,
    onUnlock: (FishingMapInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF9BE8FF), Color(0xFFF2FCFF))
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            FishingHeader(
                title = "Chọn khu câu",
                subtitle = "Mỗi map có khung cảnh, cá và độ khó riêng",
                onBack = onBack
            )
        }

        items(state.maps, key = { it.code }) { map ->
            val palette = fishingArcadePalette(map.theme)
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.White.copy(alpha = 0.94f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFFC9EAFF)
                )
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FishingArcadeMapPreview(map)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                map.name,
                                color = Color(0xFF173F65),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                map.subtitle,
                                color = Color(0xFF6B87A2)
                            )
                        }
                        Text(
                            "★".repeat(map.difficulty),
                            color = Color(0xFFFFA900),
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        map.fishNames.forEach { fish ->
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        fish,
                                        color = Color(0xFF315A7E)
                                    )
                                }
                            )
                        }
                    }

                    if (map.unlocked) {
                        Button(
                            onClick = { onPlay(map) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.accent,
                                contentColor = Color(0xFF173F65)
                            )
                        ) {
                            Icon(Icons.Default.SportsEsports, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "CÂU TẠI ${map.name.uppercase()}",
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onUnlock(map) },
                            enabled = actionKey == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (actionKey == "map:${map.code}") {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Lock, null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("MỞ MAP • ${map.unlockCost} M4X")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingShop(
    state: FishingGameState,
    actionKey: String?,
    onBack: () -> Unit,
    onBuy: (FishingRodInfo) -> Unit,
    onEquip: (FishingRodInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFA6EBFF), Color(0xFFF4FCFF))
                )
            ),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FishingHeader(
                title = "Cửa hàng cần câu",
                subtitle = "Số dư ${state.balance} M4X",
                onBack = onBack
            )
        }

        items(state.rods, key = { it.code }) { rod ->
            ElevatedCard(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (rod.equipped) {
                        Color(0xFFE2FFF1)
                    } else {
                        Color.White.copy(alpha = 0.94f)
                    }
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (rod.equipped) Color(0xFF7BDFB4)
                    else Color(0xFFCDEBFF)
                )
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    FishingArcadeRodPreview(rod)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                rod.name,
                                color = Color(0xFF173F65),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                rod.description,
                                color = Color(0xFF6987A3)
                            )
                        }
                        Text(
                            if (rod.price == 0) "MIỄN PHÍ"
                            else "${rod.price} M4X",
                            color = Color(0xFF966000),
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Lực ${rod.power}") }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("Ổn định ${rod.stability}") }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text("May mắn ${rod.luck}") }
                        )
                    }

                    when {
                        rod.equipped -> FilledTonalButton(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, null)
                            Spacer(Modifier.width(8.dp))
                            Text("ĐANG TRANG BỊ")
                        }

                        rod.owned -> Button(
                            onClick = { onEquip(rod) },
                            enabled = actionKey == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (actionKey == "equip:${rod.code}") {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("TRANG BỊ")
                            }
                        }

                        else -> OutlinedButton(
                            onClick = { onBuy(rod) },
                            enabled = actionKey == null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (actionKey == "rod:${rod.code}") {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.ShoppingCart, null)
                                Spacer(Modifier.width(8.dp))
                                Text("MUA ${rod.price} M4X")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingInventory(
    state: FishingGameState,
    actionKey: String?,
    onBack: () -> Unit,
    onSellOne: (FishingCatchInfo) -> Unit,
    onSellAll: () -> Unit
) {
    var confirmSellAll by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFA7ECFF), Color(0xFFF4FCFF)))),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FishingHeader(
                title = "Kho cá",
                subtitle = "${state.catches.size} con • ${state.inventoryValue} M4X",
                onBack = onBack
            )
        }

        item {
            Button(
                onClick = { confirmSellAll = true },
                enabled = state.catches.isNotEmpty() &&
                    actionKey == null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color(0xFF241600)
                )
            ) {
                if (actionKey == "sell:all") {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Sell, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "BÁN TẤT CẢ • ${state.inventoryValue} M4X",
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        if (state.catches.isEmpty()) {
            item {
                Surface(
                    color = Color.White.copy(alpha = 0.94f),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Chưa có cá. Hãy chọn một map và đi câu.",
                        modifier = Modifier.padding(24.dp),
                        color = Color(0xFF173F65),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        items(state.catches, key = { it.id }) { fish ->
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color.White.copy(alpha = 0.94f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = rarityColor(fish.rarity)
                            .copy(alpha = 0.18f),
                        shape = CircleShape,
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "🐟",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            fish.fishName,
                            color = Color(0xFF173F65),
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "${rarityLabel(fish.rarity)} • " +
                                "${fish.weightGrams / 1000f} kg",
                            color = rarityColor(fish.rarity)
                        )
                        Text(
                            fish.mapName,
                            color = Color(0xFF6987A3),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${fish.sellValue} M4X",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Black
                        )
                        TextButton(
                            onClick = { onSellOne(fish) },
                            enabled = actionKey == null
                        ) {
                            if (actionKey == "sell:${fish.id}") {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Bán")
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmSellAll) {
        AlertDialog(
            onDismissRequest = { confirmSellAll = false },
            title = { Text("Bán toàn bộ cá?") },
            text = {
                Text(
                    "Bạn sẽ nhận tối đa ${state.inventoryValue} M4X Coin. " +
                        "Giới hạn bán theo ngày được máy chủ áp dụng."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmSellAll = false
                        onSellAll()
                    }
                ) {
                    Text("Bán tất cả")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmSellAll = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun FishingHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = Color.White.copy(alpha = 0.90f),
            shape = RoundedCornerShape(16.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    "Quay lại",
                    tint = Color(0xFF1E4B74)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title,
                color = Color(0xFF173F65),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                subtitle,
                color = Color(0xFF6886A2)
            )
        }
    }
}

private data class FishingBattleBuffs(
    val maxHpBonus: Float = 0f,
    val damageBonus: Float = 0f,
    val damageMultiplier: Float = 1f,
    val defenseMultiplier: Float = 1f,
    val cooldownMultiplier: Float = 1f,
    val luckBonus: Int = 0
)

private data class FishingUpgradeCard(
    val id: String,
    val title: String,
    val description: String,
    val symbol: String
)

private val fishingUpgradePool = listOf(
    FishingUpgradeCard(
        id = "ocean_core",
        title = "Tinh Hoa Biển Cả",
        description = "+28 sát thương kéo cần và +25 Máu tối đa.",
        symbol = "☀"
    ),
    FishingUpgradeCard(
        id = "emergency_tonic",
        title = "Thuốc Bổ Khẩn Cấp",
        description = "+45 Máu tối đa và hồi đầy Máu ở lượt kế tiếp.",
        symbol = "✚"
    ),
    FishingUpgradeCard(
        id = "frenzy",
        title = "Cuồng Nộ",
        description = "Tăng x1.28 toàn bộ sát thương kéo và kỹ năng.",
        symbol = "⚡"
    ),
    FishingUpgradeCard(
        id = "titan_line",
        title = "Dây Câu Titan",
        description = "Giảm 18% sát thương thủy quái gây ra.",
        symbol = "⛓"
    ),
    FishingUpgradeCard(
        id = "quick_cast",
        title = "Mạch Nước Nhanh",
        description = "Giảm 14% thời gian hồi của mọi kỹ năng.",
        symbol = "◈"
    ),
    FishingUpgradeCard(
        id = "golden_bait",
        title = "Mồi Câu Hoàng Kim",
        description = "+1 may mắn, tăng cơ hội cá hiếm và chí mạng.",
        symbol = "★"
    )
)

private fun applyFishingUpgrade(
    buffs: FishingBattleBuffs,
    card: FishingUpgradeCard
): FishingBattleBuffs = when (card.id) {
    "ocean_core" -> buffs.copy(
        maxHpBonus = buffs.maxHpBonus + 25f,
        damageBonus = buffs.damageBonus + 28f
    )
    "emergency_tonic" -> buffs.copy(
        maxHpBonus = buffs.maxHpBonus + 45f
    )
    "frenzy" -> buffs.copy(
        damageMultiplier = buffs.damageMultiplier * 1.28f
    )
    "titan_line" -> buffs.copy(
        defenseMultiplier = (
            buffs.defenseMultiplier * 0.82f
            ).coerceAtLeast(0.48f)
    )
    "quick_cast" -> buffs.copy(
        cooldownMultiplier = (
            buffs.cooldownMultiplier * 0.86f
            ).coerceAtLeast(0.55f)
    )
    "golden_bait" -> buffs.copy(
        luckBonus = buffs.luckBonus + 1
    )
    else -> buffs
}

@Composable
private fun FishingPlay(
    api: SupabaseApi,
    session: Session,
    map: FishingMapInfo,
    mapIndex: Int,
    mapCount: Int,
    balance: Long,
    rod: FishingRodInfo?,
    onMessage: (String) -> Unit,
    onBack: () -> Unit,
    onShop: () -> Unit,
    onInventory: () -> Unit,
    onInventoryChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shake = remember { Animatable(0f) }

    var round by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(FishingPhase.LOADING) }
    var cast by remember { mutableStateOf<FishingCastStart?>(null) }
    var result by remember { mutableStateOf<FishingCastFinish?>(null) }
    var buffs by remember { mutableStateOf(FishingBattleBuffs()) }

    var playerMaxHp by remember { mutableFloatStateOf(100f) }
    var playerHp by remember { mutableFloatStateOf(100f) }
    var bossMaxHp by remember { mutableFloatStateOf(1000f) }
    var bossHp by remember { mutableFloatStateOf(1000f) }
    var battleElapsedMs by remember { mutableIntStateOf(0) }
    var attackAccumulator by remember { mutableFloatStateOf(0f) }

    var fireCooldown by remember { mutableFloatStateOf(0f) }
    var freezeCooldown by remember { mutableFloatStateOf(0f) }
    var tornadoCooldown by remember { mutableFloatStateOf(0f) }
    var frozenSeconds by remember { mutableFloatStateOf(0f) }
    var healUses by remember { mutableIntStateOf(1) }
    var finishing by remember { mutableStateOf(false) }
    var criticalFlash by remember { mutableStateOf(false) }
    var bossHitFlash by remember { mutableFloatStateOf(0f) }
    var playerHitFlash by remember { mutableFloatStateOf(0f) }
    var damagePopup by remember { mutableStateOf<String?>(null) }
    var lineTension by remember { mutableFloatStateOf(6f) }
    var pullPulse by remember { mutableIntStateOf(0) }

    val activeRod = rod ?: FishingRodInfo(
        code = "bamboo",
        name = "Cần tre M4X",
        price = 0,
        power = 1,
        stability = 1,
        luck = 1,
        owned = true,
        equipped = true,
        description = ""
    )

    fun screenShake(power: Float = 8f) {
        scope.launch {
            repeat(3) {
                shake.animateTo(
                    power,
                    animationSpec = tween(35)
                )
                shake.animateTo(
                    -power,
                    animationSpec = tween(35)
                )
            }
            shake.animateTo(0f, animationSpec = tween(55))
        }
    }

    fun showDamage(textValue: String) {
        damagePopup = textValue
        scope.launch {
            delay(520)
            if (damagePopup == textValue) {
                damagePopup = null
            }
        }
    }

    fun bossDamage(
        amount: Float,
        label: String,
        vibration: Long = 24L
    ) {
        if (phase != FishingPhase.REELING || finishing) return
        val active = cast ?: return

        val minimumBattleMs = active.minReelMs
        val next = bossHp - amount
        bossHp = if (
            next <= 0f &&
            battleElapsedMs < minimumBattleMs
        ) {
            1f
        } else {
            next.coerceAtLeast(0f)
        }

        bossHitFlash = 1f
        lineTension = (lineTension + 2.5f).coerceAtMost(120f)
        showDamage(label)
        vibratePattern(
            context,
            longArrayOf(0, vibration),
            intArrayOf(0, 105)
        )

        scope.launch {
            delay(90)
            bossHitFlash = 0f
        }
    }

    suspend fun finishBattle(success: Boolean) {
        val active = cast ?: return
        if (finishing) return

        finishing = true
        val quality = (
            if (success) {
                55f +
                    playerHp / playerMaxHp.coerceAtLeast(1f) * 35f +
                    buffs.luckBonus * 3f
            } else {
                12f + playerHp.coerceAtLeast(0f) / 10f
            }
            ).toInt().coerceIn(0, 100)

        api.finishFishingCast(
            session = session,
            castId = active.castId,
            success = success,
            reelDurationMs = battleElapsedMs,
            reelQuality = quality
        ).onSuccess {
            result = it
            if (it.caught) {
                phase = FishingPhase.CAUGHT
                vibratePattern(
                    context,
                    longArrayOf(0, 80, 55, 130, 70, 230),
                    intArrayOf(0, 180, 0, 225, 0, 255)
                )
                screenShake(13f)
                onInventoryChanged()
            } else {
                phase = FishingPhase.ESCAPED
                vibratePattern(
                    context,
                    longArrayOf(0, 110)
                )
            }
        }.onFailure {
            phase = FishingPhase.ERROR
            onMessage(
                it.message ?: "Không xác nhận được trận câu"
            )
        }
        finishing = false
    }

    fun abandonAnd(action: () -> Unit) {
        val active = cast
        if (
            active == null ||
            phase == FishingPhase.LOADING ||
            phase == FishingPhase.ERROR
        ) {
            action()
            return
        }

        scope.launch {
            if (
                phase == FishingPhase.CASTING ||
                phase == FishingPhase.REELING
            ) {
                api.finishFishingCast(
                    session = session,
                    castId = active.castId,
                    success = false,
                    reelDurationMs = battleElapsedMs,
                    reelQuality = 0
                )
            }
            action()
        }
    }

    LaunchedEffect(round) {
        phase = FishingPhase.LOADING
        cast = null
        result = null
        finishing = false
        battleElapsedMs = 0
        attackAccumulator = 0f
        fireCooldown = 0f
        freezeCooldown = 0f
        tornadoCooldown = 0f
        frozenSeconds = 0f
        healUses = 1
        criticalFlash = false
        bossHitFlash = 0f
        playerHitFlash = 0f
        damagePopup = null
        lineTension = 6f
        pullPulse = 0

        api.startFishingCast(session, map.code)
            .onSuccess { newCast ->
                cast = newCast
                phase = FishingPhase.CASTING

                val maxPlayer = (
                    112f +
                        newCast.rodStability * 18f +
                        buffs.maxHpBonus
                    ).coerceAtMost(380f)
                playerMaxHp = maxPlayer
                playerHp = maxPlayer

                bossMaxHp = (
                    550f +
                        newCast.fishDifficulty * 560f +
                        map.difficulty * 260f +
                        round * 90f
                    )
                bossHp = bossMaxHp

                delay(newCast.biteDelayMs.toLong())
                if (phase == FishingPhase.CASTING) {
                    phase = FishingPhase.REELING
                    vibratePattern(
                        context,
                        longArrayOf(0, 120, 60, 185),
                        intArrayOf(0, 185, 0, 255)
                    )
                    screenShake(10f)
                }
            }
            .onFailure {
                phase = FishingPhase.ERROR
                onMessage(
                    it.message ?: "Không thể bắt đầu câu cá"
                )
            }
    }

    LaunchedEffect(phase, cast) {
        val active = cast ?: return@LaunchedEffect
        if (phase != FishingPhase.REELING) {
            return@LaunchedEffect
        }

        var previous = System.nanoTime()

        while (
            phase == FishingPhase.REELING &&
            !finishing
        ) {
            val now = System.nanoTime()
            val dt = (
                (now - previous) / 1_000_000_000f
                ).coerceIn(0.01f, 0.05f)
            previous = now

            battleElapsedMs += (dt * 1000f).toInt()
            fireCooldown = (fireCooldown - dt).coerceAtLeast(0f)
            freezeCooldown = (freezeCooldown - dt).coerceAtLeast(0f)
            tornadoCooldown = (tornadoCooldown - dt).coerceAtLeast(0f)
            frozenSeconds = (frozenSeconds - dt).coerceAtLeast(0f)
            lineTension = (lineTension - dt * 10.5f)
                .coerceAtLeast(2f)

            if (frozenSeconds <= 0f) {
                attackAccumulator += dt
            }

            val attackInterval = (
                1.75f -
                    active.fishDifficulty * 0.055f +
                    active.rodStability * 0.025f
                ).coerceIn(1.05f, 1.8f)

            if (attackAccumulator >= attackInterval) {
                attackAccumulator = 0f

                val rawDamage = (
                    4.5f +
                        active.fishDifficulty * 0.95f +
                        map.difficulty * 0.55f
                    )
                val damage = (
                    rawDamage * buffs.defenseMultiplier
                    ).coerceAtLeast(2f)

                playerHp = (
                    playerHp - damage
                    ).coerceAtLeast(0f)
                lineTension = (
                    lineTension + 5f +
                        active.fishDifficulty * 0.9f
                    ).coerceAtMost(120f)
                playerHitFlash = 1f
                screenShake(7f)
                vibratePattern(
                    context,
                    longArrayOf(0, 45, 35, 70),
                    intArrayOf(0, 115, 0, 175)
                )
                scope.launch {
                    delay(110)
                    playerHitFlash = 0f
                }
            }

            if (bossHp <= 0f) {
                finishBattle(true)
            } else if (
                playerHp <= 0f ||
                lineTension >= 120f ||
                battleElapsedMs >= active.maxReelMs
            ) {
                finishBattle(false)
            }

            delay(16)
        }
    }

    BackHandler {
        abandonAnd(onBack)
    }

    val palette = fishingPalette(map.theme)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF8FE3FF), Color(0xFFF3FCFF))))
            .graphicsLayer {
                translationX = shake.value
            }
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 700.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = if (compact) 8.dp else 12.dp,
                        vertical = if (compact) 5.dp else 9.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    if (compact) 6.dp else 9.dp
                )
            ) {
                FishingArcadeBattleScene(
                    theme = map.theme,
                    cast = cast,
                    bossHpFraction = (
                        bossHp / bossMaxHp.coerceAtLeast(1f)
                        ).coerceIn(0f, 1f),
                    bossHitFlash = bossHitFlash,
                    criticalFlash = criticalFlash,
                    frozen = frozenSeconds > 0f,
                    rodCode = activeRod.code,
                    pullPulse = pullPulse,
                    tension = lineTension,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (compact) 0.37f else 0.42f)
                )

                FishingBossInfo(
                    map = map,
                    mapIndex = mapIndex,
                    mapCount = mapCount,
                    balance = balance,
                    cast = cast,
                    playerHp = playerHp,
                    playerMaxHp = playerMaxHp,
                    bossHp = bossHp,
                    bossMaxHp = bossMaxHp,
                    lineTension = lineTension,
                    playerHitFlash = playerHitFlash,
                    compact = compact
                )

                FishingBossControls(
                    phase = phase,
                    cast = cast,
                    rod = activeRod,
                    fireCooldown = fireCooldown,
                    freezeCooldown = freezeCooldown,
                    tornadoCooldown = tornadoCooldown,
                    healUses = healUses,
                    frozenSeconds = frozenSeconds,
                    compact = compact,
                    onPull = {
                        val active = cast
                        if (
                            active == null ||
                            phase != FishingPhase.REELING ||
                            finishing
                        ) return@FishingBossControls

                        val critChance = (
                            0.04f +
                                active.rodLuck * 0.035f +
                                buffs.luckBonus * 0.028f
                            ).coerceAtMost(0.38f)
                        val critical = Math.random() < critChance.toDouble()

                        var damage = (
                            22f +
                                active.rodPower * 17f +
                                buffs.damageBonus
                            ) * buffs.damageMultiplier
                        damage *= (
                            0.90f + Math.random().toFloat() * 0.22f
                            )
                        if (critical) {
                            damage *= 1.82f
                        }

                        pullPulse += 1
                        lineTension = (
                            lineTension + 6f +
                                active.rodPower * 1.15f
                            ).coerceAtMost(120f)
                        criticalFlash = critical
                        bossDamage(
                            damage,
                            if (critical) {
                                "CHÍ MẠNG -${damage.toInt()}"
                            } else {
                                "-${damage.toInt()}"
                            },
                            if (critical) 48L else 22L
                        )
                        scope.launch {
                            delay(120)
                            criticalFlash = false
                        }
                    },
                    onFire = {
                        val active = cast
                        if (
                            active == null ||
                            fireCooldown > 0f ||
                            phase != FishingPhase.REELING
                        ) return@FishingBossControls

                        val damage = (
                            170f +
                                active.rodPower * 62f +
                                buffs.damageBonus * 1.4f
                            ) * buffs.damageMultiplier
                        bossDamage(
                            damage,
                            "CÂU LỬA -${damage.toInt()}",
                            85L
                        )
                        fireCooldown = (
                            18f * buffs.cooldownMultiplier
                            )
                        screenShake(11f)
                    },
                    onFreeze = {
                        if (
                            freezeCooldown > 0f ||
                            phase != FishingPhase.REELING
                        ) return@FishingBossControls

                        frozenSeconds = (
                            3.4f +
                                activeRod.stability * 0.22f
                            )
                        freezeCooldown = (
                            24f * buffs.cooldownMultiplier
                            )
                        vibratePattern(
                            context,
                            longArrayOf(0, 45, 35, 45)
                        )
                    },
                    onTornado = {
                        val active = cast
                        if (
                            active == null ||
                            tornadoCooldown > 0f ||
                            phase != FishingPhase.REELING
                        ) return@FishingBossControls

                        val damage = (
                            260f +
                                active.rodPower * 88f +
                                buffs.damageBonus * 1.8f
                            ) * buffs.damageMultiplier
                        bossDamage(
                            damage,
                            "LỐC XOÁY -${damage.toInt()}",
                            105L
                        )
                        playerHp = (
                            playerHp + playerMaxHp * 0.08f
                            ).coerceAtMost(playerMaxHp)
                        tornadoCooldown = (
                            31f * buffs.cooldownMultiplier
                            )
                        screenShake(13f)
                    },
                    onHeal = {
                        if (
                            healUses <= 0 ||
                            phase != FishingPhase.REELING
                        ) return@FishingBossControls

                        healUses -= 1
                        playerHp = (
                            playerHp + playerMaxHp * 0.46f
                            ).coerceAtMost(playerMaxHp)
                        lineTension = (lineTension - 26f)
                            .coerceAtLeast(4f)
                        vibratePattern(
                            context,
                            longArrayOf(0, 45, 40, 80)
                        )
                    },
                    onShop = {
                        abandonAnd(onShop)
                    },
                    onInventory = {
                        abandonAnd(onInventory)
                    }
                )
            }

            damagePopup?.let {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-72).dp),
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        it,
                        modifier = Modifier.padding(
                            horizontal = 13.dp,
                            vertical = 7.dp
                        ),
                        color = if (it.contains("CHÍ MẠNG")) {
                            Color(0xFFFF9800)
                        } else {
                            Color(0xFF17466F)
                        },
                        fontWeight = FontWeight.Black
                    )
                }
            }

            when (phase) {
                FishingPhase.LOADING -> FishingBossMessage(
                    title = "ĐANG CHUẨN BỊ",
                    subtitle = "Máy chủ đang chọn thủy quái…"
                )
                FishingPhase.CASTING -> FishingBossMessage(
                    title = "ĐÃ THẢ CÂU",
                    subtitle = "Chờ thủy quái cắn mồi…"
                )
                FishingPhase.CAUGHT -> FishingVictoryUpgrade(
                    result = result,
                    options = remember(round) {
                        fishingUpgradePool.shuffled().take(3)
                    },
                    onChoose = { card ->
                        buffs = applyFishingUpgrade(buffs, card)
                        round += 1
                    },
                    onHome = {
                        onBack()
                    }
                )
                FishingPhase.ESCAPED,
                FishingPhase.ERROR -> FishingDefeatOverlay(
                    phase = phase,
                    result = result,
                    onRetry = { round += 1 },
                    onHome = onBack
                )
                else -> Unit
            }
        }
    }
}

@Composable
private fun FishingBossScene(
    palette: FishingPalette,
    phase: FishingPhase,
    cast: FishingCastStart?,
    bossHpFraction: Float,
    bossHitFlash: Float,
    criticalFlash: Boolean,
    frozen: Boolean,
    modifier: Modifier
) {
    val animationTick = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            animationTick.snapTo(0f)
            animationTick.animateTo(
                1f,
                animationSpec = tween(1500)
            )
        }
    }

    Canvas(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                palette.accent.copy(alpha = 0.42f),
                RoundedCornerShape(24.dp)
            )
    ) {
        val t = animationTick.value
        val horizon = size.height * 0.50f

        drawRect(
            brush = Brush.verticalGradient(
                listOf(palette.skyTop, palette.skyBottom),
                endY = horizon
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width,
                horizon
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(palette.waterTop, palette.waterBottom),
                startY = horizon,
                endY = size.height
            ),
            topLeft = androidx.compose.ui.geometry.Offset(0f, horizon),
            size = androidx.compose.ui.geometry.Size(
                size.width,
                size.height - horizon
            )
        )

        drawCircle(
            color = palette.accent.copy(alpha = 0.85f),
            radius = size.minDimension * 0.095f,
            center = androidx.compose.ui.geometry.Offset(
                size.width * 0.86f,
                size.height * 0.18f
            )
        )

        drawRect(
            color = palette.land,
            topLeft = androidx.compose.ui.geometry.Offset(
                0f,
                horizon - 8f
            ),
            size = androidx.compose.ui.geometry.Size(
                size.width * 0.35f,
                size.height * 0.30f
            )
        )

        repeat(6) { index ->
            val y = horizon + index * 22f +
                sin((t * PI * 2 + index).toFloat()) * 4f
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y + 4f),
                strokeWidth = 2f
            )
        }

        val rodStart = androidx.compose.ui.geometry.Offset(
            size.width * 0.17f,
            horizon - 38f
        )
        val rodEnd = androidx.compose.ui.geometry.Offset(
            size.width * 0.48f,
            size.height * 0.16f
        )
        drawLine(
            color = Color(0xFF5D4037),
            start = rodStart,
            end = rodEnd,
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        val fishCenter = androidx.compose.ui.geometry.Offset(
            size.width * 0.70f +
                sin((t * PI * 2).toFloat()) * size.width * 0.08f,
            horizon + size.height * 0.20f +
                cos((t * PI * 2).toFloat()) * 9f
        )

        drawLine(
            color = Color.White.copy(alpha = 0.84f),
            start = rodEnd,
            end = fishCenter,
            strokeWidth = 2f
        )

        val fishColor = when {
            frozen -> Color(0xFF8BE9FD)
            criticalFlash -> Color(0xFFFFD740)
            bossHitFlash > 0f -> Color.White
            else -> rarityColor(cast?.rarity ?: "common")
        }

        val scale = 0.82f + (1f - bossHpFraction) * 0.30f
        val bodyW = size.width * 0.13f * scale
        val bodyH = bodyW * 0.58f
        val fishPath = Path().apply {
            moveTo(fishCenter.x - bodyW * 0.54f, fishCenter.y)
            quadraticBezierTo(
                fishCenter.x,
                fishCenter.y - bodyH,
                fishCenter.x + bodyW * 0.58f,
                fishCenter.y
            )
            quadraticBezierTo(
                fishCenter.x,
                fishCenter.y + bodyH,
                fishCenter.x - bodyW * 0.54f,
                fishCenter.y
            )
            close()
            moveTo(fishCenter.x - bodyW * 0.50f, fishCenter.y)
            lineTo(
                fishCenter.x - bodyW * 0.86f,
                fishCenter.y - bodyH * 0.72f
            )
            lineTo(
                fishCenter.x - bodyW * 0.86f,
                fishCenter.y + bodyH * 0.72f
            )
            close()
        }
        drawPath(
            path = fishPath,
            color = fishColor.copy(alpha = 0.95f)
        )
        drawCircle(
            color = Color.White,
            radius = bodyW * 0.055f,
            center = androidx.compose.ui.geometry.Offset(
                fishCenter.x + bodyW * 0.32f,
                fishCenter.y - bodyH * 0.18f
            )
        )
        drawCircle(
            color = Color.Black,
            radius = bodyW * 0.025f,
            center = androidx.compose.ui.geometry.Offset(
                fishCenter.x + bodyW * 0.32f,
                fishCenter.y - bodyH * 0.18f
            )
        )

        repeat(14) { index ->
            val angle = index * 0.48f + t * 6.28f
            val radius = 26f + index * 3.5f
            drawCircle(
                color = Color.White.copy(alpha = 0.20f),
                radius = 2.5f + index % 3,
                center = androidx.compose.ui.geometry.Offset(
                    fishCenter.x + cos(angle) * radius,
                    fishCenter.y + sin(angle) * radius
                )
            )
        }

        if (frozen) {
            drawCircle(
                color = Color(0xFF80DEEA).copy(alpha = 0.28f),
                radius = bodyW * 0.95f,
                center = fishCenter,
                style = Stroke(width = 5f)
            )
        }

        if (phase != FishingPhase.REELING) {
            drawRect(
                color = Color.Black.copy(alpha = 0.20f),
                size = size
            )
        }
    }
}

@Composable
private fun FishingBossInfo(
    map: FishingMapInfo,
    mapIndex: Int,
    mapCount: Int,
    balance: Long,
    cast: FishingCastStart?,
    playerHp: Float,
    playerMaxHp: Float,
    bossHp: Float,
    bossMaxHp: Float,
    lineTension: Float,
    playerHitFlash: Float,
    compact: Boolean
) {
    Surface(
        color = Color.White.copy(alpha = 0.94f),
        shape = RoundedCornerShape(if (compact) 16.dp else 20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFCBEAFF)
        )
    ) {
        Column(
            Modifier.padding(
                horizontal = if (compact) 11.dp else 15.dp,
                vertical = if (compact) 8.dp else 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) 6.dp else 9.dp
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Map ${mapIndex + 1}: ${map.name}",
                        color = Color(0xFF174B79),
                        fontWeight = FontWeight.Black,
                        style = if (compact) {
                            MaterialTheme.typography.titleSmall
                        } else {
                            MaterialTheme.typography.titleMedium
                        }
                    )
                    Text(
                        "Sức kéo: ${(cast?.fishDifficulty ?: map.difficulty) * 125} kg",
                        color = Color(0xFF6A87A3),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Surface(
                    color = Color(0xFFFFE58B),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text(
                        "🪙 $balance",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        color = Color(0xFF895300),
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Map: ${mapIndex + 1}/$mapCount",
                    color = Color(0xFF6A87A3),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    cast?.fishName ?: "Đang dò cá…",
                    color = rarityColor(cast?.rarity ?: "common"),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            FishingHealthBar(
                label = "Máu Người Chơi",
                value = playerHp,
                maxValue = playerMaxHp,
                color = if (playerHitFlash > 0f) {
                    Color.White
                } else {
                    Color(0xFF19C997)
                }
            )
            FishingHealthBar(
                label = "Máu Thủy Quái",
                value = bossHp,
                maxValue = bossMaxHp,
                color = Color(0xFFFF6072)
            )
            FishingHealthBar(
                label = "Độ căng dây",
                value = lineTension,
                maxValue = 120f,
                color = when {
                    lineTension >= 95f -> Color(0xFFFF5B4A)
                    lineTension >= 70f -> Color(0xFFFFB832)
                    else -> Color(0xFF42CFF5)
                }
            )
        }
    }
}

@Composable
private fun FishingHealthBar(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(
                "$label:",
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${value.coerceAtLeast(0f).toInt()} / ${maxValue.toInt()}",
                color = Color(0xFF365B7A),
                style = MaterialTheme.typography.bodySmall
            )
        }
        LinearProgressIndicator(
            progress = {
                (value / maxValue.coerceAtLeast(1f))
                    .coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape),
            color = color,
            trackColor = Color(0xFFDCEFFA)
        )
    }
}

@Composable
private fun FishingBossControls(
    phase: FishingPhase,
    cast: FishingCastStart?,
    rod: FishingRodInfo,
    fireCooldown: Float,
    freezeCooldown: Float,
    tornadoCooldown: Float,
    healUses: Int,
    frozenSeconds: Float,
    compact: Boolean,
    onPull: () -> Unit,
    onFire: () -> Unit,
    onFreeze: () -> Unit,
    onTornado: () -> Unit,
    onHeal: () -> Unit,
    onShop: () -> Unit,
    onInventory: () -> Unit
) {
    val enabled = phase == FishingPhase.REELING
    val buttonHeight = if (compact) 48.dp else 58.dp
    val skillUnlocked = (cast?.fishDifficulty ?: 1) >= 3 ||
        rod.power >= 3
    val tornadoUnlocked = (cast?.fishDifficulty ?: 1) >= 5 ||
        rod.power >= 4

    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            if (compact) 5.dp else 7.dp
        )
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Button(
                onClick = onPull,
                enabled = enabled,
                modifier = Modifier
                    .weight(1.5f)
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA000),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "🎣 KÉO CẦN!",
                    fontWeight = FontWeight.Black,
                    style = if (compact) {
                        MaterialTheme.typography.bodyMedium
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
            }

            FishingSkillButton(
                title = "🔥 CÂU LỬA",
                cooldown = fireCooldown,
                enabled = enabled && fireCooldown <= 0f,
                color = Color(0xFFE53935),
                modifier = Modifier.weight(0.9f),
                height = buttonHeight,
                onClick = onFire
            )

            FishingSkillButton(
                title = if (frozenSeconds > 0f) {
                    "❄ ĐANG ĐÓNG BĂNG"
                } else {
                    "❄ ĐÓNG BĂNG"
                },
                cooldown = freezeCooldown,
                enabled = enabled &&
                    skillUnlocked &&
                    freezeCooldown <= 0f,
                locked = !skillUnlocked,
                color = Color(0xFF1565C0),
                modifier = Modifier.weight(0.9f),
                height = buttonHeight,
                onClick = onFreeze
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            FishingSkillButton(
                title = "🌪 LỐC XOÁY",
                cooldown = tornadoCooldown,
                enabled = enabled &&
                    tornadoUnlocked &&
                    tornadoCooldown <= 0f,
                locked = !tornadoUnlocked,
                color = Color(0xFF6A1B9A),
                modifier = Modifier.weight(1f),
                height = buttonHeight,
                onClick = onTornado
            )

            FishingSkillButton(
                title = "✚ HỒI MÁU ($healUses)",
                cooldown = 0f,
                enabled = enabled && healUses > 0,
                color = Color(0xFF00897B),
                modifier = Modifier.weight(1f),
                height = buttonHeight,
                onClick = onHeal
            )

            Button(
                onClick = onShop,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2458D3)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "🛒 SHOP",
                    fontWeight = FontWeight.Black
                )
            }

            Button(
                onClick = onInventory,
                modifier = Modifier
                    .weight(1f)
                    .height(buttonHeight),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF049B73)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "🏪 CHỢ CÁ",
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun FishingSkillButton(
    title: String,
    cooldown: Float,
    enabled: Boolean,
    color: Color,
    modifier: Modifier,
    height: androidx.compose.ui.unit.Dp,
    locked: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Color(0xFF263345),
            disabledContentColor = Color.White.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(
            horizontal = 6.dp,
            vertical = 4.dp
        )
    ) {
        Text(
            when {
                locked -> "🔒 KHÓA"
                cooldown > 0f -> "${cooldown.toInt() + 1}s"
                else -> title
            },
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun FishingBossMessage(
    title: String,
    subtitle: String
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.94f),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    color = Color(0xFF174B79),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    subtitle,
                    color = Color(0xFF6A87A3)
                )
            }
        }
    }
}

@Composable
private fun FishingVictoryUpgrade(
    result: FishingCastFinish?,
    options: List<FishingUpgradeCard>,
    onChoose: (FishingUpgradeCard) -> Unit,
    onHome: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x6656A5FF)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.96f),
            shape = RoundedCornerShape(26.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFCDEBFF)
            ),
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "🎉 BẮT ĐƯỢC ${result?.fishName?.uppercase() ?: "CÁ"}!",
                        color = Color(0xFF174B79),
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${rarityLabel(result?.rarity ?: "common")} • " +
                            "${(result?.weightGrams ?: 0) / 1000f} kg • " +
                            "giá ${result?.sellValue ?: 0} M4X",
                        color = Color(0xFF6A87A3),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "🎴 CHỌN THẺ BÀI NÂNG CẤP",
                        color = Color(0xFF174B79),
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                items(options, key = { it.id }) { card ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChoose(card) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = Color(0xFFF1F9FF)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${card.symbol} ${card.title}",
                                color = Color(0xFF174B79),
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                card.description,
                                color = Color(0xFF6685A3),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "[ CHỌN ]",
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                item {
                    TextButton(
                        onClick = onHome,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Về bến và giữ cá trong kho")
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingDefeatOverlay(
    phase: FishingPhase,
    result: FishingCastFinish?,
    onRetry: () -> Unit,
    onHome: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x6656A5FF)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.96f),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Text(
                    if (phase == FishingPhase.ERROR) {
                        "⚠ LỖI TRẬN CÂU"
                    } else {
                        "💨 THỦY QUÁI ĐÃ THOÁT"
                    },
                    color = Color(0xFFFF8A80),
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    result?.message
                        ?: "Nâng cấp cần câu hoặc dùng kỹ năng đúng lúc để chiến thắng.",
                    color = Color(0xFF6685A3),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CÂU LẠI")
                }
                OutlinedButton(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VỀ BẾN")
                }
            }
        }
    }
}

