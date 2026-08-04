package com.m4xtheme.app

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.max
import kotlin.math.min

private enum class GamesPage { HUB, ARENA, FISHING, OBSTACLE, MAZE, TREASURE, PET }

@Composable
fun M4XGamesHubScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit,
    onImmersiveChanged: (Boolean) -> Unit = {},
    startInFishing: Boolean = false,
    onStartInFishingHandled: () -> Unit = {}
) {
    var page by remember {
        mutableStateOf(
            if (startInFishing) GamesPage.FISHING
            else GamesPage.HUB
        )
    }

    LaunchedEffect(Unit) {
        if (startInFishing) onStartInFishingHandled()
    }
    BackHandler(enabled = page != GamesPage.HUB) {
        onImmersiveChanged(false)
        page = GamesPage.HUB
    }

    DisposableEffect(Unit) {
        onDispose { onImmersiveChanged(false) }
    }

    when (page) {
        GamesPage.HUB -> GamesHub(
            profile = profile,
            onBack = onBack,
            onOpen = { page = it },
            api = api,
            session = session,
            onCoinChanged = onCoinChanged,
            onMessage = onMessage
        )
        GamesPage.ARENA -> ArenaGameScreen(
            api = api,
            session = session,
            profile = profile,
            onBack = {
                onImmersiveChanged(false)
                page = GamesPage.HUB
            },
            onCoinChanged = onCoinChanged,
            onMessage = onMessage,
            onImmersiveChanged = onImmersiveChanged
        )
        GamesPage.FISHING -> FishingGameScreen(
            api = api,
            session = session,
            profile = profile,
            onBack = {
                onImmersiveChanged(false)
                page = GamesPage.HUB
            },
            onCoinChanged = onCoinChanged,
            onMessage = onMessage,
            onImmersiveChanged = onImmersiveChanged
        )
        GamesPage.OBSTACLE -> ObstacleGameScreen(api, session, { page = GamesPage.HUB }, onCoinChanged, onMessage)
        GamesPage.MAZE -> MazeGameScreen(api, session, profile, { page = GamesPage.HUB }, onCoinChanged, onMessage)
        GamesPage.TREASURE -> TreasureMapScreen(api, session, { page = GamesPage.HUB }, onCoinChanged, onMessage)
        GamesPage.PET -> PetScreen(api, session, { page = GamesPage.HUB }, onCoinChanged, onMessage)
    }
}

@Composable
private fun GamesHub(
    profile: Profile?,
    onBack: () -> Unit,
    onOpen: (GamesPage) -> Unit,
    api: SupabaseApi,
    session: Session,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var selectedNumber by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf<String?>(null) }
    var balance by remember(profile?.points) { mutableLongStateOf(profile?.points ?: 0L) }
    var result by remember { mutableStateOf<MiniGameResult?>(null) }
    val scope = rememberCoroutineScope()

    fun playClassic(code: String, choice: Int) {
        if (loading != null) return
        loading = code
        scope.launch {
            api.playMiniGame(session, code, choice)
                .onSuccess {
                    balance = it.balance
                    onCoinChanged(it.balance)
                    result = it
                }
                .onFailure { onMessage(it.message ?: "Không thể chơi minigame") }
            loading = null
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
                    Text("M4X GAME CENTER", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Chơi game, săn kho báu và nuôi linh vật", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF34227A), Color(0xFF006D7E))))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Số dư hiện tại", color = Color.White.copy(alpha = .8f))
                    Text("$balance M4X COIN", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Phần thưởng được xác nhận trên Supabase", color = Color.White.copy(alpha = .72f))
                }
            }
        }
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(GamesPage.ARENA) },
                colors = CardDefaults.elevatedCardColors(
                    containerColor = Color(0xFF0D1B2B)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF052E4F),
                                    Color(0xFF251B57),
                                    Color(0xFF080B13)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.MyLocation,
                                null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "M4X ARENA",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    "Bắn súng 10 người • thiếu người tự thêm bot",
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                        Row(
                            Modifier.horizontalScroll(
                                rememberScrollState()
                            ),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text("AI chiến thuật") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("Cửa hàng riêng") }
                            )
                            AssistChip(
                                onClick = {},
                                label = { Text("Hiệu ứng đạn") }
                            )
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
                    .clickable { onOpen(GamesPage.FISHING) }
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
                            "M4X FISHING ARCADE",
                            color = Color(0xFF173F65),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Map động • chibi • boss • shop cần",
                            color = Color(0xFF6687A5)
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
                    Text(
                        "CHƠI NGAY",
                        modifier = Modifier.padding(
                            horizontal = 16.dp,
                            vertical = 11.dp
                        ),
                        color = Color(0xFF744300),
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameMenuCard(
                    title = "Né chướng ngại",
                    subtitle = "Tối đa 200 coin/ngày",
                    icon = Icons.Default.SportsEsports,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen(GamesPage.OBSTACLE) }
                )
                GameMenuCard(
                    title = "Mê cung M4X",
                    subtitle = "50 coin/lượt",
                    icon = Icons.Default.Route,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen(GamesPage.MAZE) }
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameMenuCard(
                    title = "Bản đồ kho báu",
                    subtitle = "Rương 30–500 coin",
                    icon = Icons.Default.Map,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen(GamesPage.TREASURE) }
                )
                GameMenuCard(
                    title = "Thú cưng M4X",
                    subtitle = "Cho ăn và lên cấp",
                    icon = Icons.Default.Pets,
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen(GamesPage.PET) }
                )
            }
        }
        item {
            Text("Game nhanh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text("Hai minigame cũ vẫn được giữ lại", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Đoán số M4X", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        (1..5).forEach { number ->
                            if (number == selectedNumber) {
                                Button({ selectedNumber = number }, Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("$number") }
                            } else {
                                OutlinedButton({ selectedNumber = number }, Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) { Text("$number") }
                            }
                        }
                    }
                    Button(
                        onClick = { playClassic("number_guess", selectedNumber) },
                        enabled = loading == null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (loading == "number_guess") "Đang xử lý…" else "Đoán số $selectedNumber") }
                }
            }
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lật thẻ may mắn", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..3).forEach { card ->
                            ElevatedButton(
                                onClick = { playClassic("lucky_card", card) },
                                enabled = loading == null,
                                modifier = Modifier.weight(1f).height(72.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Style, null)
                                    Text("Thẻ $card")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    result?.let { r ->
        AlertDialog(
            onDismissRequest = { result = null },
            icon = { Icon(Icons.Default.AutoAwesome, null) },
            title = { Text("+${r.reward} M4X COIN") },
            text = { Text("${r.message}\nCòn ${r.remaining} lượt hôm nay") },
            confirmButton = { Button({ result = null }) { Text("Tiếp tục") } }
        )
    }
}

@Composable
private fun GameMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp).heightIn(min = 116.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(34.dp))
            Text(title, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// -------------------------------------------------------------------------
// NÉ CHƯỚNG NGẠI
// -------------------------------------------------------------------------
@Composable
private fun ObstacleGameScreen(
    api: SupabaseApi,
    session: Session,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    var running by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var playerY by remember { mutableFloatStateOf(160f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    var obstacleX by remember { mutableFloatStateOf(900f) }
    var gapY by remember { mutableFloatStateOf(160f) }
    var claimed by remember { mutableStateOf(false) }
    var claiming by remember { mutableStateOf(false) }
    var rewardResult by remember { mutableStateOf<ArcadeRewardResult?>(null) }
    val scope = rememberCoroutineScope()

    fun reset() {
        score = 0
        playerY = if (boardSize.height > 0) boardSize.height * .5f else 160f
        velocity = 0f
        obstacleX = if (boardSize.width > 0) boardSize.width.toFloat() else 900f
        gapY = if (boardSize.height > 0) boardSize.height * .45f else 160f
        gameOver = false
        claimed = false
        rewardResult = null
        running = true
    }

    LaunchedEffect(running, boardSize) {
        if (!running || boardSize == IntSize.Zero) return@LaunchedEffect
        var tick = 0
        val radius = 25f
        val obstacleWidth = 52f
        val gapHeight = max(135f, boardSize.height * .34f)
        while (running) {
            delay(16)
            velocity += .72f
            playerY += velocity
            obstacleX -= 5.8f
            tick++
            if (tick % 6 == 0) score++

            if (obstacleX < -obstacleWidth) {
                obstacleX = boardSize.width + 30f
                gapY = (gapHeight / 2f + 30f) + Random().nextFloat() * max(1f, boardSize.height - gapHeight - 60f)
            }

            val playerX = boardSize.width * .22f
            val inObstacleX = playerX + radius > obstacleX && playerX - radius < obstacleX + obstacleWidth
            val gapTop = gapY - gapHeight / 2f
            val gapBottom = gapY + gapHeight / 2f
            val hitObstacle = inObstacleX && (playerY - radius < gapTop || playerY + radius > gapBottom)
            val hitEdge = playerY - radius <= 0f || playerY + radius >= boardSize.height
            if (hitObstacle || hitEdge) {
                running = false
                gameOver = true
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GameHeader("Né chướng ngại M4X", "Chạm màn hình để logo bay lên", onBack) }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF071323), modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().height(390.dp)
                        .onSizeChanged {
                            boardSize = it
                            if (!running && !gameOver) {
                                playerY = it.height * .5f
                                obstacleX = it.width.toFloat()
                                gapY = it.height * .5f
                            }
                        }
                        .pointerInput(running) {
                            detectTapGestures {
                                if (running) velocity = -10.5f else if (!gameOver) reset()
                            }
                        }
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF09203F), Color(0xFF071323))))
                        val gapHeight = max(135f, size.height * .34f)
                        val gapTop = gapY - gapHeight / 2f
                        val gapBottom = gapY + gapHeight / 2f
                        drawRect(Color(0xFF8E4DFF), topLeft = Offset(obstacleX, 0f), size = Size(52f, max(0f, gapTop)))
                        drawRect(Color(0xFF13C6E6), topLeft = Offset(obstacleX, gapBottom), size = Size(52f, max(0f, size.height - gapBottom)))
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0xFF7B61FF), Color(0xFF00D4E8))),
                            radius = 27f,
                            center = Offset(size.width * .22f, playerY)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "M4X",
                            size.width * .22f - 20f,
                            playerY + 6f,
                            Paint().apply { color = android.graphics.Color.WHITE; textSize = 18f; isFakeBoldText = true }
                        )
                    }
                    Surface(
                        color = Color.Black.copy(alpha = .45f),
                        shape = CircleShape,
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart)
                    ) { Text("Điểm: $score", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.Black) }
                    if (!running) {
                        Column(
                            Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(if (gameOver) Icons.Default.SportsScore else Icons.Default.TouchApp, null, tint = Color.White, modifier = Modifier.size(50.dp))
                            Text(if (gameOver) "Kết thúc • $score điểm" else "Chạm để bắt đầu", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                            Text("Phần thưởng tối đa 200 coin mỗi ngày", color = Color.White.copy(alpha = .75f), textAlign = TextAlign.Center)
                            if (gameOver) Button(onClick = { reset() }) { Text("Chơi lại") }
                        }
                    }
                }
            }
        }
        if (gameOver && !claimed) {
            item {
                Button(
                    onClick = {
                        claiming = true
                        scope.launch {
                            api.claimObstacleReward(session, score)
                                .onSuccess {
                                    rewardResult = it
                                    claimed = true
                                    onCoinChanged(it.balance)
                                }
                                .onFailure { onMessage(it.message ?: "Không thể nhận thưởng") }
                            claiming = false
                        }
                    },
                    enabled = !claiming,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) { Text(if (claiming) "Đang nhận…" else "Nhận thưởng theo điểm", fontWeight = FontWeight.Black) }
            }
        }
        rewardResult?.let { r ->
            item {
                ElevatedCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("+${r.reward} M4X COIN", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Text(r.message)
                        Text("Hôm nay đã nhận: ${r.dailyTotal}/200 coin", color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// MÊ CUNG
// -------------------------------------------------------------------------
private const val WALL_N = 1
private const val WALL_E = 2
private const val WALL_S = 4
private const val WALL_W = 8
private data class MazeBoard(val size: Int, val cells: IntArray)

private fun generateMaze(size: Int, seed: Long): MazeBoard {
    val cells = IntArray(size * size) { WALL_N or WALL_E or WALL_S or WALL_W }
    val visited = BooleanArray(size * size)
    val stack = ArrayDeque<Int>()
    val random = Random(seed)
    fun idx(x: Int, y: Int) = y * size + x
    val start = 0
    visited[start] = true
    stack.add(start)
    while (stack.isNotEmpty()) {
        val current = stack.last()
        val x = current % size
        val y = current / size
        val options = mutableListOf<Triple<Int, Int, Int>>()
        if (y > 0 && !visited[idx(x, y - 1)]) options += Triple(idx(x, y - 1), WALL_N, WALL_S)
        if (x < size - 1 && !visited[idx(x + 1, y)]) options += Triple(idx(x + 1, y), WALL_E, WALL_W)
        if (y < size - 1 && !visited[idx(x, y + 1)]) options += Triple(idx(x, y + 1), WALL_S, WALL_N)
        if (x > 0 && !visited[idx(x - 1, y)]) options += Triple(idx(x - 1, y), WALL_W, WALL_E)
        if (options.isEmpty()) {
            stack.removeLast()
        } else {
            val (next, wallHere, wallThere) = options[random.nextInt(options.size)]
            cells[current] = cells[current] and wallHere.inv()
            cells[next] = cells[next] and wallThere.inv()
            visited[next] = true
            stack.add(next)
        }
    }
    return MazeBoard(size, cells)
}

@Composable
private fun MazeGameScreen(
    api: SupabaseApi,
    session: Session,
    profile: Profile?,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var difficulty by remember { mutableStateOf("easy") }
    var startResult by remember { mutableStateOf<MazeStartResult?>(null) }
    var board by remember { mutableStateOf<MazeBoard?>(null) }
    var playerX by remember { mutableIntStateOf(0) }
    var playerY by remember { mutableIntStateOf(0) }
    var starting by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var finishResult by remember { mutableStateOf<MazeFinishResult?>(null) }
    val scope = rememberCoroutineScope()

    fun start() {
        starting = true
        finishResult = null
        scope.launch {
            api.startMazeGame(session, difficulty)
                .onSuccess { r ->
                    startResult = r
                    onCoinChanged(r.balance)
                    val size = when (r.difficulty) { "easy" -> 8; "medium" -> 12; else -> 16 }
                    board = generateMaze(size, r.seed)
                    playerX = 0
                    playerY = 0
                    startedAt = System.currentTimeMillis()
                }
                .onFailure { onMessage(it.message ?: "Không thể bắt đầu mê cung") }
            starting = false
        }
    }

    fun move(dx: Int, dy: Int) {
        val maze = board ?: return
        if (finishing) return
        val index = playerY * maze.size + playerX
        val wall = when { dx == 1 -> WALL_E; dx == -1 -> WALL_W; dy == 1 -> WALL_S; else -> WALL_N }
        if (maze.cells[index] and wall != 0) return
        playerX += dx
        playerY += dy
        if (playerX == maze.size - 1 && playerY == maze.size - 1) {
            val start = startResult ?: return
            finishing = true
            scope.launch {
                val elapsed = System.currentTimeMillis() - startedAt
                val wait = start.minSeconds * 1000L - elapsed
                if (wait > 0) delay(wait)
                api.finishMazeGame(session, start.sessionId)
                    .onSuccess {
                        finishResult = it
                        onCoinChanged(it.balance)
                        board = null
                        startResult = null
                    }
                    .onFailure { onMessage(it.message ?: "Không thể nhận thưởng mê cung") }
                finishing = false
            }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GameHeader("Mê cung M4X", "Không giới hạn lượt • Phí 50 coin/lượt", onBack) }
        if (board == null) {
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Chọn độ khó", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        listOf(
                            Triple("easy", "Dễ", "Thưởng 30 coin"),
                            Triple("medium", "Trung bình", "Thưởng 100 coin"),
                            Triple("hard", "Khó", "Thưởng 200 coin")
                        ).forEach { (code, title, reward) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { difficulty = code }
                                    .border(if (difficulty == code) 2.dp else 1.dp, if (difficulty == code) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                color = if (difficulty == code) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = difficulty == code, onClick = { difficulty = code })
                                    Column(Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold)
                                        Text(reward, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        Text("Số dư: ${profile?.points ?: 0} coin", color = MaterialTheme.colorScheme.secondary)
                        Button(
                            onClick = { start() },
                            enabled = !starting && (profile?.points ?: 0) >= 50,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text(if (starting) "Đang tạo mê cung…" else "Vào mê cung • 50 coin", fontWeight = FontWeight.Black) }
                    }
                }
            }
            finishResult?.let { r ->
                item {
                    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(18.dp)) {
                            Text("+${r.reward} M4X COIN", fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                            Text(r.message)
                        }
                    }
                }
            }
        } else {
            item {
                MazeCanvas(board!!, playerX, playerY)
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    FilledIconButton(onClick = { move(0, -1) }, enabled = !finishing) { Icon(Icons.Default.KeyboardArrowUp, null) }
                    Row(horizontalArrangement = Arrangement.spacedBy(34.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledIconButton(onClick = { move(-1, 0) }, enabled = !finishing) { Icon(Icons.Default.KeyboardArrowLeft, null) }
                        FilledIconButton(onClick = { move(1, 0) }, enabled = !finishing) { Icon(Icons.Default.KeyboardArrowRight, null) }
                    }
                    FilledIconButton(onClick = { move(0, 1) }, enabled = !finishing) { Icon(Icons.Default.KeyboardArrowDown, null) }
                    if (finishing) Text("Đang xác nhận chiến thắng…", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun MazeCanvas(board: MazeBoard, playerX: Int, playerY: Int) {
    Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFF071323), modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val cell = size.width / board.size
            val wallColor = Color(0xFF57D8F4)
            val stroke = max(1.5f, 6f - board.size * .18f)
            for (y in 0 until board.size) for (x in 0 until board.size) {
                val walls = board.cells[y * board.size + x]
                val left = x * cell
                val top = y * cell
                val right = left + cell
                val bottom = top + cell
                if (walls and WALL_N != 0) drawLine(wallColor, Offset(left, top), Offset(right, top), stroke)
                if (walls and WALL_E != 0) drawLine(wallColor, Offset(right, top), Offset(right, bottom), stroke)
                if (walls and WALL_S != 0) drawLine(wallColor, Offset(left, bottom), Offset(right, bottom), stroke)
                if (walls and WALL_W != 0) drawLine(wallColor, Offset(left, top), Offset(left, bottom), stroke)
            }
            val goalCenter = Offset((board.size - .5f) * cell, (board.size - .5f) * cell)
            drawCircle(Color(0xFFFFB800), radius = cell * .30f, center = goalCenter)
            drawContext.canvas.nativeCanvas.drawText("$", goalCenter.x - cell * .11f, goalCenter.y + cell * .14f, Paint().apply {
                color = android.graphics.Color.BLACK; textSize = cell * .45f; isFakeBoldText = true
            })
            val playerCenter = Offset((playerX + .5f) * cell, (playerY + .5f) * cell)
            drawCircle(Brush.radialGradient(listOf(Color(0xFF925BFF), Color(0xFF00D8E8))), cell * .28f, playerCenter)
        }
    }
}

// -------------------------------------------------------------------------
// BẢN ĐỒ KHO BÁU
// -------------------------------------------------------------------------
@Composable
private fun TreasureMapScreen(
    api: SupabaseApi,
    session: Session,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var state by remember { mutableStateOf<TreasureState?>(null) }
    var loading by remember { mutableStateOf(true) }
    var action by remember { mutableStateOf<String?>(null) }
    var shareInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        loading = true
        scope.launch {
            api.treasureState(session)
                .onSuccess { state = it }
                .onFailure { onMessage(it.message ?: "Không tải được bản đồ") }
            loading = false
        }
    }

    fun runAction(code: String, block: suspend () -> Result<TreasureActionResult>) {
        if (action != null) return
        action = code
        scope.launch {
            block().onSuccess { r ->
                if (r.balance > 0) onCoinChanged(r.balance)
                val message = when {
                    r.openedDay > 0 -> buildString {
                        append("Đã mở ô ngày ${r.openedDay}")
                        if (r.secret && r.bonus > 0) append(" • Ô bí mật +${r.bonus} coin")
                        if (r.rareItem) append(" • Nhận vật phẩm hiếm")
                    }
                    r.rescuedDay > 0 -> "Đã cứu ngày ${r.rescuedDay}"
                    r.damage > 0 -> "Gây ${r.damage} sát thương cho Boss"
                    r.reward > 0 -> "+${r.reward + r.streakBonus} M4X COIN"
                    else -> "Thành công"
                }
                onMessage(message)
                reload()
            }.onFailure { onMessage(it.message ?: "Không thể xử lý") }
            action = null
        }
    }

    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GameHeader("Bản đồ kho báu M4X", "Mở ô mỗi ngày, đủ chìa khóa để nhận rương", onBack) }
        if (loading && state == null) {
            item { Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
        state?.let { s ->
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF102A4C), Color(0xFF30155D))))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, null, tint = Color(0xFFFFC443), modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${s.keys}/7 mảnh chìa khóa", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                Text("Tuần bắt đầu ${s.weekStart}", color = Color.White.copy(alpha = .7f))
                            }
                            Text("Chuỗi ${s.streakWeeks} tuần", color = Color(0xFFFFC443), fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { (s.openedDays.size / 7f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                        )
                    }
                }
            }
            item {
                Text("Hành trình 7 ngày", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    (1..7).forEach { day ->
                        val opened = day in s.openedDays
                        val today = day == s.day
                        Surface(
                            modifier = Modifier.width(92.dp).height(112.dp)
                                .border(if (today) 2.dp else 1.dp, if (today) Color(0xFFFFB800) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = when { opened -> MaterialTheme.colorScheme.primaryContainer; today -> MaterialTheme.colorScheme.surfaceVariant; else -> MaterialTheme.colorScheme.surface }
                        ) {
                            Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("Ngày $day", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Icon(
                                    when { opened && s.secretDay == day -> Icons.Default.AutoAwesome; opened -> Icons.Default.CheckCircle; day == 7 -> Icons.Default.Inventory2; else -> Icons.Default.Lock },
                                    null,
                                    tint = when { opened && s.secretDay == day -> Color(0xFFFFB800); opened -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.onSurfaceVariant },
                                    modifier = Modifier.size(30.dp)
                                )
                                Text(if (opened) "Đã mở" else if (today) "Hôm nay" else "Chưa mở", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Nhiệm vụ hôm nay", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Text(s.taskTitle)
                        LinearProgressIndicator(
                            progress = { (s.taskProgress.toFloat() / max(1, s.taskTarget)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(9.dp).clip(CircleShape)
                        )
                        Text("${min(s.taskProgress, s.taskTarget)}/${s.taskTarget}", color = MaterialTheme.colorScheme.secondary)
                        Button(
                            onClick = { runAction("day") { api.claimTreasureDay(session) } },
                            enabled = action == null && s.taskProgress >= s.taskTarget && s.day !in s.openedDays,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (s.day in s.openedDays) "Ô hôm nay đã mở" else "Nhận mảnh chìa khóa và mở ô") }
                    }
                }
            }
            item {
                Text("Rương theo cấp độ", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TreasureChestCard("Rương Đồng", "3 ô", "30–80 coin", s.openedDays.size >= 3, s.bronzeClaimed) {
                        runAction("bronze") { api.claimTreasureChest(session, "bronze") }
                    }
                    TreasureChestCard("Rương Bạc", "5 ô", "70–200 coin", s.openedDays.size >= 5, s.silverClaimed) {
                        runAction("silver") { api.claimTreasureChest(session, "silver") }
                    }
                    TreasureChestCard("Rương Vàng", "7 ô", "100–500 coin", s.openedDays.size >= 7, s.goldClaimed) {
                        runAction("gold") { api.claimTreasureChest(session, "gold") }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = Color(0xFFFF5E66), modifier = Modifier.size(34.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.bossName, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                Text("Năng lượng: ${s.bossEnergy}", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        LinearProgressIndicator(
                            progress = { (s.bossHp.toFloat() / max(1, s.bossMaxHp)).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                            color = Color(0xFFFF5E66)
                        )
                        Text("${s.bossHp}/${s.bossMaxHp} HP")
                        Text("Boss bị hạ: tất cả người tham gia nhận ngẫu nhiên 50–300 coin.", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { runAction("boss") { api.attackTreasureBoss(session) } },
                            enabled = action == null && s.bossEnergy > 0 && s.bossHp > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (s.bossHp <= 0) "Boss đã bị hạ" else "Tấn công Boss") }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Thẻ cứu ngày", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Text("Mua tại Cửa hàng với giá 100 coin, dùng tối đa 1 lần/tuần.")
                        Button(
                            onClick = { runAction("rescue") { api.useTreasureRescueCard(session) } },
                            enabled = action == null && !s.rescueUsed,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (s.rescueUsed) "Tuần này đã dùng" else "Dùng Thẻ cứu ngày") }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bản đồ theo mùa: ${s.seasonName}", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Text(if (s.seasonRewardItem.isBlank()) "Hoàn thành 7 ô để nhận vật phẩm giới hạn." else "Vật phẩm: ${s.seasonRewardItem}")
                        Text("Chuỗi 1/2/3 tuần nhận thêm 50/100/200 coin; tuần 4 nhận huy hiệu giới hạn.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Chia sẻ kho báu", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        if (s.shareCode.isNotBlank()) {
                            Text("Mã của bạn: ${s.shareCode}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("Bạn bè nhận 10–30 coin, giới hạn 5 lượt dùng.")
                        } else Text("Mở Rương Vàng để tạo mã chia sẻ.")
                        OutlinedTextField(shareInput, { shareInput = it.uppercase() }, modifier = Modifier.fillMaxWidth(), label = { Text("Nhập mã của bạn bè") }, singleLine = true)
                        Button(
                            onClick = { runAction("share") { api.redeemTreasureShareCode(session, shareInput) } },
                            enabled = action == null && shareInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Nhận quà từ mã") }
                    }
                }
            }
            item {
                Text("Top thợ săn tuần", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                Text("Top 1: 1.000 • Top 2: 700 • Top 3: 500 • Top 4–10: 200 coin", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(s.leaders, key = { "${it.rank}-${it.displayName}" }) { leader ->
                ElevatedCard(shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("#${leader.rank}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Text(leader.displayName, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("${leader.openedCount} ô")
                    }
                }
            }
        }
    }
}

@Composable
private fun TreasureChestCard(
    title: String,
    requirement: String,
    reward: String,
    unlocked: Boolean,
    claimed: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(modifier = Modifier.width(174.dp), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Inventory2, null, tint = if (unlocked) Color(0xFFFFB800) else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(42.dp))
            Text(title, fontWeight = FontWeight.Black)
            Text("Mở $requirement • $reward", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            Button(onClick = onClick, enabled = unlocked && !claimed) { Text(if (claimed) "Đã nhận" else if (unlocked) "Mở rương" else "Đang khóa") }
        }
    }
}

// -------------------------------------------------------------------------
// THÚ CƯNG
// -------------------------------------------------------------------------
@Composable
private fun PetScreen(
    api: SupabaseApi,
    session: Session,
    onBack: () -> Unit,
    onCoinChanged: (Long) -> Unit,
    onMessage: (String) -> Unit
) {
    var pet by remember { mutableStateOf<PetState?>(null) }
    var loading by remember { mutableStateOf(true) }
    var feeding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun reload() {
        loading = true
        scope.launch {
            api.petState(session).onSuccess { pet = it }.onFailure { onMessage(it.message ?: "Không tải được thú cưng") }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GameHeader("Nuôi thú cưng M4X", "Chơi game và mở kho báu để kiếm thức ăn", onBack) }
        if (loading && pet == null) item { Box(Modifier.fillMaxWidth().height(280.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        pet?.let { p ->
            item {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
                        .background(Brush.radialGradient(listOf(Color(0xFF6F4AFF), Color(0xFF09223B))))
                        .padding(24.dp)
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = Color.White.copy(alpha = .14f), shape = CircleShape, modifier = Modifier.size(150.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(86.dp))
                                Surface(color = Color(0xFFFFC443), shape = CircleShape, modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                                    Text("LV.${p.level}", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Color.Black, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Text(p.name, color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                        Text("Linh vật đồng hành cùng cộng đồng M4X", color = Color.White.copy(alpha = .72f))
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PetProgress("Kinh nghiệm", p.xp, p.xpTarget, Color(0xFF8F62FF))
                        PetProgress("No bụng", p.hunger, 100, Color(0xFF17CFE6))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Restaurant, null, tint = Color(0xFFFFB800))
                            Spacer(Modifier.width(8.dp))
                            Text("Thức ăn: ${p.food}", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("Mỗi lần ăn +25 XP")
                        }
                        Button(
                            onClick = {
                                feeding = true
                                scope.launch {
                                    api.feedPet(session)
                                        .onSuccess {
                                            pet = it
                                            onCoinChanged(it.balance)
                                            if (it.levelReward > 0) onMessage("Lên cấp! +${it.levelReward} M4X COIN")
                                        }
                                        .onFailure { onMessage(it.message ?: "Không thể cho thú cưng ăn") }
                                    feeding = false
                                }
                            },
                            enabled = !feeding && p.food > 0,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text(if (feeding) "Đang cho ăn…" else "Cho ${p.name} ăn", fontWeight = FontWeight.Black) }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Phần thưởng lên cấp", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                        Text("Mỗi cấp nhận từ 50 đến tối đa 300 M4X COIN.")
                        Text("Cách kiếm thức ăn:", fontWeight = FontWeight.Bold)
                        Text("• Chơi minigame\n• Hoàn thành mê cung\n• Né chướng ngại\n• Mở ô Bản đồ kho báu")
                    }
                }
            }
        }
    }
}

@Composable
private fun PetProgress(label: String, value: Int, maxValue: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row { Text(label, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("$value/$maxValue") }
        LinearProgressIndicator(
            progress = { (value.toFloat() / max(1, maxValue)).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
            color = color
        )
    }
}

@Composable
private fun GameHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Quay lại") }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
