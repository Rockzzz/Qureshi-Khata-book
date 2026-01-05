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
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    onNavigateToSuppliers: (String?) -> Unit,
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
    val totalCustomers =
        customers.count { it.customer.type == "CUSTOMER" || it.customer.type == "BOTH" }
    val totalSellers =
        customers.count { it.customer.type == "SELLER" || it.customer.type == "BOTH" }

    // Customer outstanding (they owe us) - positive balance means they owe us
    val customersWithOutstanding = customers
        .filter { (it.customer.type == "CUSTOMER" || it.customer.type == "BOTH") && it.balance > 0 }
    val totalOutstanding = customersWithOutstanding.sumOf { it.balance }
    val outstandingCustomerCount = customersWithOutstanding.size

    // Customer payments (we paid them) - negative customer balance
    val customersWithPayments = customers
        .filter { (it.customer.type == "CUSTOMER" || it.customer.type == "BOTH") && it.balance < 0 }
    val customerPayments = customersWithPayments.sumOf { kotlin.math.abs(it.balance) }
    val customerPaymentCount = customersWithPayments.size

    // Supplier payment pending (we owe them) - negative balance means we owe supplier
    val suppliersWithPending = customers
        .filter { (it.customer.type == "SELLER" || it.customer.type == "BOTH") && it.balance < 0 }
    val supplierPaymentPending = suppliersWithPending.sumOf { kotlin.math.abs(it.balance) }
    val pendingSupplierCount = suppliersWithPending.size

    // Supplier advance (they owe us) - positive supplier balance
    val suppliersWithAdvance = customers
        .filter { (it.customer.type == "SELLER" || it.customer.type == "BOTH") && it.balance > 0 }
    val supplierAdvance = suppliersWithAdvance.sumOf { it.balance }
    val supplierAdvanceCount = suppliersWithAdvance.size

    // Today's closing balances - LIVE calculation from ledger transactions
    val openingCash = todayBalance?.openingCash ?: 0.0
    val openingBank = todayBalance?.openingBank ?: 0.0

    // Calculate live totals from ledger transactions
    val (liveCash, liveBank) = remember(todayLedgerTransactions, openingCash, openingBank) {
        dailyViewModel.calculateBalancesFromTransactions(
            openingCash,
            openingBank,
            todayLedgerTransactions
        )
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

    // Trading profits - use totalProfit field which sums actual profit from trades
    val monthProfit = thisMonthProfit?.totalProfit ?: 0.0
    val overallTradingProfit = overallProfit?.totalProfit ?: 0.0

    // Net Worth calculation for graph (includes date and amount)
    // Net Worth = Cash + Bank + Customer Outstanding - Supplier Payable
    val netWorthWithDates = remember(allDailyBalances, customers) {
        allDailyBalances
            .sortedBy { it.date }
            .takeLast(7) // Last 7 days for better readability
            .map { balance ->
                val cash = balance.closingCash
                val bank = balance.closingBank
                Pair(balance.date, cash + bank + totalOutstanding - supplierPaymentPending)
            }
    }
    val netWorthDataPoints = netWorthWithDates.map { it.second }

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
                            // App Logo - Using launcher foreground (same as app drawer)
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(82.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Qureshi Khata Book",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = SimpleDateFormat(
                                        "EEEE, dd MMMM yyyy",
                                        Locale.getDefault()
                                    ).format(Date()),
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

            // Pull-to-refresh state
            val pullRefreshState = rememberPullToRefreshState()
            val coroutineScope = rememberCoroutineScope()
            var isRefreshing by remember { mutableStateOf(false) }

            // Handle refresh trigger
            if (pullRefreshState.isRefreshing) {
                LaunchedEffect(true) {
                    isRefreshing = true
                    // Trigger refresh on all viewmodels
                    customerViewModel.refresh()
                    dailyViewModel.refresh()
                    tradingViewModel.refresh()
                    delay(1000) // Give time for data to load
                    isRefreshing = false
                    pullRefreshState.endRefresh()
                }
            }

            // Scrollable content with pull-to-refresh
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(pullRefreshState.nestedScrollConnection)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                        .background(
                                            Color.White.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
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

                            Spacer(modifier = Modifier.height(14.dp))

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
                    // SECTION 2: Money Flow Cards (2x2 Grid)
                    // ═══════════════════════════════════════════════════════════
                    // Row 1: Customer cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        // Customer Payments (we paid them) - BLUE
                        MoneyFlowCard(
                            modifier = Modifier.weight(1f),
                            title = "Customer Payments",
                            subtitle = "$customerPaymentCount customers",
                            amount = customerPayments,
                            icon = Icons.Outlined.CallMade,
                            backgroundColor = SoftBlue,
                            accentColor = Color(0xFF3B82F6),
                            iconBackgroundColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                            onClick = { onNavigateToCustomers("CUSTOMER_PAYMENTS") }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Row 2: Supplier cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Supplier Payment Pending (Payable) - RED/ORANGE
                        MoneyFlowCard(
                            modifier = Modifier.weight(1f),
                            title = "Supplier Pending",
                            subtitle = "$pendingSupplierCount suppliers",
                            amount = supplierPaymentPending,
                            icon = Icons.Outlined.CallMade,
                            backgroundColor = SoftRed,
                            accentColor = AccentOrange,
                            iconBackgroundColor = Color(0xFFF97316).copy(alpha = 0.15f),
                            onClick = { onNavigateToSuppliers("SUPPLIER_WITH_BALANCE") }
                        )

                        // Supplier Advance (they owe us) - PURPLE
                        MoneyFlowCard(
                            modifier = Modifier.weight(1f),
                            title = "Supplier Advance",
                            subtitle = "$supplierAdvanceCount suppliers",
                            amount = supplierAdvance,
                            icon = Icons.Outlined.CallReceived,
                            backgroundColor = SoftPurple,
                            accentColor = Color(0xFF8B5CF6),
                            iconBackgroundColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            onClick = { onNavigateToSuppliers("SUPPLIER_ADVANCE") }
                        )
                    }

                    // ═══════════════════════════════════════════════════════════
                    // SECTION 3: Money Distribution
                    // ═══════════════════════════════════════════════════════════
                    PremiumWhiteCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Calculate today's expenses from ledger transactions
                            val todayExpenses = remember(todayLedgerTransactions) {
                                todayLedgerTransactions
                                    .filter { it.sourceType == "expense" }
                                    .sumOf { it.amount }
                            }

                            val totalMoney = todayCash + todayBank + totalOutstanding
                            val cashPercent =
                                if (totalMoney > 0) (todayCash / totalMoney * 100) else 0.0
                            val bankPercent =
                                if (totalMoney > 0) (todayBank / totalMoney * 100) else 0.0
                            val customerPercent =
                                if (totalMoney > 0) (totalOutstanding / totalMoney * 100) else 0.0

                            // Distribution Bars
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    label = "Today's Expense",
                                    amount = -todayExpenses,
                                    percentage = 0.0,
                                    color = Color(0xFFEF4444),
                                    isNegative = true
                                )
                                DistributionItem(
                                    label = "Pending to Suppliers",
                                    amount = -supplierPaymentPending,
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
                        Column(modifier = Modifier.padding(16.dp)) {
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
                                val currentNetWorth =
                                    todayCash + todayBank + totalOutstanding - supplierPaymentPending
                                val trendUp = netWorthDataPoints.lastOrNull()?.let {
                                    it >= (netWorthDataPoints.getOrNull(netWorthDataPoints.size - 2)
                                        ?: 0.0)
                                } ?: true

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            if (trendUp) AccentGreen.copy(alpha = 0.1f) else AccentRed.copy(
                                                alpha = 0.1f
                                            ),
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

                            Spacer(modifier = Modifier.height(12.dp))

                            // Day-wise Net Worth List (more informative than chart)
                            if (netWorthWithDates.isNotEmpty()) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                                    netWorthWithDates.forEachIndexed { index, (date, amount) ->
                                        val prevAmount =
                                            if (index > 0) netWorthWithDates[index - 1].second else amount
                                        val change = amount - prevAmount
                                        val isUp = change >= 0

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isUp) AccentGreen.copy(alpha = 0.05f) else AccentRed.copy(
                                                        alpha = 0.05f
                                                    ),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Date
                                            Text(
                                                text = dateFormat.format(Date(date)),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = TextPrimary
                                            )

                                            // Amount and Change
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = formatCurrency(amount),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                if (index > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isUp) "+${formatCurrency(change)}" else formatCurrency(
                                                            change
                                                        ),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isUp) AccentGreen else AccentRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(60.dp),
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
                    // SECTION 6: Trading & Profit Snapshot
                    // ═══════════════════════════════════════════════════════════
                    PremiumWhiteCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToTrading
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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

                            Spacer(modifier = Modifier.height(14.dp))

                            Divider(color = BorderGray.copy(alpha = 0.5f))

                            Spacer(modifier = Modifier.height(12.dp))

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
                
                // Pull-to-refresh indicator - only visible when pulling or refreshing
                if (pullRefreshState.isRefreshing || pullRefreshState.progress > 0) {
                    PullToRefreshContainer(
                        state = pullRefreshState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 100.dp), // Push below the header
                        containerColor = Color.White,
                        contentColor = Color(0xFF667EEA)
                    )
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
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBackgroundColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = formatCurrency(amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                        .fillMaxWidth(
                            fraction = (percentage / 100.0).coerceIn(0.0, 1.0).toFloat()
                        )
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
    // Use Indian number format (lakhs, crores with commas: 1,79,870)
    val formatted = when {
        absAmount >= 10000000 -> {
            // Crores: show as full number with commas
            val crores = absAmount.toLong()
            formatIndianNumber(crores)
        }
        else -> formatIndianNumber(absAmount.toLong())
    }
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}

// Format number with Indian comma system (1,00,000 = 1 lakh)
private fun formatIndianNumber(number: Long): String {
    val numStr = number.toString()
    if (numStr.length <= 3) return numStr

    val result = StringBuilder()
    val len = numStr.length
    var count = 0

    for (i in len - 1 downTo 0) {
        result.insert(0, numStr[i])
        count++
        // First comma after 3 digits, then every 2 digits
        if (i > 0) {
            if (count == 3 || (count > 3 && (count - 3) % 2 == 0)) {
                result.insert(0, ',')
            }
        }
    }
    return result.toString()
}
