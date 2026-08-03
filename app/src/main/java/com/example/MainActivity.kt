package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.M4xBottomBar
import com.example.ui.components.M4xTopBar
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenRoute
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                M4xThemeApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M4xThemeApp(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val unreadNotifCount by viewModel.unreadNotifCount.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle snackbar messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearSnackbar()
            }
        }
    }

    Scaffold(
        topBar = {
            M4xTopBar(
                currentUser = currentUser,
                unreadNotifCount = unreadNotifCount,
                currentScreen = currentScreen,
                onNavigate = { viewModel.navigateTo(it) },
                onSwitchRole = { viewModel.switchUserRole(it) },
                onCheckOta = {
                    viewModel.checkOtaUpdate()
                    viewModel.navigateTo(ScreenRoute.PROFILE)
                }
            )
        },
        bottomBar = {
            M4xBottomBar(
                currentScreen = currentScreen,
                currentUser = currentUser,
                onNavigate = { viewModel.navigateTo(it) }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = Color(0xFF06B6D4),
                        contentColor = Color.White,
                        actionColor = Color.White
                    )
                }
            )
        },
        containerColor = Color(0xFF0F172A)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    ScreenRoute.HOME -> HomeScreen(
                        viewModel = viewModel,
                        onOpenThemeDetail = { themeId -> viewModel.openThemeDetail(themeId) }
                    )
                    ScreenRoute.THEME_DETAIL -> ThemeDetailScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(ScreenRoute.HOME) }
                    )
                    ScreenRoute.UPLOAD_THEME -> UploadThemeScreen(
                        viewModel = viewModel
                    )
                    ScreenRoute.ADMIN_DASHBOARD -> AdminDashboardScreen(
                        viewModel = viewModel,
                        onOpenThemeDetail = { themeId -> viewModel.openThemeDetail(themeId) }
                    )
                    ScreenRoute.PROFILE -> ProfileScreen(
                        viewModel = viewModel,
                        onOpenThemeDetail = { themeId -> viewModel.openThemeDetail(themeId) }
                    )
                    ScreenRoute.REWARDS -> RewardsScreen(
                        viewModel = viewModel
                    )
                    ScreenRoute.NOTIFICATIONS -> NotificationsScreen(
                        viewModel = viewModel
                    )
                    else -> HomeScreen(
                        viewModel = viewModel,
                        onOpenThemeDetail = { themeId -> viewModel.openThemeDetail(themeId) }
                    )
                }
            }
        }
    }
}
