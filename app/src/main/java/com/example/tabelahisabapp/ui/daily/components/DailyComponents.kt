package com.example.tabelahisabapp.ui.daily.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Horizontal date selector for navigating through days
 * Shows last 7 days by default with option to pick any date
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateNavigator(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = AppTheme.colors
    val today = remember { normalizeToMidnight(System.currentTimeMillis()) }
    
    // Generate list of recent days (today + last 6 days)
    val recentDays = remember {
        (0 until 7).map { daysAgo ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = today
            calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
            normalizeToMidnight(calendar.timeInMillis)
        }
    }
    
    val listState = rememberLazyListState()
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    val monthFormat = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val selectedNormalized = remember(selectedDate) { normalizeToMidnight(selectedDate) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header with month/year and calendar button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val selectedCalendar = remember(selectedDate) {
                    Calendar.getInstance().apply { timeInMillis = selectedDate }
                }
                val fullMonthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                
                Text(
                    text = fullMonthFormat.format(Date(selectedDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
                
                IconButton(
                    onClick = onCalendarClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Purple600.copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Open calendar",
                        tint = Purple600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Date chips row
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(recentDays) { dayMillis ->
                    val isSelected = dayMillis == selectedNormalized
                    val isToday = dayMillis == today
                    
                    DateChip(
                        dayOfWeek = dayFormat.format(Date(dayMillis)),
                        dayOfMonth = dateFormat.format(Date(dayMillis)),
                        isSelected = isSelected,
                        isToday = isToday,
                        onClick = { onDateSelected(dayMillis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(
    dayOfWeek: String,
    dayOfMonth: String,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = AppTheme.colors
    val surfaceColor = extendedColors.cardBackground
    
    val backgroundColor = when {
        isSelected -> Brush.linearGradient(listOf(Purple600, Purple700))
        isToday -> Brush.linearGradient(listOf(Purple600.copy(alpha = 0.2f), Purple600.copy(alpha = 0.2f)))
        else -> Brush.linearGradient(listOf(surfaceColor, surfaceColor))
    }
    
    val textColor = when {
        isSelected -> Color.White
        isToday -> Purple600
        else -> extendedColors.textSecondary
    }
    
    Box(
        modifier = Modifier
            .width(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayOfWeek,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = if (isSelected || isToday) FontWeight.Medium else FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayOfMonth,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            if (isToday && !isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Purple600)
                )
            }
        }
    }
}

/**
 * Daily summary card showing totals for the selected date
 * Opening balances are clickable to allow editing
 */
@Composable
fun DailySummaryCard(
    date: Long,
    openingCash: Double,
    openingBank: Double,
    closingCash: Double,
    closingBank: Double,
    cashIn: Double,
    cashOut: Double,
    bankIn: Double,
    bankOut: Double,
    modifier: Modifier = Modifier,
    onEditOpeningBalance: (() -> Unit)? = null
) {
    val extendedColors = AppTheme.colors
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Opening Balances (Clickable to edit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onEditOpeningBalance != null) {
                            Modifier.clickable { onEditOpeningBalance() }
                        } else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                BalanceItem(
                    label = "Opening Cash",
                    amount = openingCash,
                    color = extendedColors.textSecondary
                )
                Column(horizontalAlignment = Alignment.End) {
                    BalanceItem(
                        label = "Opening Bank",
                        amount = openingBank,
                        color = extendedColors.textSecondary,
                        alignment = Alignment.End
                    )
                    if (onEditOpeningBalance != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Purple600,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap to edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Purple600,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = BorderGray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Money Flow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FlowItem(
                    label = "Cash In",
                    amount = cashIn,
                    color = Color(0xFF22C55E),
                    icon = Icons.Default.TrendingUp
                )
                FlowItem(
                    label = "Cash Out",
                    amount = cashOut,
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.TrendingDown
                )
                FlowItem(
                    label = "Bank In",
                    amount = bankIn,
                    color = Color(0xFF22C55E),
                    icon = Icons.Default.AccountBalance
                )
                FlowItem(
                    label = "Bank Out",
                    amount = bankOut,
                    color = Color(0xFFEF4444),
                    icon = Icons.Default.AccountBalance
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            HorizontalDivider(color = BorderGray)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Closing Balances
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BalanceItem(
                    label = "Closing Cash",
                    amount = closingCash,
                    color = if (closingCash >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                    isBold = true
                )
                BalanceItem(
                    label = "Closing Bank",
                    amount = closingBank,
                    color = if (closingBank >= 0) Color(0xFF22C55E) else Color(0xFFEF4444),
                    alignment = Alignment.End,
                    isBold = true
                )
            }
        }
    }
}

@Composable
private fun BalanceItem(
    label: String,
    amount: Double,
    color: Color,
    alignment: Alignment.Horizontal = Alignment.Start,
    isBold: Boolean = false
) {
    Column(horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary
        )
        Text(
            text = "₹${String.format("%,.0f", amount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun FlowItem(
    label: String,
    amount: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "₹${String.format("%,.0f", amount)}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textSecondary
        )
    }
}

/**
 * Source tag showing where a transaction originated
 */
@Composable
fun SourceTag(
    source: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = source,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

private fun normalizeToMidnight(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
