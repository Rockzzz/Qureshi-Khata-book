package com.example.tabelahisabapp.ui.accounts

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tabelahisabapp.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Tab configuration for Accounts container
 */
data class AccountTab(
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

/**
 * AccountsScreen - Container with horizontal tabs for Customers, Suppliers, and Expenses
 * This replaces the old "Customers" bottom nav item with a unified "Accounts" view
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    initialTab: Int = 0,
    initialFilter: String? = null,
    // Customer callbacks
    onAddCustomer: () -> Unit = {},
    onEditCustomer: (Int) -> Unit = {},
    onCustomerClick: (Int) -> Unit = {},
    // Supplier callbacks
    onAddSupplier: () -> Unit = {},
    onEditSupplier: (Int) -> Unit = {},
    onSupplierClick: (Int) -> Unit = {},
    // Expense callbacks
    onAddExpense: () -> Unit = {},
    onEditExpense: (Int) -> Unit = {},
    // Settings
    onNavigateToSettings: () -> Unit = {}
) {
    val tabs = listOf(
        AccountTab("Customers", Icons.Outlined.Person, Icons.Filled.Person),
        AccountTab("Suppliers", Icons.Outlined.Store, Icons.Filled.Store),
        AccountTab("Expenses", Icons.Outlined.Receipt, Icons.Filled.Receipt)
    )
    
    val pagerState = rememberPagerState(
        initialPage = initialTab.coerceIn(0, tabs.size - 1),
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Get theme colors
    val extendedColors = AppTheme.colors
    val isDark = AppTheme.isDark

    Scaffold(
        containerColor = if (isDark) DarkBackground else BackgroundGray,
        topBar = {
            // Gradient header matching Home screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                extendedColors.gradientStart,
                                extendedColors.gradientEnd
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Accounts",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Custom Tab Row with pill-style tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = pagerState.currentPage == index
                            val backgroundColor by animateColorAsState(
                                targetValue = if (isSelected) Color.White else Color.Transparent,
                                animationSpec = tween(300),
                                label = "tabBackground"
                            )
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) extendedColors.gradientStart else Color.White.copy(alpha = 0.8f),
                                animationSpec = tween(300),
                                label = "tabContent"
                            )
                            
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = backgroundColor,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                                        contentDescription = tab.title,
                                        tint = contentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) { paddingValues ->
        // Horizontal pager for swipeable tabs
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> {
                    // Customers Tab
                    CustomerTabContent(
                        initialFilter = initialFilter,
                        onAddCustomer = onAddCustomer,
                        onEditCustomer = onEditCustomer,
                        onCustomerClick = onCustomerClick
                    )
                }
                1 -> {
                    // Suppliers Tab
                    SupplierTabContent(
                        initialFilter = initialFilter,
                        onAddSupplier = onAddSupplier,
                        onEditSupplier = onEditSupplier,
                        onSupplierClick = onSupplierClick
                    )
                }
                2 -> {
                    // Expenses Tab
                    ExpenseTabContent(
                        onAddExpense = onAddExpense,
                        onEditExpense = onEditExpense
                    )
                }
            }
        }
    }
}

/**
 * Customer tab content - Shows only customers
 */
@Composable
private fun CustomerTabContent(
    initialFilter: String? = null,
    onAddCustomer: () -> Unit,
    onEditCustomer: (Int) -> Unit,
    onCustomerClick: (Int) -> Unit
) {
    com.example.tabelahisabapp.ui.customer.CustomerListContent(
        initialFilter = initialFilter,
        onAddCustomer = onAddCustomer,
        onEditCustomer = onEditCustomer,
        onCustomerClick = onCustomerClick
    )
}

/**
 * Supplier tab content - Shows only sellers/suppliers
 */
@Composable
private fun SupplierTabContent(
    initialFilter: String? = null,
    onAddSupplier: () -> Unit,
    onEditSupplier: (Int) -> Unit,
    onSupplierClick: (Int) -> Unit
) {
    com.example.tabelahisabapp.ui.supplier.SupplierListContent(
        initialFilter = initialFilter,
        onAddSupplier = onAddSupplier,
        onEditSupplier = onEditSupplier,
        onSupplierClick = onSupplierClick
    )
}

/**
 * Expense tab content - Shows expenses by category
 */
@Composable
private fun ExpenseTabContent(
    onAddExpense: () -> Unit,
    onEditExpense: (Int) -> Unit
) {
    com.example.tabelahisabapp.ui.expense.ExpenseListContent(
        onAddExpense = onAddExpense,
        onEditExpense = onEditExpense
    )
}

