package com.xis.mypower

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import kotlinx.coroutines.launch
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xis.mypower.ui.components.BottomNav
import com.xis.mypower.ui.screens.ConfigScreen
import com.xis.mypower.ui.screens.ControlScreen
import com.xis.mypower.ui.screens.DashboardScreen
import com.xis.mypower.ui.screens.SettingsDrawerContent
import com.xis.mypower.ui.screens.SplashScreen
import com.xis.mypower.ui.theme.MyPowerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BlynkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyPowerTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route ?: "splash"
                val appState by viewModel.uiState.collectAsState()
                
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = currentRoute in listOf("dashboard", "history", "control"),
                    drawerContent = {
                        SettingsDrawerContent(
                            onNavigateToConfig = { scope.launch { drawerState.close() }; navController.navigate("config") },
                            onNavigateToTutorial = { scope.launch { drawerState.close() }; navController.navigate("tutorial") },
                            onNavigateToAbout = { scope.launch { drawerState.close() }; navController.navigate("about") },
                            onLogout = {
                                scope.launch { drawerState.close() }
                                viewModel.logout()
                                navController.navigate("config") { popUpTo(0) }
                            }
                        )
                    }
                ) {
                    Scaffold(
                    bottomBar = {
                        if (currentRoute in listOf("dashboard", "history", "control")) {
                            BottomNav(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(
                                    hasValidToken = viewModel.hasValidToken(),
                                    onNavigateToMain = {
                                        navController.navigate("dashboard") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    },
                                    onNavigateToConfig = {
                                        navController.navigate("config") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("config") {
                                ConfigScreen(
                                    initialToken = viewModel.authToken,
                                    initialId = viewModel.templateId,
                                    initialName = viewModel.templateName,
                                    onConnectSuccess = {
                                        if (navController.previousBackStackEntry?.destination?.route == "settings") {
                                            navController.popBackStack()
                                        } else {
                                            navController.navigate("dashboard") {
                                                popUpTo("config") { inclusive = true }
                                            }
                                        }
                                    },
                                    onVerify = { token, tId, tName, callback ->
                                        viewModel.verifyAndSaveCredentials(token, tId, tName, callback)
                                    }
                                )
                            }
                            composable("dashboard") {
                                DashboardScreen(
                                    state = appState,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            composable("tutorial") {
                                com.xis.mypower.ui.screens.TutorialScreen()
                            }
                            composable("about") {
                                com.xis.mypower.ui.screens.AboutScreen()
                            }
                            composable("history") {
                                com.xis.mypower.ui.screens.HistoryScreen(
                                    state = appState,
                                    onOpenDrawer = { scope.launch { drawerState.open() } }
                                )
                            }
                            composable("control") {
                                ControlScreen(
                                    state = appState,
                                    onOpenDrawer = { scope.launch { drawerState.open() } },
                                    onModeChange = { isAuto -> viewModel.setMode(isAuto) },
                                    onRelayChange = { pin, isOn -> viewModel.setRelay(pin, isOn) }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}