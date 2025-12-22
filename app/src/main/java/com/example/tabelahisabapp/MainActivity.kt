package com.example.tabelahisabapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tabelahisabapp.ui.company.AddCompanyScreen
import com.example.tabelahisabapp.ui.company.CompanyListScreen
import com.example.tabelahisabapp.ui.customer.AddCustomerScreen
import com.example.tabelahisabapp.ui.customer.AddTransactionScreen
import com.example.tabelahisabapp.ui.customer.CustomerLedgerScreen
import com.example.tabelahisabapp.ui.customer.CustomerListScreen
import com.example.tabelahisabapp.ui.daily.DailyEntryScreen
import com.example.tabelahisabapp.ui.daily.DailySummaryHomeScreen
import com.example.tabelahisabapp.ui.home.HomeDashboardScreen
import com.example.tabelahisabapp.ui.settings.*
import com.example.tabelahisabapp.ui.theme.TabelaHisabAppTheme
import com.example.tabelahisabapp.ui.trading.AddTradeTransactionScreen
import com.example.tabelahisabapp.ui.trading.TradingHomeScreen
import com.example.tabelahisabapp.ui.trading.FarmDetailScreen
import com.example.tabelahisabapp.ui.voice.VoiceRecordingScreen
import com.example.tabelahisabapp.ui.voice.VoiceConfirmationScreen
import com.example.tabelahisabapp.ui.voice.VoiceClarificationScreen
import com.example.tabelahisabapp.data.preferences.ThemePreferences
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreferences: ThemePreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Use remember to safely access themePreferences
            val prefs = remember {
                try {
                    if (::themePreferences.isInitialized) themePreferences else null
                } catch (e: Exception) {
                    null
                }
            }
            TabelaHisabAppTheme(themePreferences = prefs) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showExitDialog by remember { mutableStateOf(false) }

    // Handle back press on home - show exit confirmation
    BackHandler(enabled = currentRoute == "home") {
        showExitDialog = true
    }

    // Exit confirmation dialog
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Exit App") },
            text = { Text("Are you sure you want to exit?") },
            confirmButton = {
                TextButton(onClick = { 
                    showExitDialog = false
                    (navController.context as? ComponentActivity)?.finish()
                }) {
                    Text("Exit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bottom navigation items (Settings removed - accessible via gear icon in headers)
    val bottomNavItems = listOf(
        BottomNavItem("home", "Home", Icons.Default.Home),
        BottomNavItem("customers", "Customers", Icons.Default.Person),
        BottomNavItem("daily", "Daily", Icons.Default.CalendarToday),
        BottomNavItem("trading", "Trading", Icons.Default.TrendingUp)
    )

    // Main screens where bottom nav should be visible
    val mainScreens = listOf("home", "customers", "daily", "trading")

    Scaffold(
        bottomBar = {
            if (currentRoute in mainScreens) {
                NavigationBar(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    item.icon, 
                                    contentDescription = item.label,
                                    modifier = Modifier.size(if (currentRoute == item.route) 26.dp else 24.dp)
                                )
                            },
                            label = { 
                                Text(
                                    item.label,
                                    fontWeight = if (currentRoute == item.route) 
                                        androidx.compose.ui.text.font.FontWeight.Bold 
                                    else 
                                        androidx.compose.ui.text.font.FontWeight.Normal
                                )
                            },
                            selected = currentRoute == item.route,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = com.example.tabelahisabapp.ui.theme.Purple600,
                                selectedTextColor = com.example.tabelahisabapp.ui.theme.Purple600,
                                indicatorColor = com.example.tabelahisabapp.ui.theme.Purple50,
                                unselectedIconColor = com.example.tabelahisabapp.ui.theme.TextSecondary,
                                unselectedTextColor = com.example.tabelahisabapp.ui.theme.TextSecondary
                            ),
                            onClick = {
                                // Special handling for Home - always clear stack and go to home
                                if (item.route == "home") {
                                    navController.navigate("home") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { 
                slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = { 
                slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = { 
                slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = { 
                slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            // Home Dashboard
            composable("home") {
                HomeDashboardScreen(
                    onNavigateToCustomers = { filter -> 
                        if (filter != null) {
                            navController.navigate("customers?filter=$filter")
                        } else {
                            navController.navigate("customers")
                        }
                    },
                    onNavigateToDaily = { navController.navigate("daily") },
                    onNavigateToTrading = { navController.navigate("trading") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onVoiceClick = { navController.navigate("voice_flow") }
                )
            }

            // Customer Module
            composable(
                route = "customers?filter={filter}",
                arguments = listOf(navArgument("filter") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter")
                CustomerListScreen(
                    initialFilter = filter,
                    onAddCustomer = { navController.navigate("add_customer") },
                    onEditCustomer = { customerId ->
                        navController.navigate("edit_customer/$customerId")
                    },
                    onCustomerClick = { customerId ->
                        navController.navigate("customer_ledger/$customerId")
                    }
                )
            }
        composable("add_customer") {
            AddCustomerScreen(
                    savedStateHandle = null,
                    onCustomerAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_customer/{customerId}",
                arguments = listOf(navArgument("customerId") { type = NavType.IntType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getInt("customerId")
                val savedStateHandle = backStackEntry.savedStateHandle
                savedStateHandle["customerId"] = customerId
                AddCustomerScreen(
                    savedStateHandle = savedStateHandle,
                onCustomerAdded = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = "customer_ledger/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getInt("customerId") ?: 0
            CustomerLedgerScreen(
                onBackPressed = { navController.popBackStack() },
                    onAddTransaction = { 
                    navController.navigate("add_transaction/$customerId")
                    },
                    onEditTransaction = { custId, transactionId ->
                        navController.navigate("edit_transaction/$custId/$transactionId")
                },
                onVoiceClick = { navController.navigate("voice_flow") }
            )
        }
        composable(
            route = "add_transaction/{customerId}",
            arguments = listOf(navArgument("customerId") { type = NavType.IntType })
        ) {
            AddTransactionScreen(
                    savedStateHandle = null,
                    onTransactionAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_transaction/{customerId}/{transactionId}",
                arguments = listOf(
                    navArgument("customerId") { type = NavType.IntType },
                    navArgument("transactionId") { type = NavType.IntType }
                )
            ) { backStackEntry ->
                val savedStateHandle = backStackEntry.savedStateHandle
                val customerId = backStackEntry.arguments?.getInt("customerId")
                val transactionId = backStackEntry.arguments?.getInt("transactionId")
                savedStateHandle["customerId"] = customerId
                savedStateHandle["transactionId"] = transactionId
                AddTransactionScreen(
                    savedStateHandle = savedStateHandle,
                onTransactionAdded = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

            // Daily Balance Module
            composable("daily") {
                DailySummaryHomeScreen(
                    onAddToday = {
                        navController.navigate("add_daily_entry")
                    },
                    onEditEntry = { date ->
                        navController.navigate("edit_daily_entry/$date")
                    }
                )
            }
            composable("add_daily_entry") {
                DailyEntryScreen(
                    onEntrySaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_daily_entry/{date}",
                arguments = listOf(navArgument("date") { type = NavType.LongType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: 0L
                DailyEntryScreen(
                    editDate = date,
                    onEntrySaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // Trading Module
            composable("trading") {
                TradingHomeScreen(
                    onAddTransaction = {
                        navController.navigate("add_trade_transaction")
                    },
                    onEditTransaction = { transactionId ->
                        navController.navigate("edit_trade_transaction/$transactionId")
                    },
                    onFarmClick = { farmId ->
                        navController.navigate("farm_detail/$farmId")
                    }
                )
            }
            composable(
                route = "farm_detail/{farmId}",
                arguments = listOf(navArgument("farmId") { type = NavType.IntType })
            ) { backStackEntry ->
                val farmId = backStackEntry.arguments?.getInt("farmId") ?: 0
                FarmDetailScreen(
                    farmId = farmId,
                    onBackClick = { navController.popBackStack() },
                    onAddTransaction = { fId ->
                        navController.navigate("add_trade_to_farm/$fId")
                    },
                    onEditTransaction = { transactionId ->
                        navController.navigate("edit_trade_transaction/$transactionId")
                    }
                )
            }
            composable("add_trade_transaction") {
                AddTradeTransactionScreen(
                    savedStateHandle = null,
                    farmId = null,
                    onTransactionSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable(
                route = "add_trade_to_farm/{farmId}",
                arguments = listOf(navArgument("farmId") { type = NavType.IntType })
            ) { backStackEntry ->
                val farmId = backStackEntry.arguments?.getInt("farmId")
                AddTradeTransactionScreen(
                    savedStateHandle = null,
                    farmId = farmId,
                    onTransactionSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            composable(
                route = "edit_trade_transaction/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.IntType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getInt("transactionId")
                val savedStateHandle = backStackEntry.savedStateHandle
                savedStateHandle["transactionId"] = transactionId
                AddTradeTransactionScreen(
                    savedStateHandle = savedStateHandle,
                    onTransactionSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            // Settings Module
            composable("settings") {
                SettingsScreen(
                    onNavigateToBackup = { navController.navigate("settings_backup") },
                    onNavigateToExport = { navController.navigate("settings_export") },
                    onNavigateToTheme = { navController.navigate("settings_theme") },
                    onNavigateToCompany = { navController.navigate("settings_company") },
                    onNavigateToAbout = { navController.navigate("settings_about") }
                )
            }
            composable("settings_backup") {
                BackupRestoreScreen(onBackPressed = { navController.popBackStack() })
            }
            composable("settings_export") {
                ExportPrintScreen(onBackPressed = { navController.popBackStack() })
            }
            composable("settings_theme") {
                ThemeScreen(onBackPressed = { navController.popBackStack() })
            }
            composable("settings_company") {
                CompanyListScreen(
                    onAddCompany = { navController.navigate("add_company") },
                    onEditCompany = { companyId ->
                        navController.navigate("edit_company/$companyId")
                    }
                )
            }
            composable("add_company") {
                AddCompanyScreen(
                    savedStateHandle = null,
                    onCompanyAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_company/{companyId}",
                arguments = listOf(navArgument("companyId") { type = NavType.IntType })
            ) { backStackEntry ->
                val companyId = backStackEntry.arguments?.getInt("companyId")
                val savedStateHandle = backStackEntry.savedStateHandle
                savedStateHandle["companyId"] = companyId
                AddCompanyScreen(
                    savedStateHandle = savedStateHandle,
                    onCompanyAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("settings_about") {
                AboutScreen(onBackPressed = { navController.popBackStack() })
            }
            
            // Voice Recording Module - Use nested navigation to share ViewModel
            navigation(startDestination = "voice_recording_screen", route = "voice_flow") {
                composable("voice_recording_screen") { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry("voice_flow")
                    }
                    VoiceRecordingScreen(
                        viewModel = hiltViewModel(parentEntry),
                        onNavigateToConfirmation = { navController.navigate("voice_confirmation_screen") },
                        onNavigateToClarification = { navController.navigate("voice_clarification_screen") },
                        onCancel = { navController.popBackStack("voice_flow", inclusive = true) }
                    )
                }
                composable("voice_confirmation_screen") { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry("voice_flow")
                    }
                    VoiceConfirmationScreen(
                        viewModel = hiltViewModel(parentEntry),
                        onConfirmSuccess = { 
                            navController.popBackStack("voice_flow", inclusive = true)
                        },
                        onEdit = { 
                            // Navigate back to recording screen within the same flow
                            navController.popBackStack("voice_recording_screen", inclusive = false)
                        },
                        onCancel = { navController.popBackStack("voice_flow", inclusive = true) }
                    )
                }
                composable("voice_clarification_screen") { entry ->
                    val parentEntry = remember(entry) {
                        navController.getBackStackEntry("voice_flow")
                    }
                    VoiceClarificationScreen(
                        viewModel = hiltViewModel(parentEntry),
                        onTryAgain = { 
                            navController.popBackStack()
                            navController.navigate("voice_recording_screen")
                        },
                        onManualEntry = { navController.popBackStack("voice_flow", inclusive = true) },
                        onCancel = { navController.popBackStack("voice_flow", inclusive = true) }
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
