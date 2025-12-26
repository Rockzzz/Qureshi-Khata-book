package com.example.tabelahisabapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tabelahisabapp.ui.accounts.AccountsScreen
import com.example.tabelahisabapp.ui.company.AddCompanyScreen
import com.example.tabelahisabapp.ui.company.CompanyListScreen
import com.example.tabelahisabapp.ui.customer.AddCustomerScreen
import com.example.tabelahisabapp.ui.customer.AddTransactionScreen
import com.example.tabelahisabapp.ui.customer.CustomerLedgerScreen
import com.example.tabelahisabapp.ui.customer.CustomerListScreen
import com.example.tabelahisabapp.ui.supplier.AddSupplierScreen
import com.example.tabelahisabapp.ui.supplier.SupplierLedgerScreen
import com.example.tabelahisabapp.ui.expense.AddExpenseScreen
import com.example.tabelahisabapp.ui.daily.DailyEntryScreen
import com.example.tabelahisabapp.ui.daily.DailySummaryHomeScreen
import com.example.tabelahisabapp.ui.daily.EnhancedDailySummaryScreen
import com.example.tabelahisabapp.ui.home.HomeDashboardScreen
import com.example.tabelahisabapp.ui.settings.*
import com.example.tabelahisabapp.ui.theme.*
import com.example.tabelahisabapp.ui.trading.AddTradeTransactionScreen
import com.example.tabelahisabapp.ui.trading.TradingHomeScreen
import com.example.tabelahisabapp.ui.trading.FarmDetailScreen
import com.example.tabelahisabapp.ui.voice.VoiceRecordingScreen
import com.example.tabelahisabapp.ui.voice.VoiceConfirmationScreen
import com.example.tabelahisabapp.ui.voice.VoiceClarificationScreen
import com.example.tabelahisabapp.data.preferences.ThemePreferences
import com.example.tabelahisabapp.ui.expense.AddExpenseScreen
import com.example.tabelahisabapp.ui.supplier.AddSupplierScreen
import com.example.tabelahisabapp.ui.supplier.AddSupplierTransactionScreen
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
                Surface(
                    modifier = Modifier.fillMaxSize(), 
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * Bottom navigation item data class
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var showExitDialog by remember { mutableStateOf(false) }
    
    // Get theme colors
    val extendedColors = AppTheme.colors
    val isDark = AppTheme.isDark

    // Handle back press on home - show exit confirmation
    BackHandler(enabled = currentRoute == "home") {
        showExitDialog = true
    }

    // Exit confirmation dialog - Beautiful centered design
    if (showExitDialog) {
        Dialog(
            onDismissRequest = { showExitDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(0.85f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Exit Icon
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = Purple50
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Purple600
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Title
                        Text(
                            text = "Exit App?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Message
                        Text(
                            text = "Are you sure you want to exit the application?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cancel Button
                            OutlinedButton(
                                onClick = { showExitDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(
                                    "Cancel",
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            
                            // Exit Button
                            Button(
                                onClick = { 
                                    showExitDialog = false
                                    (navController.context as? ComponentActivity)?.finish()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Exit",
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW NAVIGATION: Home | Accounts | Daily | Trading
    // ═══════════════════════════════════════════════════════════════════════════
    val bottomNavItems = listOf(
        BottomNavItem("home", "Home", Icons.Outlined.Home, Icons.Filled.Home),
        BottomNavItem("accounts", "Accounts", Icons.Outlined.AccountBalance, Icons.Filled.AccountBalance),
        BottomNavItem("daily", "Daily", Icons.Outlined.CalendarToday, Icons.Filled.CalendarToday),
        BottomNavItem("trading", "Trading", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp)
    )

    // Main screens where bottom nav should be visible
    val mainScreens = listOf("home", "accounts", "daily", "trading")

    Scaffold(
        bottomBar = {
            if (currentRoute in mainScreens) {
                NavigationBar(
                    containerColor = extendedColors.navBarBackground,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    if (isSelected) item.selectedIcon else item.icon, 
                                    contentDescription = item.label,
                                    modifier = Modifier.size(if (isSelected) 26.dp else 24.dp)
                                )
                            },
                            label = { 
                                Text(
                                    item.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = extendedColors.navBarSelected,
                                selectedTextColor = extendedColors.navBarSelected,
                                indicatorColor = if (isDark) Purple700.copy(alpha = 0.3f) else Purple50,
                                unselectedIconColor = extendedColors.navBarUnselected,
                                unselectedTextColor = extendedColors.navBarUnselected
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
            // ═══════════════════════════════════════════════════════════════════
            // HOME DASHBOARD
            // ═══════════════════════════════════════════════════════════════════
            composable("home") {
                HomeDashboardScreen(
                    onNavigateToCustomers = { filter -> 
                        // Navigate to Accounts tab with optional filter
                        navController.navigate("accounts?tab=0&filter=${filter ?: ""}")
                    },
                    onNavigateToSuppliers = { filter ->
                        // Navigate to Suppliers tab (tab=1) with optional filter
                        navController.navigate("accounts?tab=1&filter=${filter ?: ""}")
                    },
                    onNavigateToDaily = { navController.navigate("daily") },
                    onNavigateToTrading = { navController.navigate("trading") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onVoiceClick = { navController.navigate("voice_flow") }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // ACCOUNTS MODULE (Container with Customers/Suppliers/Expenses tabs)
            // ═══════════════════════════════════════════════════════════════════
            composable(
                route = "accounts?tab={tab}&filter={filter}",
                arguments = listOf(
                    navArgument("tab") { 
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("filter") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                val initialFilter = backStackEntry.arguments?.getString("filter")
                AccountsScreen(
                    initialTab = initialTab,
                    initialFilter = initialFilter,
                    onAddCustomer = { navController.navigate("add_customer") },
                    onEditCustomer = { customerId ->
                        navController.navigate("edit_customer/$customerId")
                    },
                    onCustomerClick = { customerId ->
                        navController.navigate("customer_ledger/$customerId")
                    },
                    onAddSupplier = { navController.navigate("add_supplier") },
                    onEditSupplier = { supplierId ->
                        navController.navigate("edit_supplier/$supplierId")
                    },
                    onSupplierClick = { supplierId ->
                        navController.navigate("supplier_ledger/$supplierId")
                    },
                    onAddExpense = { navController.navigate("add_expense") },
                    onEditExpense = { expenseId ->
                        navController.navigate("edit_expense/$expenseId")
                    },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // LEGACY CUSTOMERS ROUTE (Redirect to Accounts tab 0)
            // ═══════════════════════════════════════════════════════════════════
            composable(
                route = "customers?filter={filter}",
                arguments = listOf(navArgument("filter") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val filter = backStackEntry.arguments?.getString("filter")
                // Redirect to new Accounts screen
                LaunchedEffect(Unit) {
                    navController.navigate("accounts?tab=0&filter=${filter ?: ""}") {
                        popUpTo("customers?filter={filter}") { inclusive = true }
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // CUSTOMER CRUD OPERATIONS
            // ═══════════════════════════════════════════════════════════════════
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
                    onAddTransaction = { custId, type -> 
                        navController.navigate("add_transaction/$custId/CUSTOMER?initialType=$type")
                    },
                    onEditTransaction = { _, transactionId ->
                        navController.navigate("edit_transaction/$customerId/$transactionId/CUSTOMER")
                    },
                    onVoiceClick = { navController.navigate("voice_flow") }
                )
            }
            
            composable(
                route = "add_transaction/{customerId}/{transactionContext}?initialType={initialType}",
                arguments = listOf(
                    navArgument("customerId") { type = NavType.IntType },
                    navArgument("transactionContext") { type = NavType.StringType },
                    navArgument("initialType") { type = NavType.StringType; nullable = true; defaultValue = "DEBIT" }
                )
            ) { 
                AddTransactionScreen(
                    onTransactionAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "edit_transaction/{customerId}/{transactionId}/{transactionContext}",
                arguments = listOf(
                    navArgument("customerId") { type = NavType.IntType },
                    navArgument("transactionId") { type = NavType.IntType },
                    navArgument("transactionContext") { type = NavType.StringType; defaultValue = "CUSTOMER" }
                )
            ) { 
                AddTransactionScreen(
                    onTransactionAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // SUPPLIER CRUD OPERATIONS
            // ═══════════════════════════════════════════════════════════════════
            composable("add_supplier") {
                AddSupplierScreen(
                    onSupplierSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "edit_supplier/{supplierId}",
                arguments = listOf(navArgument("supplierId") { type = NavType.IntType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getInt("supplierId")
                AddSupplierScreen(
                    editSupplierId = supplierId,
                    onSupplierSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "supplier_ledger/{supplierId}",
                arguments = listOf(navArgument("supplierId") { type = NavType.IntType })
            ) { backStackEntry ->
                val supplierId = backStackEntry.arguments?.getInt("supplierId") ?: 0
                // Use dedicated SupplierLedgerScreen to avoid ViewModel crash
                SupplierLedgerScreen(
                    onBackPressed = { navController.popBackStack() },
                    onAddTransaction = { suppId, type ->
                        navController.navigate("add_supplier_transaction/$suppId/SUPPLIER?initialType=$type")
                    },
                    onEditTransaction = { _, transactionId ->
                        navController.navigate("edit_supplier_transaction/$supplierId/$transactionId/SUPPLIER")
                    }
                )
            }
            
            // Add Supplier Transaction (Purchase/Payment)
            composable(
                route = "add_supplier_transaction/{supplierId}/{transactionContext}?initialType={initialType}",
                arguments = listOf(
                    navArgument("supplierId") { type = NavType.IntType },
                    navArgument("transactionContext") { type = NavType.StringType; defaultValue = "SUPPLIER" },
                    navArgument("initialType") { type = NavType.StringType; nullable = true; defaultValue = "DEBIT" }
                )
            ) { 
                AddTransactionScreen(
                    onTransactionAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            
            // Edit Supplier Transaction
            composable(
                route = "edit_supplier_transaction/{supplierId}/{transactionId}/{transactionContext}",
                arguments = listOf(
                    navArgument("supplierId") { type = NavType.IntType },
                    navArgument("transactionId") { type = NavType.IntType },
                    navArgument("transactionContext") { type = NavType.StringType; defaultValue = "SUPPLIER" }
                )
            ) { 
                AddTransactionScreen(
                    onTransactionAdded = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // EXPENSE CRUD OPERATIONS
            // ═══════════════════════════════════════════════════════════════════
            composable("add_expense") {
                AddExpenseScreen(
                    onExpenseSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }
            
            composable(
                route = "edit_expense/{expenseId}",
                arguments = listOf(navArgument("expenseId") { type = NavType.IntType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getInt("expenseId") ?: 0
                AddExpenseScreen(
                    editExpenseId = expenseId,
                    onExpenseSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // DAILY BALANCE MODULE (Enhanced with Bidirectional Sync)
            // ═══════════════════════════════════════════════════════════════════
            composable("daily") {
                EnhancedDailySummaryScreen(
                    onAddCustomerTransaction = { date, isGiven ->
                        // Navigate to add transaction with locked date
                        navController.navigate("add_daily_customer_transaction/$date/$isGiven")
                    },
                    onAddSupplierTransaction = { date ->
                        navController.navigate("add_daily_supplier_transaction/$date")
                    },
                    onAddExpense = { date ->
                        navController.navigate("add_daily_expense/$date")
                    },
                    onEditEntry = { date ->
                        navController.navigate("edit_daily_entry/$date")
                    },
                    onViewCustomerLedger = { customerId ->
                        navController.navigate("customer_ledger/$customerId")
                    },
                    onViewSupplierLedger = { supplierId ->
                        navController.navigate("supplier_ledger/$supplierId")
                    }
                )
            }
            
            // Add customer transaction from Daily screen (date locked)
            // Note: This route needs customerId which we don't have yet
            // For now, this route is not used - transactions are added via customer ledger
            composable(
                route = "add_daily_customer_transaction/{date}/{isGiven}",
                arguments = listOf(
                    navArgument("date") { type = NavType.LongType },
                    navArgument("isGiven") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                // This route is deprecated - redirect to customer selection
                LaunchedEffect(Unit) {
                    navController.navigate("accounts?tab=0") {
                        popUpTo("add_daily_customer_transaction/{date}/{isGiven}") { inclusive = true }
                    }
                }
            }
            
            // Add supplier transaction from Daily screen (date locked)
            composable(
                route = "add_daily_supplier_transaction/{date}",
                arguments = listOf(navArgument("date") { type = NavType.LongType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                // Navigate to supplier selection first, then transaction
                // For now, go to accounts tab with suppliers
                LaunchedEffect(Unit) {
                    navController.navigate("accounts?tab=1") {
                        popUpTo("add_daily_supplier_transaction/{date}") { inclusive = true }
                    }
                }
            }
            
            // Add expense from Daily screen (date locked)
            composable(
                route = "add_daily_expense/{date}",
                arguments = listOf(navArgument("date") { type = NavType.LongType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                AddExpenseScreen(
                    lockedDate = date,
                    onExpenseSaved = { navController.popBackStack() },
                    onCancel = { navController.popBackStack() }
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

            // ═══════════════════════════════════════════════════════════════════
            // TRADING MODULE
            // ═══════════════════════════════════════════════════════════════════
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

            // ═══════════════════════════════════════════════════════════════════
            // SETTINGS MODULE
            // ═══════════════════════════════════════════════════════════════════
            composable("settings") {
                SettingsScreen(
                    onNavigateToBackup = { navController.navigate("settings_backup") },
                    onNavigateToExport = { navController.navigate("settings_export") },
                    onNavigateToBulkImport = { navController.navigate("settings_bulk_import") },
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
            
            composable("settings_bulk_import") {
                BulkImportScreen(onBackPressed = { navController.popBackStack() })
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
            
            // ═══════════════════════════════════════════════════════════════════
            // VOICE RECORDING MODULE
            // ═══════════════════════════════════════════════════════════════════
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
