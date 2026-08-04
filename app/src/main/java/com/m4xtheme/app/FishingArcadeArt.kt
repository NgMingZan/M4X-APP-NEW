package com.m4xtheme.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal data class FishingArcadePalette(
    val skyTop: Color,
    val skyBottom: Color,
    val waterTop: Color,
    val waterBottom: Color,
    val hill: Color,
    val accent: Color,
    val decor: Color
)

internal data class FishingArcadeRodVisual(
    val body: Color,
    val bodyLight: Color,
    val line: Color,
    val bobber: Color,
    val widthScale: Float,
    val glow: Color
)

internal fun fishingArcadePalette(theme: String): FishingArcadePalette =
    when (theme) {
        "swamp" -> FishingArcadePalette(
            skyTop = Color(0xFFD4FFE2),
            skyBottom = Color(0xFF7EE6B2),
            waterTop = Color(0xFF56D7A2),
            waterBottom = Color(0xFF178667),
            hill = Color(0xFF78C98C),
            accent = Color(0xFFFFD650),
            decor = Color(0xFF2E8C58)
        )

        "coral" -> FishingArcadePalette(
            skyTop = Color(0xFFB5F5FF),
            skyBottom = Color(0xFF65DCFF),
            waterTop = Color(0xFF3EDCFF),
            waterBottom = Color(0xFF0E76D2),
            hill = Color(0xFF7FD7AC),
            accent = Color(0xFFFFD65A),
            decor = Color(0xFFFF7C87)
        )

        "ice" -> FishingArcadePalette(
            skyTop = Color(0xFFF8FEFF),
            skyBottom = Color(0xFFB8F1FF),
            waterTop = Color(0xFF8CEEFF),
            waterBottom = Color(0xFF2B8ED0),
            hill = Color(0xFFDDF8FF),
            accent = Color(0xFFBFF8FF),
            decor = Color(0xFFEAFDFF)
        )

        "legend" -> FishingArcadePalette(
            skyTop = Color(0xFFFFE99D),
            skyBottom = Color(0xFFFFA278),
            waterTop = Color(0xFF6FB8FF),
            waterBottom = Color(0xFF345CC8),
            hill = Color(0xFF8067D9),
            accent = Color(0xFFFFD64F),
            decor = Color(0xFFFFC45D)
        )

        else -> FishingArcadePalette(
            skyTop = Color(0xFFB6F2FF),
            skyBottom = Color(0xFF73DBFF),
            waterTop = Color(0xFF54D5FF),
            waterBottom = Color(0xFF1174CE),
            hill = Color(0xFF8FDBA7),
            accent = Color(0xFFFFD34F),
            decor = Color(0xFF68CE78)
        )
    }

internal fun fishingArcadeRod(code: String): FishingArcadeRodVisual =
    when (code) {
        "fiber" -> FishingArcadeRodVisual(
            body = Color(0xFF7E8E9C),
            bodyLight = Color(0xFFC3D5E0),
            line = Color(0xFFF7FDFF),
            bobber = Color(0xFFFF9848),
            widthScale = 1.10f,
            glow = Color(0xFF7DE8FF)
        )

        "carbon" -> FishingArcadeRodVisual(
            body = Color(0xFF3F4750),
            bodyLight = Color(0xFF77838C),
            line = Color.White,
            bobber = Color(0xFF43D4F2),
            widthScale = 1.22f,
            glow = Color(0xFF55DFFF)
        )

        "titan" -> FishingArcadeRodVisual(
            body = Color(0xFFAEBCC7),
            bodyLight = Color(0xFFF0F7FA),
            line = Color(0xFFFFFEF4),
            bobber = Color(0xFFAA82FF),
            widthScale = 1.34f,
            glow = Color(0xFFB998FF)
        )

        "celestial" -> FishingArcadeRodVisual(
            body = Color(0xFFD4B54B),
            bodyLight = Color(0xFFFFEA86),
            line = Color(0xFFFFF8D2),
            bobber = Color(0xFFFFD34F),
            widthScale = 1.46f,
            glow = Color(0xFFFFD85F)
        )

        else -> FishingArcadeRodVisual(
            body = Color(0xFF8A5A30),
            bodyLight = Color(0xFFC78A4C),
            line = Color.White,
            bobber = Color(0xFFFF6979),
            widthScale = 1f,
            glow = Color.Transparent
        )
    }

@Composable
internal fun FishingArcadeHomeHero(
    rod: FishingRodInfo?,
    inventoryCount: Int,
    inventoryValue: Int,
    modifier: Modifier = Modifier
) {
    val visual = fishingArcadeRod(rod?.code.orEmpty())
    val time = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            time.snapTo(0f)
            time.animateTo(1f, tween(4200))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(RoundedCornerShape(30.dp))
            .border(
                1.dp,
                Color(0xFFBCEBFF),
                RoundedCornerShape(30.dp)
            )
            .background(Color(0xFF8FE4FF))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = time.value
            val palette = fishingArcadePalette("lotus")
            drawArcadeEnvironment(
                palette = palette,
                theme = "lotus",
                time = t,
                withDecor = true
            )
            drawArcadeBoatAndChibi(
                origin = Offset(size.width * 0.29f, size.height * 0.66f),
                scale = size.minDimension / 280f,
                rod = visual,
                pull = 0.10f + sin(t * PI.toFloat() * 2f) * 0.04f,
                critical = false,
                blink = blinkAmount(t)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.86f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                rod?.name ?: "Cần tre M4X",
                color = Color(0xFF173E63),
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Kho: $inventoryCount con • $inventoryValue M4X",
                color = Color(0xFF6685A3),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun FishingArcadeMapPreview(
    map: FishingMapInfo,
    modifier: Modifier = Modifier
) {
    val time = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            time.snapTo(0f)
            time.animateTo(1f, tween(5200))
        }
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(124.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                Color(0xFFC9ECFF),
                RoundedCornerShape(20.dp)
            )
    ) {
        drawArcadeEnvironment(
            palette = fishingArcadePalette(map.theme),
            theme = map.theme,
            time = time.value,
            withDecor = true
        )
    }
}

@Composable
internal fun FishingArcadeRodPreview(
    rod: FishingRodInfo,
    modifier: Modifier = Modifier
) {
    val visual = fishingArcadeRod(rod.code)
    Canvas(
        modifier
            .height(62.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFE2F6FF))
    ) {
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White, Color(0xFFDDF4FF))
            ),
            cornerRadius = CornerRadius(18.dp.toPx())
        )
        val start = Offset(size.width * 0.18f, size.height * 0.75f)
        val end = Offset(size.width * 0.82f, size.height * 0.20f)
        drawLine(
            color = visual.glow.copy(alpha = 0.35f),
            start = start,
            end = end,
            strokeWidth = 12.dp.toPx() * visual.widthScale,
            cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.linearGradient(
                listOf(visual.body, visual.bodyLight),
                start = start,
                end = end
            ),
            start = start,
            end = end,
            strokeWidth = 5.dp.toPx() * visual.widthScale,
            cap = StrokeCap.Round
        )
        val reel = Offset(size.width * 0.34f, size.height * 0.64f)
        drawCircle(Color(0xFF2F77AD), 10.dp.toPx(), reel)
        drawCircle(Color.White, 5.dp.toPx(), reel)
        drawLine(
            visual.line,
            end,
            Offset(size.width * 0.88f, size.height * 0.70f),
            1.5.dp.toPx()
        )
        drawBobber(
            center = Offset(size.width * 0.88f, size.height * 0.70f),
            color = visual.bobber,
            scale = 0.72f
        )
    }
}

@Composable
internal fun FishingArcadeBattleScene(
    theme: String,
    cast: FishingCastStart?,
    bossHpFraction: Float,
    bossHitFlash: Float,
    criticalFlash: Boolean,
    frozen: Boolean,
    rodCode: String,
    pullPulse: Int,
    tension: Float,
    modifier: Modifier = Modifier
) {
    val world = remember { Animatable(0f) }
    val pull = remember { Animatable(0f) }
    var previousPulse by remember { mutableStateOf(pullPulse) }

    LaunchedEffect(Unit) {
        while (true) {
            world.snapTo(0f)
            world.animateTo(1f, tween(5200))
        }
    }

    LaunchedEffect(pullPulse) {
        if (pullPulse != previousPulse) {
            previousPulse = pullPulse
            pull.snapTo(0f)
            pull.animateTo(1f, tween(180))
            pull.animateTo(0f, tween(280))
        }
    }

    Canvas(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .border(
                1.dp,
                Color(0xFFBCEBFF),
                RoundedCornerShape(26.dp)
            )
    ) {
        val t = world.value
        val palette = fishingArcadePalette(theme)
        drawArcadeEnvironment(
            palette = palette,
            theme = theme,
            time = t,
            withDecor = true
        )

        val visual = fishingArcadeRod(rodCode)
        drawArcadeBoatAndChibi(
            origin = Offset(size.width * 0.27f, size.height * 0.63f),
            scale = size.minDimension / 340f,
            rod = visual,
            pull = pull.value,
            critical = criticalFlash,
            blink = blinkAmount(t)
        )

        val fishCenter = Offset(
            x = size.width * 0.69f +
                sin(t * PI.toFloat() * 2.4f) * size.width * 0.085f,
            y = size.height * 0.70f +
                cos(t * PI.toFloat() * 2f) * size.height * 0.035f
        )

        drawFishAura(
            center = fishCenter,
            rarity = cast?.rarity.orEmpty(),
            frozen = frozen
        )
        drawArcadeFish(
            center = fishCenter,
            name = cast?.fishName.orEmpty(),
            rarity = cast?.rarity.orEmpty(),
            hpFraction = bossHpFraction,
            hit = bossHitFlash > 0f,
            frozen = frozen,
            time = t
        )

        val hand = chibiHandPosition(
            origin = Offset(size.width * 0.27f, size.height * 0.63f),
            scale = size.minDimension / 340f,
            pull = pull.value
        )
        val rodTip = Offset(
            hand.x + 96f * size.minDimension / 340f,
            hand.y - (82f + pull.value * 18f) * size.minDimension / 340f
        )
        val bobber = Offset(
            fishCenter.x - size.width * 0.02f,
            fishCenter.y - size.height * 0.16f +
                sin(t * PI.toFloat() * 6f) * 3f
        )

        drawLine(
            color = visual.line.copy(alpha = 0.92f),
            start = rodTip,
            end = bobber,
            strokeWidth = 1.5.dp.toPx() +
                (tension / 120f).coerceIn(0f, 1f) * 1.2.dp.toPx()
        )
        drawBobber(bobber, visual.bobber, 0.88f)
        drawRipple(
            center = Offset(bobber.x, bobber.y + 6.dp.toPx()),
            time = t,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

private fun DrawScope.drawArcadeEnvironment(
    palette: FishingArcadePalette,
    theme: String,
    time: Float,
    withDecor: Boolean
) {
    val horizon = size.height * 0.43f
    drawRect(
        brush = Brush.verticalGradient(
            listOf(palette.skyTop, palette.skyBottom),
            endY = horizon
        ),
        size = Size(size.width, horizon)
    )

    val sun = Offset(size.width * 0.84f, size.height * 0.16f)
    drawCircle(
        palette.accent.copy(alpha = 0.28f),
        size.minDimension * 0.11f,
        sun
    )
    drawCircle(
        Color(0xFFFFF4A8),
        size.minDimension * 0.075f,
        sun
    )

    repeat(3) { index ->
        val cloudX = (
            size.width * (0.06f + index * 0.32f) +
                sin(time * PI.toFloat() * 2f + index) *
                size.width * 0.035f
            ) % (size.width + 60f)
        val cloudY = size.height * (0.09f + index * 0.06f)
        drawCloud(Offset(cloudX, cloudY), 0.72f + index * 0.12f)
    }

    val hill = Path().apply {
        moveTo(0f, horizon)
        lineTo(size.width * 0.11f, horizon - size.height * 0.13f)
        lineTo(size.width * 0.24f, horizon - size.height * 0.04f)
        lineTo(size.width * 0.39f, horizon - size.height * 0.17f)
        lineTo(size.width * 0.54f, horizon - size.height * 0.03f)
        lineTo(size.width * 0.70f, horizon - size.height * 0.14f)
        lineTo(size.width * 0.86f, horizon - size.height * 0.04f)
        lineTo(size.width, horizon - size.height * 0.10f)
        lineTo(size.width, horizon)
        close()
    }
    drawPath(hill, palette.hill)

    drawRect(
        brush = Brush.verticalGradient(
            listOf(palette.waterTop, palette.waterBottom),
            startY = horizon,
            endY = size.height
        ),
        topLeft = Offset(0f, horizon),
        size = Size(size.width, size.height - horizon)
    )

    repeat(8) { index ->
        val y = horizon + 12.dp.toPx() + index *
            (size.height - horizon) / 8f +
            sin(time * PI.toFloat() * 4f + index) * 3.dp.toPx()
        drawLine(
            Color.White.copy(alpha = 0.23f - index * 0.014f),
            Offset(-20f, y),
            Offset(size.width + 20f, y + 4.dp.toPx()),
            2.dp.toPx()
        )
    }

    if (withDecor) {
        drawThemeDecor(theme, palette, horizon, time)
        drawJumpingFish(time, horizon)
        drawFlyingLeaves(theme, time)
    }
}

private fun DrawScope.drawCloud(center: Offset, scale: Float) {
    val white = Color.White.copy(alpha = 0.83f)
    drawOval(
        white,
        topLeft = Offset(center.x, center.y),
        size = Size(62f * scale, 20f * scale)
    )
    drawCircle(white, 15f * scale, Offset(center.x + 17f * scale, center.y))
    drawCircle(white, 18f * scale, Offset(center.x + 38f * scale, center.y - 3f * scale))
}

private fun DrawScope.drawThemeDecor(
    theme: String,
    palette: FishingArcadePalette,
    horizon: Float,
    time: Float
) {
    when (theme) {
        "swamp" -> repeat(2) { side ->
            val x = if (side == 0) size.width * 0.08f else size.width * 0.88f
            repeat(5) { blade ->
                drawLine(
                    palette.decor,
                    Offset(x + blade * 5f, size.height * 0.91f),
                    Offset(
                        x + blade * 4f + sin(time * 6f + blade) * 4f,
                        size.height * (0.73f - blade * 0.012f)
                    ),
                    3f,
                    StrokeCap.Round
                )
            }
        }

        "coral" -> repeat(2) { side ->
            val x = if (side == 0) size.width * 0.12f else size.width * 0.84f
            drawCoral(Offset(x, size.height * 0.90f), palette.decor)
        }

        "ice" -> repeat(2) { side ->
            val x = if (side == 0) size.width * 0.10f else size.width * 0.83f
            val iceberg = Path().apply {
                moveTo(x - 28f, size.height * 0.91f)
                lineTo(x - 16f, size.height * 0.80f)
                lineTo(x - 5f, size.height * 0.85f)
                lineTo(x + 8f, size.height * 0.73f)
                lineTo(x + 23f, size.height * 0.86f)
                lineTo(x + 32f, size.height * 0.79f)
                lineTo(x + 40f, size.height * 0.91f)
                close()
            }
            drawPath(iceberg, palette.decor.copy(alpha = 0.94f))
        }

        "legend" -> repeat(2) { side ->
            val x = if (side == 0) size.width * 0.10f else size.width * 0.84f
            drawRoundRect(
                palette.decor.copy(alpha = 0.88f),
                Offset(x, size.height * 0.73f),
                Size(24f, size.height * 0.18f),
                CornerRadius(4f)
            )
            drawRoundRect(
                palette.decor.copy(alpha = 0.88f),
                Offset(x - 8f, size.height * 0.70f),
                Size(40f, 14f),
                CornerRadius(4f)
            )
        }

        else -> repeat(3) { index ->
            val center = Offset(
                size.width * (0.15f + index * 0.31f),
                horizon + size.height * (0.26f + index * 0.025f)
            )
            drawOval(
                Color(0xFF68CE78).copy(alpha = 0.92f),
                Offset(center.x - 20f, center.y - 8f),
                Size(40f, 17f)
            )
            drawLotus(center + Offset(0f, -5f))
        }
    }
}

private fun DrawScope.drawLotus(center: Offset) {
    val petal = Color(0xFFFF82B4)
    repeat(6) { index ->
        val angle = index * PI.toFloat() / 3f
        drawOval(
            petal,
            topLeft = Offset(
                center.x + cos(angle) * 7f - 5f,
                center.y + sin(angle) * 5f - 7f
            ),
            size = Size(10f, 14f)
        )
    }
    drawCircle(Color(0xFFFFD45E), 4f, center)
}

private fun DrawScope.drawCoral(center: Offset, color: Color) {
    drawLine(color, center, center + Offset(0f, -42f), 7f, StrokeCap.Round)
    drawLine(color, center + Offset(0f, -22f), center + Offset(-16f, -38f), 6f, StrokeCap.Round)
    drawLine(color, center + Offset(0f, -28f), center + Offset(17f, -47f), 6f, StrokeCap.Round)
    drawLine(color, center + Offset(-10f, -32f), center + Offset(-18f, -51f), 5f, StrokeCap.Round)
}

private fun DrawScope.drawJumpingFish(time: Float, horizon: Float) {
    repeat(2) { index ->
        val local = (time * 1.45f + index * 0.46f) % 1f
        if (local < 0.38f) {
            val progress = local / 0.38f
            val x = size.width * (0.62f + index * 0.18f) +
                (progress - 0.5f) * 34f
            val y = horizon + size.height * 0.18f -
                sin(progress * PI.toFloat()) * size.height * 0.18f
            val fish = Path().apply {
                moveTo(x - 13f, y)
                quadraticBezierTo(x, y - 8f, x + 15f, y)
                quadraticBezierTo(x, y + 8f, x - 13f, y)
                close()
                moveTo(x - 12f, y)
                lineTo(x - 22f, y - 7f)
                lineTo(x - 22f, y + 7f)
                close()
            }
            drawPath(fish, Color.White.copy(alpha = 0.86f))
            drawCircle(Color(0xFF204567), 1.8f, Offset(x + 8f, y - 2f))
        }
    }
}

private fun DrawScope.drawFlyingLeaves(theme: String, time: Float) {
    val leafColor = when (theme) {
        "ice" -> Color(0xFFD9F8FF)
        "legend" -> Color(0xFFFFD66A)
        else -> Color(0xFF86D75E)
    }
    repeat(4) { index ->
        val local = (time * 1.1f + index * 0.24f) % 1f
        val x = size.width * (0.10f + index * 0.20f) + local * 40f
        val y = size.height * (0.10f + index * 0.035f) + local * size.height * 0.40f
        drawOval(
            leafColor.copy(alpha = (1f - local).coerceAtLeast(0.15f)),
            topLeft = Offset(x, y),
            size = Size(12f, 7f)
        )
    }
}

private fun DrawScope.drawArcadeBoatAndChibi(
    origin: Offset,
    scale: Float,
    rod: FishingArcadeRodVisual,
    pull: Float,
    critical: Boolean,
    blink: Float
) {
    val s = scale
    val shake = if (critical) sin(pull * PI.toFloat() * 8f) * 5f * s else 0f
    val bodyOrigin = origin + Offset(shake, -pull * 8f * s)

    drawOval(
        Color.Black.copy(alpha = 0.13f),
        topLeft = Offset(bodyOrigin.x - 48f * s, bodyOrigin.y + 30f * s),
        size = Size(96f * s, 18f * s)
    )
    val boat = Path().apply {
        moveTo(bodyOrigin.x - 68f * s, bodyOrigin.y + 5f * s)
        quadraticBezierTo(
            bodyOrigin.x,
            bodyOrigin.y + 36f * s,
            bodyOrigin.x + 72f * s,
            bodyOrigin.y + 5f * s
        )
        lineTo(bodyOrigin.x + 58f * s, bodyOrigin.y + 29f * s)
        quadraticBezierTo(
            bodyOrigin.x,
            bodyOrigin.y + 50f * s,
            bodyOrigin.x - 54f * s,
            bodyOrigin.y + 29f * s
        )
        close()
    }
    drawPath(boat, Color(0xFF7C4821))
    drawRoundRect(
        Color(0xFFC27A40),
        Offset(bodyOrigin.x - 50f * s, bodyOrigin.y + 1f * s),
        Size(100f * s, 12f * s),
        CornerRadius(6f * s)
    )

    val torsoCenter = bodyOrigin + Offset(-10f * s, -17f * s)
    val headCenter = bodyOrigin + Offset(-10f * s, -62f * s)

    drawLine(
        Color(0xFF2D4D96),
        torsoCenter + Offset(-7f * s, 20f * s),
        torsoCenter + Offset(-2f * s, 50f * s),
        10f * s,
        StrokeCap.Round
    )
    drawLine(
        Color(0xFF2D4D96),
        torsoCenter + Offset(7f * s, 20f * s),
        torsoCenter + Offset(28f * s, 38f * s),
        10f * s,
        StrokeCap.Round
    )
    drawLine(
        Color(0xFF8B4D23),
        torsoCenter + Offset(-2f * s, 50f * s),
        torsoCenter + Offset(9f * s, 50f * s),
        7f * s,
        StrokeCap.Round
    )
    drawLine(
        Color(0xFF8B4D23),
        torsoCenter + Offset(28f * s, 38f * s),
        torsoCenter + Offset(39f * s, 40f * s),
        7f * s,
        StrokeCap.Round
    )

    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color(0xFF3B71DB), Color(0xFF224DB0))
        ),
        topLeft = Offset(torsoCenter.x - 19f * s, torsoCenter.y - 16f * s),
        size = Size(38f * s, 42f * s),
        cornerRadius = CornerRadius(15f * s)
    )
    drawRoundRect(
        Color(0xFFFF8F58),
        Offset(torsoCenter.x - 12f * s, torsoCenter.y - 16f * s),
        Size(24f * s, 15f * s),
        CornerRadius(7f * s)
    )

    drawCircle(Color(0xFFFFD9C2), 17f * s, headCenter)
    drawArc(
        Color(0xFFFFCB3C),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(headCenter.x - 20f * s, headCenter.y - 22f * s),
        size = Size(40f * s, 30f * s)
    )
    drawRoundRect(
        Color(0xFFFF8A48),
        Offset(headCenter.x - 19f * s, headCenter.y - 22f * s),
        Size(36f * s, 12f * s),
        CornerRadius(8f * s)
    )
    drawRoundRect(
        Color(0xFFFFD87F),
        Offset(headCenter.x + 13f * s, headCenter.y - 17f * s),
        Size(15f * s, 5f * s),
        CornerRadius(3f * s)
    )

    val eyeHeight = (4f * (1f - blink)).coerceAtLeast(0.7f) * s
    drawOval(
        Color(0xFF20354D),
        Offset(headCenter.x - 9f * s, headCenter.y - 3f * s),
        Size(5f * s, eyeHeight)
    )
    drawOval(
        Color(0xFF20354D),
        Offset(headCenter.x + 4f * s, headCenter.y - 3f * s),
        Size(5f * s, eyeHeight)
    )
    drawOval(
        Color(0xFFFF9EB0).copy(alpha = 0.68f),
        Offset(headCenter.x - 13f * s, headCenter.y + 5f * s),
        Size(7f * s, 4f * s)
    )
    drawOval(
        Color(0xFFFF9EB0).copy(alpha = 0.68f),
        Offset(headCenter.x + 7f * s, headCenter.y + 5f * s),
        Size(7f * s, 4f * s)
    )
    if (critical) {
        drawOval(
            Color(0xFFA75B54),
            Offset(headCenter.x - 5f * s, headCenter.y + 7f * s),
            Size(10f * s, 7f * s)
        )
    } else {
        drawArc(
            Color(0xFFA75B54),
            startAngle = 8f,
            sweepAngle = 164f,
            useCenter = false,
            topLeft = Offset(headCenter.x - 5f * s, headCenter.y + 5f * s),
            size = Size(10f * s, 7f * s),
            style = Stroke(width = 1.5f * s)
        )
    }

    val hand = chibiHandPosition(bodyOrigin, s, pull)
    val shoulder = torsoCenter + Offset(14f * s, -5f * s)
    drawLine(
        Color(0xFFFFD9C2),
        torsoCenter + Offset(-15f * s, -2f * s),
        torsoCenter + Offset(-25f * s, 18f * s),
        9f * s,
        StrokeCap.Round
    )
    drawLine(
        Color(0xFFFFD9C2),
        shoulder,
        hand,
        9f * s,
        StrokeCap.Round
    )
    drawCircle(Color(0xFFFFD9C2), 5.5f * s, hand)

    val reelCenter = torsoCenter + Offset(18f * s, 12f * s)
    drawCircle(Color.White.copy(alpha = 0.9f), 10f * s, reelCenter)
    drawCircle(Color(0xFFFFB43F), 7.5f * s, reelCenter)
    drawCircle(Color(0xFFCB7612), 2.4f * s, reelCenter)
    val reelAngle = pull * PI.toFloat() * 8f
    drawLine(
        Color(0xFF8B4D23),
        reelCenter,
        reelCenter + Offset(cos(reelAngle) * 11f * s, sin(reelAngle) * 11f * s),
        3f * s,
        StrokeCap.Round
    )

    val rodTip = Offset(
        hand.x + 96f * s,
        hand.y - (82f + pull * 18f) * s
    )
    drawLine(
        rod.glow.copy(alpha = 0.25f),
        hand,
        rodTip,
        12f * s * rod.widthScale,
        StrokeCap.Round
    )
    drawLine(
        brush = Brush.linearGradient(
            listOf(rod.body, rod.bodyLight),
            start = hand,
            end = rodTip
        ),
        start = hand,
        end = rodTip,
        strokeWidth = 5f * s * rod.widthScale,
        cap = StrokeCap.Round
    )
}

private fun chibiHandPosition(
    origin: Offset,
    scale: Float,
    pull: Float
): Offset = Offset(
    x = origin.x + (31f + pull * 11f) * scale,
    y = origin.y + (-18f - pull * 18f) * scale
)

private fun DrawScope.drawFishAura(
    center: Offset,
    rarity: String,
    frozen: Boolean
) {
    val color = when {
        frozen -> Color(0xFF8EEBFF)
        rarity == "legendary" -> Color(0xFFFFD75F)
        rarity == "epic" -> Color(0xFFB58AFF)
        rarity == "rare" -> Color(0xFF63DFFF)
        else -> Color.White
    }
    drawCircle(color.copy(alpha = 0.10f), 82f, center)
    drawCircle(color.copy(alpha = 0.15f), 61f, center)
}

private fun DrawScope.drawArcadeFish(
    center: Offset,
    name: String,
    rarity: String,
    hpFraction: Float,
    hit: Boolean,
    frozen: Boolean,
    time: Float
) {
    val palette = fishColors(name, rarity, frozen, hit)
    val scale = 0.78f + (1f - hpFraction) * 0.22f
    val width = size.minDimension * 0.21f * scale
    val height = width * 0.58f
    val sway = sin(time * PI.toFloat() * 5f) * 5f

    val fish = Path().apply {
        moveTo(center.x - width * 0.54f, center.y + sway)
        quadraticBezierTo(
            center.x,
            center.y - height,
            center.x + width * 0.58f,
            center.y + sway
        )
        quadraticBezierTo(
            center.x,
            center.y + height,
            center.x - width * 0.54f,
            center.y + sway
        )
        close()
        moveTo(center.x - width * 0.50f, center.y + sway)
        lineTo(center.x - width * 0.88f, center.y - height * 0.72f + sway)
        lineTo(center.x - width * 0.88f, center.y + height * 0.72f + sway)
        close()
    }
    drawPath(
        fish,
        brush = Brush.linearGradient(
            listOf(palette.first, palette.second),
            start = Offset(center.x - width, center.y - height),
            end = Offset(center.x + width, center.y + height)
        )
    )

    val fin = Path().apply {
        moveTo(center.x - width * 0.05f, center.y - height * 0.52f + sway)
        lineTo(center.x + width * 0.08f, center.y - height * 1.05f + sway)
        lineTo(center.x + width * 0.22f, center.y - height * 0.43f + sway)
        close()
    }
    drawPath(fin, palette.third)
    val lowerFin = Path().apply {
        moveTo(center.x, center.y + height * 0.48f + sway)
        lineTo(center.x + width * 0.22f, center.y + height * 0.94f + sway)
        lineTo(center.x + width * 0.31f, center.y + height * 0.42f + sway)
        close()
    }
    drawPath(lowerFin, palette.third)

    repeat(4) { index ->
        drawOval(
            Color.White.copy(alpha = 0.12f),
            Offset(
                center.x - width * 0.26f + index * width * 0.16f,
                center.y - height * 0.22f + sway
            ),
            Size(width * 0.13f, height * 0.42f)
        )
    }
    drawCircle(
        Color.White,
        width * 0.055f,
        Offset(center.x + width * 0.34f, center.y - height * 0.16f + sway)
    )
    drawCircle(
        Color(0xFF172635),
        width * 0.024f,
        Offset(center.x + width * 0.35f, center.y - height * 0.16f + sway)
    )
    drawArc(
        Color(0xFF24313C).copy(alpha = 0.55f),
        12f,
        150f,
        false,
        Offset(center.x + width * 0.23f, center.y + height * 0.03f + sway),
        Size(width * 0.22f, height * 0.18f),
        style = Stroke(width = 2f)
    )
}

private data class FishColorSet(
    val first: Color,
    val second: Color,
    val third: Color
)

private fun fishColors(
    name: String,
    rarity: String,
    frozen: Boolean,
    hit: Boolean
): FishColorSet {
    if (hit) return FishColorSet(Color.White, Color(0xFFFFE29A), Color.White)
    if (frozen) return FishColorSet(Color(0xFFE8FDFF), Color(0xFF79D9EA), Color(0xFF5BC2D9))
    val lower = name.lowercase()
    return when {
        "mập" in lower -> FishColorSet(Color(0xFF7F9BAA), Color(0xFF314852), Color(0xFF20323A))
        "ngừ" in lower || "thu" in lower -> FishColorSet(Color(0xFF91C4D5), Color(0xFF3B6275), Color(0xFF274550))
        "rồng" in lower || "leviathan" in lower -> FishColorSet(Color(0xFFFFDD69), Color(0xFFE58C18), Color(0xFF8147DE))
        "băng" in lower || "pha lê" in lower || "tuyết" in lower -> FishColorSet(Color(0xFFF4FEFF), Color(0xFF8ADCEB), Color(0xFF66C6DB))
        "lươn" in lower -> FishColorSet(Color(0xFFA0D76B), Color(0xFF4D8724), Color(0xFF2D6013))
        rarity == "legendary" -> FishColorSet(Color(0xFFFFD75C), Color(0xFFE98B12), Color(0xFF8249DD))
        rarity == "epic" -> FishColorSet(Color(0xFFB881FF), Color(0xFF5932AE), Color(0xFF3B227A))
        rarity == "rare" -> FishColorSet(Color(0xFF68C1FF), Color(0xFF2858AA), Color(0xFF1D3D77))
        rarity == "uncommon" -> FishColorSet(Color(0xFF81C9B5), Color(0xFF317169), Color(0xFF24564F))
        else -> FishColorSet(Color(0xFFE0A46A), Color(0xFF955E32), Color(0xFF75401F))
    }
}

private fun DrawScope.drawBobber(
    center: Offset,
    color: Color,
    scale: Float
) {
    drawCircle(Color.Black.copy(alpha = 0.12f), 11f * scale, center + Offset(0f, 6f * scale))
    drawCircle(Color.White, 9f * scale, center)
    drawArc(
        color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(center.x - 9f * scale, center.y - 9f * scale),
        size = Size(18f * scale, 18f * scale)
    )
}

private fun DrawScope.drawRipple(
    center: Offset,
    time: Float,
    color: Color
) {
    repeat(3) { index ->
        val phase = (time * 1.6f + index * 0.33f) % 1f
        drawOval(
            color.copy(alpha = (1f - phase) * 0.6f),
            topLeft = Offset(
                center.x - (10f + phase * 28f),
                center.y - (4f + phase * 8f)
            ),
            size = Size(
                (20f + phase * 56f),
                (8f + phase * 16f)
            ),
            style = Stroke(width = 1.5f)
        )
    }
}

private fun blinkAmount(time: Float): Float {
    val local = (time * 3.7f) % 1f
    return if (local in 0.44f..0.50f) {
        1f - ((local - 0.47f) / 0.03f).let { kotlin.math.abs(it) }.coerceIn(0f, 1f)
    } else {
        0f
    }
}
