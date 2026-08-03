package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ThemeEntity
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.ThemeCardItem
import com.example.ui.viewmodel.HomeTab
import com.example.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenThemeDetail: (Long) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedOs by viewModel.selectedOs.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val activeHomeTab by viewModel.activeHomeTab.collectAsState()
    val approvedThemes by viewModel.filteredApprovedThemes.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()

    val osList = listOf("ALL", "HyperOS 2.0", "HyperOS 1.0", "MIUI 14", "MIUI 13")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Search & OS Filter Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Tìm kiếm theme, creator, tag (Ví dụ: Cyber, iOS)...", color = Color(0xFF64748B), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color(0xFF06B6D4)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Color(0xFF94A3B8))
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF06B6D4),
                    unfocusedBorderColor = Color(0xFF334155),
                    focusedContainerColor = Color(0xFF1E293B),
                    unfocusedContainerColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // OS Version Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(osList.size) { index ->
                    val os = osList[index]
                    val isSelected = selectedOs == os
                    Surface(
                        onClick = { viewModel.setSelectedOs(os) },
                        color = if (isSelected) Color(0xFF06B6D4) else Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)),
                        modifier = Modifier.testTag("os_filter_$os")
                    ) {
                        Text(
                            text = if (os == "ALL") "Tất cả OS" else os,
                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Hero Featured Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onOpenThemeDetail(1) }
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_banner_hyperos_1785716083770),
                contentDescription = "Featured Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xE60F172A), Color(0x660F172A), Color.Transparent)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
            ) {
                Surface(
                    color = Color(0xFFA855F7),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "HOT HYPEROS 2.0",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "CyberGlass HyperOS 2.0",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Bởi HyperDesign Studio • Control Center 3D Glass",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onOpenThemeDetail(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("banner_cta_button")
                ) {
                    Text("Tải ngay (.mtz)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Category Filter Chips
        CategoryFilterChips(
            selectedCategory = selectedCategory,
            onSelectCategory = { viewModel.setSelectedCategory(it) }
        )

        // Home Tabs: Featured, Latest, Downloads, Rating, Updates
        ScrollableTabRow(
            selectedTabIndex = activeHomeTab.ordinal,
            containerColor = Color(0xFF0F172A),
            contentColor = Color(0xFF06B6D4),
            edgePadding = 16.dp,
            divider = { HorizontalDivider(color = Color(0xFF1E293B)) }
        ) {
            HomeTab.values().forEach { tab ->
                val title = when (tab) {
                    HomeTab.FEATURED -> "🔥 Nổi bật"
                    HomeTab.LATEST -> "✨ Mới nhất"
                    HomeTab.TOP_DOWNLOADS -> "⚡ Tải nhiều"
                    HomeTab.TOP_RATED -> "⭐ Đánh giá cao"
                    HomeTab.UPDATES -> "🔔 Bản cập nhật"
                }
                Tab(
                    selected = activeHomeTab == tab,
                    onClick = { viewModel.setActiveHomeTab(tab) },
                    text = {
                        Text(
                            text = title,
                            color = if (activeHomeTab == tab) Color(0xFF06B6D4) else Color(0xFF64748B),
                            fontWeight = if (activeHomeTab == tab) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("home_tab_${tab.name}")
                )
            }
        }

        // Theme Grid
        if (approvedThemes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = "Không tìm thấy",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Không tìm thấy Theme phù hợp", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Hãy thử đổi từ khóa tìm kiếm hoặc lọc danh mục khác", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(approvedThemes) { theme ->
                    ThemeCardItem(
                        theme = theme,
                        isFavorite = favoriteIds.contains(theme.id),
                        onClick = { onOpenThemeDetail(theme.id) },
                        onFavoriteToggle = { viewModel.toggleFavorite(theme.id) }
                    )
                }
            }
        }
    }
}
