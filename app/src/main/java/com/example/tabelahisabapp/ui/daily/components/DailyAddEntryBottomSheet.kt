package com.example.tabelahisabapp.ui.daily.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Entry types that can be added from the Daily screen
 */
sealed class DailyEntryType(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
) {
    data object CustomerReceived : DailyEntryType(
        title = "Money Received",
        subtitle = "Customer paid you",
        icon = Icons.Default.TrendingUp,
        color = Color(0xFF22C55E) // Green
    )
    
    data object CustomerGiven : DailyEntryType(
        title = "Money Given",
        subtitle = "You gave udhaar",
        icon = Icons.Default.TrendingDown,
        color = Color(0xFFEF4444) // Red
    )
    
    data object SupplierPurchase : DailyEntryType(
        title = "Purchase",
        subtitle = "Bought from supplier (no cash)",
        icon = Icons.Default.ShoppingCart,
        color = Color(0xFFF59E0B) // Amber
    )
    
    data object SupplierPayment : DailyEntryType(
        title = "Supplier Payment",
        subtitle = "Paid to supplier",
        icon = Icons.Default.Payment,
        color = Color(0xFF3B82F6) // Blue
    )
    
    data object Expense : DailyEntryType(
        title = "Expense",
        subtitle = "Record an expense",
        icon = Icons.Default.Receipt,
        color = Color(0xFF8B5CF6) // Purple
    )
}

/**
 * Bottom sheet for adding entries from the Daily screen
 * 
 * This is the entry point for bidirectional sync:
 * - User selects entry type → Opens appropriate form with locked date
 * - On save → Entry created in BOTH source ledger AND Daily ledger
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAddEntryBottomSheet(
    selectedDate: Long,
    onDismiss: () -> Unit,
    onSelectEntryType: (DailyEntryType) -> Unit
) {
    val extendedColors = AppTheme.colors
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, EEEE", Locale.getDefault()) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = extendedColors.cardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Add Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = extendedColors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateFormat.format(Date(selectedDate)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = extendedColors.textSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Purple600.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Date Locked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Purple600,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(color = BorderGray)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Section: Customer Transactions
            SectionHeader("Customer Transactions")
            
            EntryTypeRow(
                entryType = DailyEntryType.CustomerReceived,
                onClick = { onSelectEntryType(DailyEntryType.CustomerReceived) }
            )
            
            EntryTypeRow(
                entryType = DailyEntryType.CustomerGiven,
                onClick = { onSelectEntryType(DailyEntryType.CustomerGiven) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Section: Supplier Transactions
            SectionHeader("Supplier Transactions")
            
            EntryTypeRow(
                entryType = DailyEntryType.SupplierPurchase,
                onClick = { onSelectEntryType(DailyEntryType.SupplierPurchase) }
            )
            
            EntryTypeRow(
                entryType = DailyEntryType.SupplierPayment,
                onClick = { onSelectEntryType(DailyEntryType.SupplierPayment) }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Section: Expenses
            SectionHeader("Expenses")
            
            EntryTypeRow(
                entryType = DailyEntryType.Expense,
                onClick = { onSelectEntryType(DailyEntryType.Expense) }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val extendedColors = AppTheme.colors
    
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = extendedColors.textSecondary,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}

@Composable
private fun EntryTypeRow(
    entryType: DailyEntryType,
    onClick: () -> Unit
) {
    val extendedColors = AppTheme.colors
    
    ListItem(
        headlineContent = {
            Text(
                text = entryType.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = extendedColors.textPrimary
            )
        },
        supportingContent = {
            Text(
                text = entryType.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = extendedColors.textSecondary
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(entryType.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    entryType.icon,
                    contentDescription = null,
                    tint = entryType.color,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = extendedColors.textTertiary
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    )
}

/**
 * Compact quick-add buttons for inline use
 */
@Composable
fun QuickAddButtons(
    onAddCustomerReceived: () -> Unit,
    onAddCustomerGiven: () -> Unit,
    onAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = AppTheme.colors
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickAddButton(
            icon = Icons.Default.TrendingUp,
            label = "Received",
            color = Color(0xFF22C55E),
            onClick = onAddCustomerReceived,
            modifier = Modifier.weight(1f)
        )
        
        QuickAddButton(
            icon = Icons.Default.TrendingDown,
            label = "Given",
            color = Color(0xFFEF4444),
            onClick = onAddCustomerGiven,
            modifier = Modifier.weight(1f)
        )
        
        QuickAddButton(
            icon = Icons.Default.Receipt,
            label = "Expense",
            color = Color(0xFF8B5CF6),
            onClick = onAddExpense,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickAddButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = AppTheme.colors
    
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
