package com.m4xtheme.app

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun AdminOnlineControlCenter(
    api: SupabaseApi,
    session: Session,
    config: OnlineControlConfig,
    themes: List<ThemeItem>,
    onSaved: (OnlineControlConfig) -> Unit,
    onMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var bannerEnabled by remember(config) {
        mutableStateOf(config.bannerEnabled)
    }
    var bannerTitle by remember(config) {
        mutableStateOf(config.bannerTitle)
    }
    var bannerSubtitle by remember(config) {
        mutableStateOf(config.bannerSubtitle)
    }

    var noticeEnabled by remember(config) {
        mutableStateOf(config.noticeEnabled)
    }
    var noticeTitle by remember(config) {
        mutableStateOf(config.noticeTitle)
    }
    var noticeMessage by remember(config) {
        mutableStateOf(config.noticeMessage)
    }

    var dailyQuestEnabled by remember(config) {
        mutableStateOf(config.dailyQuestEnabled)
    }
    var dailyQuestTitle by remember(config) {
        mutableStateOf(config.dailyQuestTitle)
    }
    var dailyQuestDescription by remember(config) {
        mutableStateOf(config.dailyQuestDescription)
    }
    var dailyQuestReward by remember(config) {
        mutableStateOf(config.dailyQuestReward.toString())
    }

    var checkinEnabled by remember(config) {
        mutableStateOf(config.checkinEnabled)
    }
    var checkinRewards by remember(config) {
        mutableStateOf(
            config.checkinRewards.joinToString(",")
        )
    }

    var spinEnabled by remember(config) {
        mutableStateOf(config.spinEnabled)
    }
    var spinCost by remember(config) {
        mutableStateOf(config.spinCost.toString())
    }
    var spinRewards by remember(config) {
        mutableStateOf(
            config.spinRewards.joinToString(",") {
                "${it.reward}:${it.weight}"
            }
        )
    }

    var fishingEnabled by remember(config) {
        mutableStateOf(config.fishingEnabled)
    }
    var fishingClosedMessage by remember(config) {
        mutableStateOf(config.fishingClosedMessage)
    }
    var fishingRewardMultiplier by remember(config) {
        mutableStateOf(
            config.fishingRewardMultiplier.toString()
        )
    }
    var fishingBossHpMultiplier by remember(config) {
        mutableStateOf(
            config.fishingBossHpMultiplier.toString()
        )
    }

    var featuredThemeId by remember(config) {
        mutableStateOf(config.featuredThemeId)
    }
    var saving by remember {
        mutableStateOf(false)
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CloudSync,
                        null,
                        tint =
                            MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Trung tâm cấu hình online",
                            style =
                                MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "Lưu xong, ứng dụng tự tải cấu hình mới trong khoảng 60 giây.",
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OnlineConfigCard(
            title = "Banner trang chủ",
            icon = {
                Icon(Icons.Default.Campaign, null)
            },
            enabled = bannerEnabled,
            onEnabledChange = { bannerEnabled = it }
        ) {
            OutlinedTextField(
                value = bannerTitle,
                onValueChange = { bannerTitle = it },
                label = { Text("Tiêu đề") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bannerSubtitle,
                onValueChange = { bannerSubtitle = it },
                label = { Text("Nội dung phụ") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OnlineConfigCard(
            title = "Thông báo trong app",
            icon = {
                Icon(Icons.Default.Notifications, null)
            },
            enabled = noticeEnabled,
            onEnabledChange = { noticeEnabled = it }
        ) {
            OutlinedTextField(
                value = noticeTitle,
                onValueChange = { noticeTitle = it },
                label = { Text("Tiêu đề thông báo") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = noticeMessage,
                onValueChange = { noticeMessage = it },
                label = { Text("Nội dung") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OnlineConfigCard(
            title = "Nhiệm vụ hằng ngày",
            icon = {
                Icon(Icons.Default.TaskAlt, null)
            },
            enabled = dailyQuestEnabled,
            onEnabledChange = {
                dailyQuestEnabled = it
            }
        ) {
            OutlinedTextField(
                value = dailyQuestTitle,
                onValueChange = {
                    dailyQuestTitle = it
                },
                label = { Text("Tên nhiệm vụ") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dailyQuestDescription,
                onValueChange = {
                    dailyQuestDescription = it
                },
                label = { Text("Mô tả") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = dailyQuestReward,
                onValueChange = {
                    dailyQuestReward =
                        it.filter(Char::isDigit)
                },
                label = { Text("Coin thưởng") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OnlineConfigCard(
            title = "Quà điểm danh",
            icon = {
                Icon(Icons.Default.CalendarMonth, null)
            },
            enabled = checkinEnabled,
            onEnabledChange = { checkinEnabled = it }
        ) {
            OutlinedTextField(
                value = checkinRewards,
                onValueChange = {
                    checkinRewards =
                        it.filter { char ->
                            char.isDigit() ||
                                char == ',' ||
                                char == ' '
                        }
                },
                label = {
                    Text("Quà từng ngày, ngăn bằng dấu phẩy")
                },
                supportingText = {
                    Text("Ví dụ: 50,75,100,125,150,200,300")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        OnlineConfigCard(
            title = "Vòng quay online",
            icon = {
                Icon(Icons.Default.Casino, null)
            },
            enabled = spinEnabled,
            onEnabledChange = { spinEnabled = it }
        ) {
            OutlinedTextField(
                value = spinCost,
                onValueChange = {
                    spinCost = it.filter(Char::isDigit)
                },
                label = { Text("Phí mỗi lượt quay") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = spinRewards,
                onValueChange = { spinRewards = it },
                label = {
                    Text("Phần thưởng:trọng số")
                },
                supportingText = {
                    Text(
                        "Ví dụ: 0:20,10:45,50:20,100:10,250:5"
                    )
                },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                Modifier.padding(18.dp),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Redeem, null)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Giftcode",
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Tạo mã, số lượt và thời hạn tại tab Giftcode.",
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        OnlineConfigCard(
            title = "Cấu hình câu cá",
            icon = {
                Icon(Icons.Default.SportsEsports, null)
            },
            enabled = fishingEnabled,
            onEnabledChange = { fishingEnabled = it }
        ) {
            OutlinedTextField(
                value = fishingClosedMessage,
                onValueChange = {
                    fishingClosedMessage = it
                },
                label = {
                    Text("Thông báo khi tạm đóng")
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fishingRewardMultiplier,
                onValueChange = {
                    fishingRewardMultiplier =
                        it.filter { char ->
                            char.isDigit() || char == '.'
                        }
                },
                label = {
                    Text("Hệ số giá cá, từ 0.1 đến 10")
                },
                supportingText = {
                    Text("1 = bình thường, 2 = nhân đôi")
                },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fishingBossHpMultiplier,
                onValueChange = {
                    fishingBossHpMultiplier =
                        it.filter { char ->
                            char.isDigit() || char == '.'
                        }
                },
                label = {
                    Text("Hệ số máu Boss, từ 0.1 đến 10")
                },
                supportingText = {
                    Text("1 = bình thường, 1.5 = tăng 50%")
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        ElevatedCard(
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(
                Modifier.padding(18.dp),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Theme nổi bật",
                        fontWeight = FontWeight.Black,
                        style =
                            MaterialTheme.typography.titleMedium
                    )
                }
                Row(
                    Modifier.horizontalScroll(
                        rememberScrollState()
                    ),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {
                            featuredThemeId = ""
                        },
                        label = {
                            Text(
                                if (
                                    featuredThemeId.isBlank()
                                ) {
                                    "✓ Không ghim"
                                } else {
                                    "Không ghim"
                                }
                            )
                        }
                    )
                    themes
                        .filter {
                            it.status == "approved"
                        }
                        .take(30)
                        .forEach { theme ->
                            AssistChip(
                                onClick = {
                                    featuredThemeId =
                                        theme.id
                                },
                                label = {
                                    Text(
                                        if (
                                            featuredThemeId ==
                                            theme.id
                                        ) {
                                            "✓ ${theme.title}"
                                        } else {
                                            theme.title
                                        }
                                    )
                                }
                            )
                        }
                }
                if (
                    featuredThemeId.isNotBlank() &&
                    themes.none {
                        it.id == featuredThemeId
                    }
                ) {
                    Text(
                        "ID đang ghim: $featuredThemeId",
                        style =
                            MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(
            enabled = !saving,
            onClick = {
                val parsedCheckin =
                    checkinRewards.split(",")
                        .mapNotNull {
                            it.trim().toIntOrNull()
                        }
                        .filter { it >= 0 }

                val parsedSpin =
                    spinRewards.split(",")
                        .mapNotNull { part ->
                            val pieces =
                                part.trim().split(":")
                            val reward =
                                pieces.getOrNull(0)
                                    ?.trim()
                                    ?.toIntOrNull()
                            val weight =
                                pieces.getOrNull(1)
                                    ?.trim()
                                    ?.toIntOrNull()
                            if (
                                reward != null &&
                                reward >= 0 &&
                                weight != null &&
                                weight > 0
                            ) {
                                OnlineSpinReward(
                                    reward,
                                    weight
                                )
                            } else {
                                null
                            }
                        }

                if (parsedCheckin.isEmpty()) {
                    onMessage(
                        "Danh sách quà điểm danh chưa hợp lệ"
                    )
                    return@Button
                }
                if (parsedSpin.isEmpty()) {
                    onMessage(
                        "Danh sách vòng quay chưa hợp lệ"
                    )
                    return@Button
                }

                val updated = OnlineControlConfig(
                    bannerEnabled = bannerEnabled,
                    bannerTitle = bannerTitle,
                    bannerSubtitle = bannerSubtitle,
                    noticeEnabled = noticeEnabled,
                    noticeTitle = noticeTitle,
                    noticeMessage = noticeMessage,
                    dailyQuestEnabled =
                        dailyQuestEnabled,
                    dailyQuestTitle = dailyQuestTitle,
                    dailyQuestDescription =
                        dailyQuestDescription,
                    dailyQuestReward =
                        dailyQuestReward.toIntOrNull()
                            ?: 0,
                    checkinEnabled = checkinEnabled,
                    checkinRewards = parsedCheckin,
                    spinEnabled = spinEnabled,
                    spinCost =
                        spinCost.toIntOrNull() ?: 0,
                    spinRewards = parsedSpin,
                    fishingEnabled = fishingEnabled,
                    fishingClosedMessage =
                        fishingClosedMessage,
                    fishingRewardMultiplier =
                        fishingRewardMultiplier
                            .toDoubleOrNull() ?: 1.0,
                    fishingBossHpMultiplier =
                        fishingBossHpMultiplier
                            .toDoubleOrNull() ?: 1.0,
                    featuredThemeId =
                        featuredThemeId
                )

                saving = true
                scope.launch {
                    api.updateOnlineControlConfig(
                        session,
                        updated
                    ).onSuccess {
                        onSaved(it)
                        onMessage(
                            "Đã cập nhật cấu hình online"
                        )
                    }.onFailure {
                        onMessage(
                            it.message
                                ?: "Không thể lưu cấu hình"
                        )
                    }
                    saving = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (saving) {
                    "Đang lưu…"
                } else {
                    "Lưu toàn bộ online"
                },
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun OnlineConfigCard(
    title: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                icon()
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Black,
                    style =
                        MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }
            content()
        }
    }
}
