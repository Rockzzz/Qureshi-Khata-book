package com.example.tabelahisabapp.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.DailyExpense
import com.example.tabelahisabapp.data.db.entity.ExpenseCategory
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Sealed class for expense list items (must be at package level, not inside a function)
sealed class ExpenseListItem {
    data class DateHeader(val date: String) : ExpenseListItem()
    data class ExpenseItem(val expense: DailyExpense) : ExpenseListItem()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExpenseListContent(
    viewModel: ExpenseViewModel = hiltViewModel(),
    onAddExpense: () -> Unit,
    onEditExpense: (Int) -> Unit
) {
    val expenses: List<DailyExpense> by viewModel.expenses.collectAsState()
    val categories: List<ExpenseCategory> by viewModel.categories.collectAsState()
    
    // Calculate Summary
    val calendar = Calendar.getInstance()
    // Today
    val startOfToday = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    // Month
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = calendar.timeInMillis
    
    val todayTotal = expenses.filter { it.date >= startOfToday }.sumOf { it.amount }
    val monthTotal = expenses.filter { it.date >= startOfMonth }.sumOf { it.amount }
    
    // Group by Date for Ledger View
    val groupedExpenses = expenses.sortedByDescending { it.date }.groupBy { 
       SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.date))
    }
    
    // Helper to get Icon
    fun getCategoryIcon(iconName: String): ImageVector {
        return try {
            val field = Icons.Filled::class.java.getDeclaredField(iconName)
            field.get(null) as ImageVector
        } catch (e: Exception) {
            Icons.Default.Receipt // Fallback
        }
    }
    
    // Helper to get Category Color
    fun getCategoryColor(catName: String): Color {
        val cat = categories.find { it.name == catName }
        return if (cat != null) Color(android.graphics.Color.parseColor(cat.colorHex)) else Color.Gray
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
        ) {
            // Header Summary Card (Khata Style)
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                 Column(modifier = Modifier.padding(16.dp)) {
                     Text(
                         text = "TOTAL EXPENSES",
                         style = MaterialTheme.typography.labelMedium,
                         color = TextSecondary,
                         fontWeight = FontWeight.Bold
                     )
                     Spacer(modifier = Modifier.height(8.dp))
                     Row(modifier = Modifier.fillMaxWidth()) {
                         // Today
                         Column(modifier = Modifier.weight(1f)) {
                             Text(
                                 text = "Today",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = TextSecondary
                             )
                             Text(
                                 text = "₹${String.format("%.0f", todayTotal)}",
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.Bold,
                                 color = AccentRed
                             )
                         }
                         // Month
                         Column(modifier = Modifier.weight(1f)) {
                             Text(
                                 text = "This Month",
                                 style = MaterialTheme.typography.bodySmall,
                                 color = TextSecondary
                             )
                             Text(
                                 text = "₹${String.format("%.0f", monthTotal)}",
                                 style = MaterialTheme.typography.titleLarge,
                                 fontWeight = FontWeight.Bold,
                                 color = AccentOrange // Orange for month warning
                             )
                         }
                     }
                 }
            }
            
            // Transaction List
            // Create a flat list that alternates between date headers and expenses
            
            val flatList = remember(groupedExpenses) {
                groupedExpenses.flatMap { (date, txs) ->
                    listOf(ExpenseListItem.DateHeader(date)) + txs.map { ExpenseListItem.ExpenseItem(it) }
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = flatList,
                    key = { item -> when (item) {
                        is ExpenseListItem.DateHeader -> "header_${item.date}"
                        is ExpenseListItem.ExpenseItem -> "expense_${item.expense.id}"
                    } }
                ) { item ->
                    when (item) {
                        is ExpenseListItem.DateHeader -> {
                            Surface(
                                color = BackgroundGray,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                 Box(
                                     modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Surface(
                                         shape = RoundedCornerShape(12.dp),
                                         color = Color.White,
                                         border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
                                     ) {
                                         Text(
                                             text = item.date,
                                             modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                             style = MaterialTheme.typography.bodySmall,
                                             color = TextSecondary
                                         )
                                     }
                                 }
                            }
                        }
                        is ExpenseListItem.ExpenseItem -> {
                            val expense = item.expense
                            var showMenu by remember { mutableStateOf(false) }
                            var showDeleteDialog by remember { mutableStateOf(false) }
                            
                            val category = categories.find { it.name == expense.category }
                            val iconName = category?.iconName ?: "Receipt"
                            val colorHex = category?.colorHex ?: "#808080"
                            val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e:Exception) { Color.Gray }
                            
                            Surface(
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = {}, // No regular click
                                        onLongClick = { showMenu = true }
                                    ),
                                shape = RoundedCornerShape(8.dp),
                                shadowElevation = 1.dp
                            ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon
                                Surface(
                                    shape = CircleShape,
                                    color = color.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = getCategoryIcon(iconName),
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = expense.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!expense.note.isNullOrBlank()) {
                                        Text(
                                            text = expense.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                                
                                // Amount
                                Text(
                                    text = "₹${String.format("%.0f", expense.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed 
                                )
                            }
                            }
                            
                            // Bottom Sheet Menu
                            if (showMenu) {
                                ModalBottomSheet(
                                    onDismissRequest = { showMenu = false },
                                    containerColor = Color.White
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 32.dp)
                                    ) {
                                        // Edit Option
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showMenu = false
                                                    onEditExpense(expense.id)
                                                }
                                                .padding(horizontal = 24.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = InfoBlue
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                "✏️ Edit Expense",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        
                                        Divider()
                                        
                                        // Delete Option
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    showMenu = false
                                                    showDeleteDialog = true
                                                }
                                                .padding(horizontal = 24.dp, vertical = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = DangerRed
                                            )
                                            Spacer(Modifier.width(16.dp))
                                            Text(
                                                "🗑️ Delete Expense",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DangerRed
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Delete Confirmation Dialog
                            if (showDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showDeleteDialog = false },
                                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
                                    title = { Text("Delete Expense") },
                                    text = { Text("Are you sure you want to delete this expense? This will also remove it from the daily ledger.") },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                viewModel.deleteExpense(expense)
                                                showDeleteDialog = false
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                                        ) {
                                            Text("Delete")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showDeleteDialog = false }) {
                                            Text("Cancel")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // FAB (Fixed at bottom right)
        FloatingActionButton(
            onClick = onAddExpense,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = AccentRed,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
        }
    }
}
