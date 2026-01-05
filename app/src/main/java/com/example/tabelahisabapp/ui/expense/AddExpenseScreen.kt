package com.example.tabelahisabapp.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.DailyExpense
import com.example.tabelahisabapp.data.db.entity.ExpenseCategory
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Expense category configuration
data class ExpenseCategoryConfig(
    val name: String,
    val icon: ImageVector,
    val color: Color
)

val expenseCategories = listOf(
    ExpenseCategoryConfig("Fuel", Icons.Default.LocalGasStation, Color(0xFFEF4444)),
    ExpenseCategoryConfig("Labour", Icons.Default.Group, Color(0xFF3B82F6)),
    ExpenseCategoryConfig("Electricity", Icons.Default.Bolt, Color(0xFFF59E0B)),
    ExpenseCategoryConfig("Home Expense", Icons.Default.Home, Color(0xFF8B5CF6)),
    ExpenseCategoryConfig("Transport", Icons.Default.DirectionsCar, Color(0xFF10B981)),
    ExpenseCategoryConfig("Maintenance", Icons.Default.Build, Color(0xFF6366F1)),
    ExpenseCategoryConfig("Feed", Icons.Default.Restaurant, Color(0xFF22D3EE)),
    ExpenseCategoryConfig("Medical", Icons.Default.MedicalServices, Color(0xFFEC4899)),
    ExpenseCategoryConfig("Misc", Icons.Default.MoreHoriz, Color(0xFF71717A))
)

/**
 * Add Expense Screen - For adding new expenses
 * 
 * Key Concept:
 * - Expenses are money GONE forever (no ledger, no recovery)
 * - Creates entry in DailyExpense table
 * - Also creates CASH_OUT/BANK_OUT in DailyLedgerTransaction for sync
 * 
 * @param lockedDate If provided, the date is locked (used when adding from Daily screen)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel = hiltViewModel(),
    editExpenseId: Int? = null,
    lockedDate: Long? = null,
    onExpenseSaved: () -> Unit,
    onCancel: () -> Unit
) {
    // Combine DB categories with "Add New" option logic
    val categories by viewModel.categories.collectAsState()
    val allExpenses by viewModel.expenses.collectAsState()
    
    var selectedCategory by remember { mutableStateOf<ExpenseCategoryConfig?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var selectedDate by remember { mutableStateOf(lockedDate ?: System.currentTimeMillis()) }
    val isDateLocked = lockedDate != null
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Get theme colors
    val extendedColors = AppTheme.colors
    val isDark = AppTheme.isDark
    
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    // Load existing expense data when editing
    LaunchedEffect(editExpenseId, allExpenses, categories) {
        if (editExpenseId != null && allExpenses.isNotEmpty() && categories.isNotEmpty()) {
            val existingExpense = allExpenses.find { it.id == editExpenseId }
            if (existingExpense != null) {
                amount = existingExpense.amount.toInt().toString()
                note = existingExpense.note ?: ""
                selectedPaymentMethod = existingExpense.paymentMethod
                if (lockedDate == null) {
                    selectedDate = existingExpense.date
                }
                // Find matching category
                val matchingCategory = categories.find { it.name == existingExpense.category }
                if (matchingCategory != null) {
                    selectedCategory = ExpenseCategoryConfig(
                        name = matchingCategory.name,
                        icon = getIconByName(matchingCategory.iconName ?: "Notes"),
                        color = Color(android.graphics.Color.parseColor(matchingCategory.colorHex ?: "#71717A"))
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = if (editExpenseId != null) "Edit Expense" else "Add Expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        containerColor = if (isDark) DarkBackground else BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Step 1: Select Category
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween,
                 verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(280.dp)
            ) {
                // DB Categories
                items(
                    items = categories as List<ExpenseCategory>,
                    key = { it.id }
                ) { categoryEntity ->
                    val config = ExpenseCategoryConfig(
                        name = categoryEntity.name,
                        icon = getIconByName(categoryEntity.iconName ?: "Notes"),
                        color = Color(android.graphics.Color.parseColor(categoryEntity.colorHex ?: "#71717A"))
                    )
                    
                    CategoryCard(
                        category = config,
                        isSelected = selectedCategory?.name == categoryEntity.name,
                        onClick = { selectedCategory = config }
                    )
                }
                
                // Add New Button
                item {
                    AddCategoryCard(onClick = { showAddCategoryDialog = true })
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Step 2: Enter Amount
            Text(
                "Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extendedColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = { Text("Enter amount") },
                leadingIcon = { 
                    Text(
                        "₹",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = selectedCategory?.color ?: Purple600
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Payment Method Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PaymentMethodButton(
                    label = "Cash",
                    icon = Icons.Default.Money,
                    isSelected = selectedPaymentMethod == "CASH",
                    onClick = { selectedPaymentMethod = "CASH" },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    label = "Bank",
                    icon = Icons.Default.AccountBalance,
                    isSelected = selectedPaymentMethod == "BANK",
                    onClick = { selectedPaymentMethod = "BANK" },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date Selector
            OutlinedCard(
                onClick = { if (!isDateLocked) showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isDateLocked) 
                        extendedColors.softPurple 
                    else 
                        extendedColors.cardBackground
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDateLocked) Icons.Default.Lock else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (isDateLocked) Purple600 else extendedColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                dateFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyLarge,
                                color = extendedColors.textPrimary
                            )
                            if (isDateLocked) {
                                Text(
                                    "Date locked from Daily entry",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Purple600
                                )
                            }
                        }
                    }
                    if (!isDateLocked) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = extendedColors.textSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note (optional)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add note (optional)") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        tint = extendedColors.textSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = selectedCategory?.color ?: Purple600
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = {
                    if (selectedCategory != null && amount.isNotBlank()) {
                        isLoading = true
                        if (editExpenseId != null) {
                            // Update existing expense
                            viewModel.updateExpense(
                                expenseId = editExpenseId,
                                category = selectedCategory!!.name,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                date = selectedDate,
                                paymentMethod = selectedPaymentMethod,
                                note = note.ifBlank { null }
                            )
                        } else {
                            // Create new expense
                            viewModel.saveExpense(
                                category = selectedCategory!!.name,
                                amount = amount.toDoubleOrNull() ?: 0.0,
                                date = selectedDate,
                                paymentMethod = selectedPaymentMethod,
                                note = note.ifBlank { null }
                            )
                        }
                        onExpenseSaved()
                    }
                },
                enabled = selectedCategory != null && amount.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = selectedCategory?.color ?: AccentRed,
                    disabledContainerColor = BorderGray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Expense",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        // Simple color selection (random for now or fixed palette)
        // Check if we can add color picker later
        
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            // Assign a random color from a nice palette
                            val colors = listOf("#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#6366F1", "#EC4899", "#8B5CF6")
                            val randomColor = colors.random()
                            viewModel.addNewCategory(newCategoryName, randomColor, "Notes")
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AddCategoryCard(onClick: () -> Unit) {
    val extendedColors = AppTheme.colors
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp, brush = Brush.linearGradient(listOf(BorderGray, BorderGray)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BorderGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = extendedColors.textSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add New",
                style = MaterialTheme.typography.labelSmall,
                color = extendedColors.textSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Icon helper
fun getIconByName(name: String): ImageVector {
    return when(name) {
        "LocalGasStation" -> Icons.Default.LocalGasStation
        "Group" -> Icons.Default.Group
        "Bolt" -> Icons.Default.Bolt
        "Home" -> Icons.Default.Home
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "Build" -> Icons.Default.Build
        "Restaurant" -> Icons.Default.Restaurant
        "MedicalServices" -> Icons.Default.MedicalServices
        "MoreHoriz" -> Icons.Default.MoreHoriz
        "Notes" -> Icons.Default.Notes
        else -> Icons.Default.Category // Fallback
    }
}

/**
 * Category card for selection grid
 */
@Composable
private fun CategoryCard(
    category: ExpenseCategoryConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = AppTheme.colors
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                category.color.copy(alpha = 0.15f) 
            else 
                extendedColors.cardBackground
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = Brush.linearGradient(listOf(category.color, category.color))
            )
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) category.color 
                        else category.color.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    category.icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else category.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                category.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) category.color else extendedColors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

/**
 * Payment method toggle button
 */
@Composable
private fun PaymentMethodButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = AppTheme.colors
    
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) 
                Purple600.copy(alpha = 0.1f) 
            else 
                extendedColors.cardBackground
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isSelected) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                if (isSelected) listOf(Purple600, Purple600)
                else listOf(BorderGray, BorderGray)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Purple600 else extendedColors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Purple600 else extendedColors.textSecondary
            )
        }
    }
}
