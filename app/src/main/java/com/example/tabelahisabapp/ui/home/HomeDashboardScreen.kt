package com.example.tabelahisabapp.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.customer.CustomerListViewModel
import com.example.tabelahisabapp.ui.daily.DailySummaryViewModel
import com.example.tabelahisabapp.ui.theme.*
import com.example.tabelahisabapp.ui.trading.TradingViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.tabelahisabapp.R
import java.text.SimpleDateFormat
import java.util.*

// Premium color palette for the redesigned home
private val CardGradientStart = Color(0xFFFFFFFF)
private val CardGradientEnd = Color(0xFFF8FAFC)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentGreen = Color(0xFF10B981)
private val AccentRed = Color(0xFFEF4444)
private val AccentOrange = Color(0xFFF97316)
private val AccentPurple = Color(0xFF8B5CF6)
private val SoftGreen = Color(0xFFD1FAE5)
private val SoftRed = Color(0xFFFEE2E2)
private val SoftBlue = Color(0xFFDBEAFE)
private val SoftPurple = Color(0xFFEDE9FE)
private val ChartLineColor = Color(0xFF6366F1)
private val ChartFillStart = Color(0x406366F1)
private val ChartFillEnd = Color(0x006366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    customerViewModel: CustomerListViewModel = hiltViewModel(),
    dailyViewModel: DailySummaryViewModel = hiltViewModel(),
    tradingViewModel: TradingViewModel = hiltViewModel(),
    onNavigateToCustomers: (String?) -> Unit,
    onNavigateToDaily: () -> Unit,
    onNavigateToTrading: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onVoiceClick: () -> Unit = {}
) {
    val customers by customerViewModel.customers.collectAsState()
    val todayBalance by dailyViewModel.todayBalance.collectAsState()
    val todayLedgerTransactions by dailyViewModel.todayLedgerTransactions.collectAsState()
    val thisMonthProfit by tradingViewModel.thisMonthProfit.collectAsState()
    val overallProfit by tradingViewModel.overallProfit.collectAsState()
    val allTrades by tradingViewModel.allTrades.collectAsState()
    val allDailyBalances by dailyViewModel.recentBalances.collectAsState()

    // Filter dropdown state
    var selectedFilter by remember { mutableStateOf("All Time") }
    var expanded by remember { mutableStateOf(false) }

    // Calculate financial metrics
    val totalCustomers = customers.count { it.customer.type == "CUSTOMER" || it.customer.type == "BOTH" }
    val totalSellers = customers.count { it.customer.type == "SELLER" || it.customer.type == "BOTH" }
    
    // Customers with outstanding (they owe us)
    val customersWithOutstanding = customers
        .filter { (it.customer.type == "CUSTOMER" || it.customer.type == "BOTH") && it.balance > 0 }
    val totalOutstanding = customersWithOutstanding.sumOf { it.balance }
    val outstandingCustomerCount = customersWithOutstanding.size
    
    // Seller payment pending (we owe them) - negative balance means we owe seller
    val sellersWithPending = customers
        .filter { (it.customer.type == "SELLER" || it.customer.type == "BOTH") && it.balance < 0 }
    val sellerPaymentPending = sellersWithPending.sumOf { kotlin.math.abs(it.balance) }
    val pendingSellerCount = sellersWithPending.size

    // Today's closing balances - LIVE calculation from ledger transactions
    val openingCash = todayBalance?.openingCash ?: 0.0
    val openingBank = todayBalance?.openingBank ?: 0.0
    
    // Calculate live totals from ledger transactions
    val (liveCash, liveBank) = remember(todayLedgerTransactions, openingCash, openingBank) {
        dailyViewModel.calculateBalancesFromTransactions(openingCash, openingBank, todayLedgerTransactions)
    }
    
    val todayCash = liveCash
    val todayBank = liveBank
    val totalAvailable = todayCash + todayBank

    // Calculate income/expense based on filter
    val todayDate = dailyViewModel.getTodayDateNormalized()
    val calendar = Calendar.getInstance()
    val thisMonthStart = calendar.apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val filteredBalances = when (selectedFilter) {
        "Today" -> allDailyBalances.filter { it.date == todayDate }
        "This Month" -> allDailyBalances.filter { it.date >= thisMonthStart }
        else -> allDailyBalances
    }

    val filteredTrades = when (selectedFilter) {
        "Today" -> allTrades.filter { it.date == todayDate }
        "This Month" -> allTrades.filter { it.date >= thisMonthStart }
        else -> allTrades
    }

    val totalIncome = filteredTrades.sumOf { it.totalAmount }
    val totalExpenses = filteredBalances.sumOf { balance ->
        (balance.openingCash - balance.closingCash).coerceAtLeast(0.0)
    }
    val netProfit = totalIncome - totalExpenses

    // Trading profits
    val monthProfit = thisMonthProfit?.let { it.totalSell - it.totalBuy } ?: 0.0
    val overallTradingProfit = overallProfit?.let { it.totalSell - it.totalBuy } ?: 0.0

    // Net Worth calculation for graph
    // Net Worth = Cash + Bank + Customer Outstanding - Seller Payable
    val netWorthDataPoints = remember(allDailyBalances, customers) {
        allDailyBalances
            .sortedBy { it.date }
            .takeLast(12) // Last 12 entries
            .map { balance ->
                val cash = balance.closingCash
                val bank = balance.closingBank
                cash + bank + totalOutstanding - sellerPaymentPending
            }
    }

    Scaffold(
        containerColor = Color(0xFFF1F5F9), // Slate-100 background
        floatingActionButton = {
            GradientFAB(
                onClick = onVoiceClick,
                icon = Icons.Default.Mic,
                contentDescription = "Voice Entry"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Modern gradient header with glassmorphism effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667EEA),
                                Color(0xFF764BA2)
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App Logo
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.app_logo),
                                    contentDescription = "App Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column {
                                Text(
                                    text = "Qureshi Khata Book",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ═══════════════════════════════════════════════════════════
                // SECTION 1: Today's Closing Position (Most Important)
                // ═══════════════════════════════════════════════════════════
                PremiumCard(
                    modifier = Modifier.fillMaxWidth(),
                    gradient = Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF334155))
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Today's Closing Balance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGreen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Cash | Bank | Total Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Cash in Hand
                            Column(horizontalAlignment = Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF94A3B8), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Cash in Hand",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(todayCash),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(50.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            // Bank Balance
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(AccentBlue, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bank Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(todayBank),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentBlue
                                )
                            }

                            // Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(50.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                            )

                            // Total Available
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(AccentGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Total Available",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(totalAvailable),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AccentGreen
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // SECTION 2: Money Given vs Money to Pay (Side by Side)
                // ═══════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Outstanding (Receivable) - GREEN
                    MoneyFlowCard(
                        modifier = Modifier.weight(1f),
                        title = "Customer Outstanding",
                        subtitle = "$outstandingCustomerCount customers",
                        amount = totalOutstanding,
                        icon = Icons.Outlined.CallReceived,
                        backgroundColor = SoftGreen,
                        accentColor = AccentGreen,
                        iconBackgroundColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        onClick = { onNavigateToCustomers("CUSTOMER_WITH_BALANCE") }
                    )

                    // Seller Payment Pending (Payable) - RED/ORANGE
                    MoneyFlowCard(
                        modifier = Modifier.weight(1f),
                        title = "Seller Pending",
                        subtitle = "$pendingSellerCount sellers",
                        amount = sellerPaymentPending,
                        icon = Icons.Outlined.CallMade,
                        backgroundColor = SoftRed,
                        accentColor = AccentOrange,
                        iconBackgroundColor = Color(0xFFF97316).copy(alpha = 0.15f),
                        onClick = { onNavigateToCustomers("SELLER_WITH_BALANCE") }
                    )
                }

                // ═══════════════════════════════════════════════════════════
                // SECTION 3: Money Distribution
                // ═══════════════════════════════════════════════════════════
                PremiumWhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Money Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Icon(
                                Icons.Outlined.PieChart,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val totalMoney = todayCash + todayBank + totalOutstanding
                        val cashPercent = if (totalMoney > 0) (todayCash / totalMoney * 100) else 0.0
                        val bankPercent = if (totalMoney > 0) (todayBank / totalMoney * 100) else 0.0
                        val customerPercent = if (totalMoney > 0) (totalOutstanding / totalMoney * 100) else 0.0

                        // Distribution Bars
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            DistributionItem(
                                label = "Cash",
                                amount = todayCash,
                                percentage = cashPercent,
                                color = Color(0xFF64748B)
                            )
                            DistributionItem(
                                label = "Bank",
                                amount = todayBank,
                                percentage = bankPercent,
                                color = AccentBlue
                            )
                            DistributionItem(
                                label = "With Customers",
                                amount = totalOutstanding,
                                percentage = customerPercent,
                                color = AccentGreen
                            )
                            DistributionItem(
                                label = "Pending to Sellers",
                                amount = -sellerPaymentPending,
                                percentage = 0.0,
                                color = AccentRed,
                                isNegative = true
                            )
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // SECTION 4: Business Growth Graph
                // ═══════════════════════════════════════════════════════════
                PremiumWhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Net Worth Growth",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Cash + Bank + Receivable - Payable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }

                            // Trend indicator
                            val currentNetWorth = todayCash + todayBank + totalOutstanding - sellerPaymentPending
                            val trendUp = netWorthDataPoints.lastOrNull()?.let { it >= (netWorthDataPoints.getOrNull(netWorthDataPoints.size - 2) ?: 0.0) } ?: true

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        if (trendUp) AccentGreen.copy(alpha = 0.1f) else AccentRed.copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    if (trendUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (trendUp) AccentGreen else AccentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (trendUp) "Growing" else "Declining",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (trendUp) AccentGreen else AccentRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Simple Line Chart
                        if (netWorthDataPoints.isNotEmpty()) {
                            NetWorthChart(
                                dataPoints = netWorthDataPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No data yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // SECTION 5: Income vs Expense Summary
                // ═══════════════════════════════════════════════════════════
                PremiumWhiteCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Financial Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            // Filter Dropdown
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = TextSecondary
                                    )
                                ) {
                                    Text(
                                        text = selectedFilter,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    listOf("Today", "This Month", "All Time").forEach { filter ->
                                        DropdownMenuItem(
                                            text = { Text(filter) },
                                            onClick = {
                                                selectedFilter = filter
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryItem(
                                label = "Total Income",
                                amount = totalIncome,
                                color = AccentGreen,
                                icon = Icons.Outlined.ArrowDownward
                            )
                            SummaryItem(
                                label = "Total Expense",
                                amount = totalExpenses,
                                color = AccentRed,
                                icon = Icons.Outlined.ArrowUpward
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Net Profit Section
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (netProfit >= 0) SoftGreen else SoftRed,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Net Profit/Loss",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (netProfit >= 0) AccentGreen else AccentRed
                                )
                                Text(
                                    text = formatCurrency(netProfit),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netProfit >= 0) AccentGreen else AccentRed
                                )
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════════
                // SECTION 6: Trading & Profit Snapshot
                // ═══════════════════════════════════════════════════════════
                PremiumWhiteCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateToTrading
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(SoftPurple, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.ShowChart,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Trading Performance",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Tap to view details",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Divider(color = BorderGray.copy(alpha = 0.5f))

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "This Month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(monthProfit),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (monthProfit >= 0) AccentGreen else AccentRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Overall",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatCurrency(overallTradingProfit),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (overallTradingProfit >= 0) AccentGreen else AccentRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER COMPOSABLES
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PremiumCard(
    modifier: Modifier = Modifier,
    gradient: Brush,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(12.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
        ) {
            content()
        }
    }
}

@Composable
private fun PremiumWhiteCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }

    Card(
        modifier = cardModifier
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        content()
    }
}

@Composable
private fun MoneyFlowCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    amount: Double,
    icon: ImageVector,
    backgroundColor: Color,
    accentColor: Color,
    iconBackgroundColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBackgroundColor, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatCurrency(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun DistributionItem(
    label: String,
    amount: Double,
    percentage: Double,
    color: Color,
    isNegative: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            Text(
                text = if (isNegative) "-${formatCurrency(-amount)}" else formatCurrency(amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }

        if (!isNegative && percentage > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat())
                        .height(6.dp)
                        .background(color, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector
) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
private fun NetWorthChart(
    dataPoints: List<Double>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) return

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(dataPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic)
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 8.dp.toPx()

        val maxValue = dataPoints.maxOrNull() ?: 1.0
        val minValue = dataPoints.minOrNull() ?: 0.0
        val range = if (maxValue == minValue) 1.0 else maxValue - minValue

        val stepX = (width - 2 * padding) / (dataPoints.size - 1).coerceAtLeast(1)

        val points = dataPoints.mapIndexed { index, value ->
            val x = padding + index * stepX
            val y = height - padding - ((value - minValue) / range * (height - 2 * padding)).toFloat()
            Offset(x, y * animatedProgress.value + (height - padding) * (1 - animatedProgress.value))
        }

        // Draw gradient fill
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, height - padding)
                points.forEach { point ->
                    lineTo(point.x, point.y)
                }
                lineTo(points.last().x, height - padding)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(ChartFillStart, ChartFillEnd)
                )
            )
        }

        // Draw line
        if (points.size >= 2) {
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = ChartLineColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw points
        points.forEach { point ->
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = ChartLineColor,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    val formatted = when {
        absAmount >= 10000000 -> String.format("%.2f Cr", absAmount / 10000000)
        absAmount >= 100000 -> String.format("%.2f L", absAmount / 100000)
        else -> String.format("%.0f", absAmount)
    }
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
