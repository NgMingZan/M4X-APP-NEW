package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ThemeEntity
import com.example.data.local.UserEntity
import com.example.data.local.UserRole
import com.example.ui.viewmodel.ScreenRoute

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M4xTopBar(
    currentUser: UserEntity?,
    unreadNotifCount: Int,
    currentScreen: ScreenRoute,
    onNavigate: (ScreenRoute) -> Unit,
    onSwitchRole: (Long) -> Unit,
    onCheckOta: () -> Unit
) {
    var showRoleDropdown by remember { mutableStateOf(false) }

    Surface(
        color = Color(0xFF0F172A),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Logo & Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onNavigate(ScreenRoute.HOME) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF06B6D4), Color(0xFF8B5CF6))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M4X",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "M4X Theme",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFF06B6D4).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HyperOS",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Cộng đồng Theme HyperOS & MIUI",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Actions: Role Switcher, OTA, Notifications
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // OTA Update button
                    IconButton(
                        onClick = onCheckOta,
                        modifier = Modifier.testTag("ota_update_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SystemUpdate,
                            contentDescription = "Cập nhật OTA",
                            tint = Color(0xFF38BDF8)
                        )
                    }

                    // Notification Bell
                    Box {
                        IconButton(
                            onClick = { onNavigate(ScreenRoute.NOTIFICATIONS) },
                            modifier = Modifier.testTag("notification_button")
                        ) {
                            Icon(
                                imageVector = if (unreadNotifCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Thông báo",
                                tint = if (unreadNotifCount > 0) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                            )
                        }
                        if (unreadNotifCount > 0) {
                            Badge(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White,
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(text = if (unreadNotifCount > 9) "9+" else unreadNotifCount.toString(), fontSize = 10.sp)
                            }
                        }
                    }

                    // Role Switcher Button
                    Box {
                        Surface(
                            onClick = { showRoleDropdown = true },
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.testTag("role_switcher_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (currentUser?.role) {
                                        UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                        UserRole.CREATOR -> Icons.Filled.Palette
                                        else -> Icons.Filled.Person
                                    },
                                    contentDescription = "Role",
                                    tint = when (currentUser?.role) {
                                        UserRole.ADMIN -> Color(0xFFEF4444)
                                        UserRole.CREATOR -> Color(0xFFA855F7)
                                        else -> Color(0xFF10B981)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (currentUser?.role) {
                                        UserRole.ADMIN -> "Admin"
                                        UserRole.CREATOR -> "Creator"
                                        else -> "User"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showRoleDropdown,
                            onDismissRequest = { showRoleDropdown = false },
                            modifier = Modifier.background(Color(0xFF1E293B))
                        ) {
                            DropdownMenuItem(
                                text = { Text("👤 User Normal (MinhTu)", color = Color.White, fontSize = 13.sp) },
                                onClick = {
                                    onSwitchRole(4)
                                    showRoleDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🎨 Creator Mode (HyperDesign)", color = Color(0xFFA855F7), fontSize = 13.sp) },
                                onClick = {
                                    onSwitchRole(2)
                                    showRoleDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("👑 Admin Panel (M4X Admin)", color = Color(0xFFEF4444), fontSize = 13.sp) },
                                onClick = {
                                    onSwitchRole(1)
                                    showRoleDropdown = false
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
fun M4xBottomBar(
    currentScreen: ScreenRoute,
    currentUser: UserEntity?,
    onNavigate: (ScreenRoute) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.HOME,
            onClick = { onNavigate(ScreenRoute.HOME) },
            icon = { Icon(Icons.Filled.Home, contentDescription = "Trang chủ") },
            label = { Text("Khám phá", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF06B6D4),
                selectedTextColor = Color(0xFF06B6D4),
                indicatorColor = Color(0xFF06B6D4).copy(alpha = 0.2f),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            ),
            modifier = Modifier.testTag("nav_home")
        )

        NavigationBarItem(
            selected = currentScreen == ScreenRoute.UPLOAD_THEME,
            onClick = { onNavigate(ScreenRoute.UPLOAD_THEME) },
            icon = { Icon(Icons.Filled.CloudUpload, contentDescription = "Tải lên Theme") },
            label = { Text("Tải lên", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFA855F7),
                selectedTextColor = Color(0xFFA855F7),
                indicatorColor = Color(0xFFA855F7).copy(alpha = 0.2f),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            ),
            modifier = Modifier.testTag("nav_upload")
        )

        NavigationBarItem(
            selected = currentScreen == ScreenRoute.REWARDS,
            onClick = { onNavigate(ScreenRoute.REWARDS) },
            icon = { Icon(Icons.Filled.Stars, contentDescription = "Đổi thưởng") },
            label = { Text("Nhiệm vụ", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFF59E0B),
                selectedTextColor = Color(0xFFF59E0B),
                indicatorColor = Color(0xFFF59E0B).copy(alpha = 0.2f),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            ),
            modifier = Modifier.testTag("nav_rewards")
        )

        if (currentUser?.role == UserRole.ADMIN) {
            NavigationBarItem(
                selected = currentScreen == ScreenRoute.ADMIN_DASHBOARD,
                onClick = { onNavigate(ScreenRoute.ADMIN_DASHBOARD) },
                icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Admin") },
                label = { Text("Bảng Admin", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFEF4444),
                    selectedTextColor = Color(0xFFEF4444),
                    indicatorColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                    unselectedIconColor = Color(0xFF64748B),
                    unselectedTextColor = Color(0xFF64748B)
                ),
                modifier = Modifier.testTag("nav_admin")
            )
        }

        NavigationBarItem(
            selected = currentScreen == ScreenRoute.PROFILE,
            onClick = { onNavigate(ScreenRoute.PROFILE) },
            icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "Trang cá nhân") },
            label = { Text("Cá nhân", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF10B981),
                selectedTextColor = Color(0xFF10B981),
                indicatorColor = Color(0xFF10B981).copy(alpha = 0.2f),
                unselectedIconColor = Color(0xFF64748B),
                unselectedTextColor = Color(0xFF64748B)
            ),
            modifier = Modifier.testTag("nav_profile")
        )
    }
}

@Composable
fun ThemeCardItem(
    theme: ThemeEntity,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("theme_card_${theme.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                when (theme.category) {
                                    "Cyberpunk" -> Color(0xFF3B0764)
                                    "iOS Style" -> Color(0xFF0284C7)
                                    "Minimal" -> Color(0xFF1E293B)
                                    "Dark Mode" -> Color(0xFF0F172A)
                                    else -> Color(0xFF1E1B4B)
                                },
                                Color(0xFF0F172A)
                            )
                        )
                    )
            ) {
                // Background image banner preview if present
                Image(
                    painter = painterResource(id = R.drawable.img_banner_hyperos_1785716083770),
                    contentDescription = theme.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC0F172A))
                            )
                        )
                )

                // Featured Badge
                if (theme.isFeatured) {
                    Surface(
                        color = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Nổi bật", tint = Color.Black, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("NỔI BẬT", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // OS Badge
                Surface(
                    color = Color(0xFF06B6D4).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = theme.osCompatibility,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Favorite Heart Toggle
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(36.dp)
                        .background(Color(0x80000000), CircleShape)
                        .testTag("favorite_toggle_${theme.id}")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (isFavorite) Color(0xFFEF4444) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Info Content
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = theme.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Palette, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = theme.creatorName,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = Color(0xFF334155),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = theme.category,
                            color = Color(0xFFCBD5E1),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", theme.averageRating),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = " (${theme.ratingCount})",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }

                    // Download count & Size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${theme.downloadCount}",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${theme.fileSizeMb}MB",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryFilterChips(
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val categories = listOf("ALL", "Minimal", "Cyberpunk", "iOS Style", "Dark Mode", "Anime")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(categories) { cat ->
            val isSelected = selectedCategory == cat
            FilterChip(
                selected = isSelected,
                onClick = { onSelectCategory(cat) },
                label = {
                    Text(
                        text = if (cat == "ALL") "Tất cả danh mục" else cat,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF06B6D4),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF94A3B8)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = Color(0xFF334155),
                    selectedBorderColor = Color(0xFF06B6D4),
                    enabled = true,
                    selected = isSelected
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_cat_$cat")
            )
        }
    }
}
