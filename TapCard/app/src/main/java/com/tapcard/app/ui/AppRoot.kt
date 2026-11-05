package com.tapcard.app.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tapcard.app.data.CardRepository
import com.tapcard.app.data.ThemeController
import com.tapcard.app.data.auth.AuthManager
import com.tapcard.app.data.auth.AuthState
import com.tapcard.app.data.cloud.CloudMode
import com.tapcard.app.ui.screens.AuthScreen
import com.tapcard.app.ui.screens.CardsScreen
import com.tapcard.app.ui.screens.CreateCardScreen
import com.tapcard.app.ui.screens.DashboardScreen
import com.tapcard.app.ui.screens.NfcWriteScreen
import com.tapcard.app.ui.screens.OnboardingScreen
import com.tapcard.app.ui.screens.PreviewScreen
import com.tapcard.app.ui.screens.QrScreen
import com.tapcard.app.ui.screens.SettingsScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val CARDS = "cards"
    const val SETTINGS = "settings"
    const val CREATE_CARD = "create"
    const val PREVIEW = "preview/{cardId}"
    const val NFC_WRITE = "nfc/{cardId}"
    const val QR = "qr/{cardId}"
    const val EDIT_CARD = "edit/{cardId}"
    const val SCAN = "scan"

    fun preview(cardId: String) = "preview/$cardId"
    fun nfc(cardId: String) = "nfc/$cardId"
    fun qr(cardId: String) = "qr/$cardId"
    fun edit(cardId: String) = "edit/$cardId"
}

private data class TopLevelDestination(val route: String, val label: String, val icon: ImageVector)

private val topLevel = listOf(
    TopLevelDestination(Routes.DASHBOARD, "Home", Icons.Filled.Home),
    TopLevelDestination(Routes.CARDS, "Cards", Icons.Filled.Person),
    TopLevelDestination(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun AppRoot() {
    val onboardingDone by ThemeController.onboardingDone.collectAsState()

    when {
        // First launch comes before any auth gate — users understand the
        // product before being asked for an account.
        !onboardingDone -> OnboardingScreen(onGetStarted = { /* recomposes via flag */ })
        !CloudMode.isConfigured -> MainApp()
        AuthManager.authState.collectAsState().value is AuthState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("TapCard", style = MaterialTheme.typography.headlineMedium)
            }
        }
        AuthManager.authState.collectAsState().value is AuthState.LoggedOut -> AuthScreen()
        else -> MainApp()
    }
}

@Composable
private fun MainApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevel.map { it.route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                // Floating glass pill nav with a plasma center FAB (2026 look)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(34.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 18.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .height(78.dp)
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            NavSlot(Icons.Filled.Home, "Home", currentRoute == Routes.DASHBOARD, Modifier.weight(1f)) {
                                navController.navigateTop(Routes.DASHBOARD)
                            }
                            NavSlot(Icons.Filled.QrCode2, "QR", false, Modifier.weight(1f)) {
                                CardRepository.activeCard()?.let { navController.navigate(Routes.qr(it.id)) }
                            }
                            // Center plasma FAB — Create
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Box(contentAlignment = Alignment.Center) {
                                    Box(
                                        Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        com.tapcard.app.ui.theme.PlasmaGlow.copy(alpha = 0.55f),
                                                        Color.Transparent,
                                                    ),
                                                ),
                                            ),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    com.tapcard.app.ui.theme.BrandGradientColors,
                                                ),
                                                CircleShape,
                                            )
                                            .clickable { navController.navigate(Routes.CREATE_CARD) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Create card",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp),
                                        )
                                    }
                                }
                            }
                            NavSlot(Icons.Filled.Person, "Cards", currentRoute == Routes.CARDS, Modifier.weight(1f)) {
                                navController.navigateTop(Routes.CARDS)
                            }
                            NavSlot(Icons.Filled.Settings, "Settings", currentRoute == Routes.SETTINGS, Modifier.weight(1f)) {
                                navController.navigateTop(Routes.SETTINGS)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(200)) + scaleIn(initialScale = 0.96f, animationSpec = tween(200)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onCreateCard = { navController.navigate(Routes.CREATE_CARD) },
                    onOpenCard = { navController.navigate(Routes.preview(it)) },
                    onWriteNfc = { navController.navigate(Routes.nfc(it)) },
                    onShowQr = { navController.navigate(Routes.qr(it)) },
                    onManageCards = { navController.navigate(Routes.CARDS) },
                    onScanCard = { navController.navigate(Routes.SCAN) },
                )
            }
            composable(Routes.CARDS) {
                CardsScreen(
                    onCreateCard = { navController.navigate(Routes.CREATE_CARD) },
                    onOpenCard = { navController.navigate(Routes.preview(it)) },
                    onWriteNfc = { navController.navigate(Routes.nfc(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.CREATE_CARD) {
                CreateCardScreen(
                    onCancel = { navController.popBackStack() },
                    onSaved = { newId ->
                        navController.navigate(Routes.preview(newId)) {
                            popUpTo(Routes.CREATE_CARD) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.SCAN) {
                com.tapcard.app.ui.screens.ScanScreen(
                    onBack = { navController.popBackStack() },
                    onParsed = {
                        navController.navigate(Routes.CREATE_CARD) {
                            popUpTo(Routes.SCAN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.EDIT_CARD) { entry ->
                val cardId = entry.arguments?.getString("cardId")
                CreateCardScreen(
                    cardId = cardId,
                    onCancel = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(Routes.PREVIEW) { entry ->
                val cardId = entry.arguments?.getString("cardId")
                PreviewScreen(
                    cardId = cardId,
                    onWriteNfc = { navController.navigate(Routes.nfc(it)) },
                    onShowQr = { navController.navigate(Routes.qr(it)) },
                    onEdit = { navController.navigate(Routes.edit(it)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.NFC_WRITE) { entry ->
                val cardId = entry.arguments?.getString("cardId")
                NfcWriteScreen(
                    cardId = cardId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.QR) { entry ->
                val cardId = entry.arguments?.getString("cardId")
                QrScreen(
                    cardId = cardId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Bottom-nav slot: icon + label with plasma selected state. */
@Composable
private fun NavSlot(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp),
        )
        Spacer(Modifier.size(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun NavHostController.navigateTop(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
