package com.example.tabelahisabapp.ui.supplier

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.models.CustomerListResult
import com.example.tabelahisabapp.ui.customer.CustomerListViewModel
import com.example.tabelahisabapp.ui.theme.*

// Supplier avatar color
private val SupplierAvatarColor = Color(0xFF22C55E) // Green

/**
 * Embeddable Supplier List Content - For use inside AccountsScreen tabs
 * Shows only SELLER type contacts
 * 
 * Supplier Transaction Types:
 * - PURCHASE: Creates liability (increases payable), NO cash movement
 * - PAYMENT: Reduces liability, deducts from Cash/Bank
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SupplierListContent(
    viewModel: CustomerListViewModel = hiltViewModel(),
    initialFilter: String? = null, // Filter from home screen navigation
    onAddSupplier: () -> Unit,
    onEditSupplier: (Int) -> Unit,
    onSupplierClick: (Int) -> Unit
) {
    val allContacts by viewModel.customers.collectAsState()
    var supplierToDelete by remember { mutableStateOf<com.example.tabelahisabapp.data.db.entity.Customer?>(null) }
    var selectedSupplier by remember { mutableStateOf<CustomerListResult?>(null) }
    var showActionSheet by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Get theme colors
    val extendedColors = AppTheme.colors
    val isDark = AppTheme.isDark
    
    // Determine active filter based on initialFilter from navigation
    var selectedFilter by remember { 
        mutableStateOf(
            when (initialFilter) {
                "SUPPLIER_WITH_BALANCE" -> "SUPPLIER_PENDING"
                "SUPPLIER_ADVANCE" -> "SUPPLIER_ADVANCE"
                else -> "ALL"
            }
        ) 
    }
    
    // Filter to only show SELLER or BOTH types, with additional balance filter if needed
    val supplierContacts = remember(allContacts, selectedFilter) {
        allContacts.filter { result ->
            // Apply filter based on selected filter type
            when (selectedFilter) {
                "ALL" -> result.customer.type == "SELLER" || result.customer.type == "BOTH"
                "SUPPLIER_PENDING" -> (result.customer.type == "SELLER" || result.customer.type == "BOTH") && result.balance < 0
                "SUPPLIER_ADVANCE" -> (result.customer.type == "SELLER" || result.customer.type == "BOTH") && result.balance > 0
                else -> result.customer.type == "SELLER" || result.customer.type == "BOTH"
            }
        }
    }
    
    // Calculate summary totals for suppliers
    // Negative balance = we owe them (payable)
    // Positive balance = they owe us (rare for suppliers, but possible)
    val totalPayable = supplierContacts
        .filter { it.balance < 0 }
        .sumOf { kotlin.math.abs(it.balance) }
    
    val totalReceivable = supplierContacts
        .filter { it.balance > 0 }
        .sumOf { it.balance }
    
    val netPayable = totalPayable - totalReceivable
    
    val filteredSuppliers = remember(supplierContacts, searchQuery) {
        supplierContacts.filter { result ->
            searchQuery.isBlank() || 
            result.customer.name.contains(searchQuery, ignoreCase = true) ||
            (result.customer.phone?.contains(searchQuery) == true) ||
            (result.customer.businessName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) DarkBackground else BackgroundGray)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary Card - Suppliers
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = extendedColors.cardBackground
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Total Payable (main metric for suppliers)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⬆️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "You need to pay",
                                style = MaterialTheme.typography.bodyMedium,
                                color = extendedColors.textSecondary
                            )
                        }
                        Text(
                            "₹${String.format("%,.0f", totalPayable)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                    }
                    
                    if (totalReceivable > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Receivable (rare case)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⬇️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "You will receive",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = extendedColors.textSecondary
                                )
                            }
                            Text(
                                "₹${String.format("%,.0f", totalReceivable)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BorderGray
                    )
                    
                    // Net payable
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🏪", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Net Payable",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = extendedColors.textPrimary
                            )
                        }
                        Text(
                            "₹${String.format("%,.0f", netPayable)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netPayable > 0) AccentOrange else AccentGreen
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search suppliers...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = extendedColors.textSecondary) 
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = extendedColors.textSecondary)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = SupplierAvatarColor
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Supplier count
            Text(
                "${filteredSuppliers.size} suppliers",
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Supplier List
            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🏪", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No suppliers found"
                            else "No suppliers yet",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = extendedColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to add a supplier",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extendedColors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredSuppliers) { supplierResult ->
                        SupplierCard(
                            supplierResult = supplierResult,
                            onClick = { onSupplierClick(supplierResult.customer.id) },
                            onLongClick = {
                                selectedSupplier = supplierResult
                                showActionSheet = true
                            }
                        )
                    }
                }
            }
        }
        
        // FAB
        FloatingActionButton(
            onClick = { showAddMenu = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = SupplierAvatarColor,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Supplier")
        }
    }

    // Delete confirmation dialog
    supplierToDelete?.let { supplier ->
        AlertDialog(
            onDismissRequest = { supplierToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed) },
            title = { Text("Delete Supplier") },
            text = { 
                Text("Are you sure you want to delete ${supplier.name}? This will also delete all related transactions.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(supplier)
                        supplierToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { supplierToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Long-press action bottom sheet
    if (showActionSheet && selectedSupplier != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showActionSheet = false
                selectedSupplier = null
            },
            containerColor = extendedColors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SupplierAvatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            selectedSupplier!!.customer.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            selectedSupplier!!.customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            selectedSupplier!!.customer.businessName ?: "Supplier",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extendedColors.textSecondary
                        )
                    }
                }
                
                HorizontalDivider(color = BorderGray)
                
                // View Ledger
                ListItem(
                    headlineContent = { Text("View Ledger") },
                    leadingContent = { 
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = SupplierAvatarColor) 
                    },
                    modifier = Modifier.clickable {
                        showActionSheet = false
                        onSupplierClick(selectedSupplier!!.customer.id)
                        selectedSupplier = null
                    }
                )
                
                // Edit
                ListItem(
                    headlineContent = { Text("Edit") },
                    leadingContent = { 
                        Icon(Icons.Default.Edit, contentDescription = null, tint = AccentBlue) 
                    },
                    modifier = Modifier.clickable {
                        showActionSheet = false
                        onEditSupplier(selectedSupplier!!.customer.id)
                        selectedSupplier = null
                    }
                )
                
                // Delete
                ListItem(
                    headlineContent = { Text("Delete", color = AccentRed) },
                    leadingContent = { 
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed) 
                    },
                    modifier = Modifier.clickable {
                        showActionSheet = false
                        supplierToDelete = selectedSupplier!!.customer
                        selectedSupplier = null
                    }
                )
            }
        }
    }
    
    // Add supplier menu
    if (showAddMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenu = false },
            containerColor = extendedColors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add Supplier",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                HorizontalDivider(color = BorderGray)
                
                ListItem(
                    headlineContent = { Text("Add New Supplier") },
                    supportingContent = { Text("Someone you buy from") },
                    leadingContent = { 
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SupplierAvatarColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = SupplierAvatarColor)
                        }
                    },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        onAddSupplier()
                    }
                )
            }
        }
    }
}

/**
 * Individual supplier card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SupplierCard(
    supplierResult: CustomerListResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val extendedColors = AppTheme.colors
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = extendedColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SupplierAvatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = supplierResult.customer.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and business
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = supplierResult.customer.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = extendedColors.textPrimary
                )
                if (supplierResult.customer.businessName != null) {
                    Text(
                        text = supplierResult.customer.businessName,
                        style = MaterialTheme.typography.bodySmall,
                        color = extendedColors.textSecondary
                    )
                } else if (supplierResult.customer.category != null) {
                    Text(
                        text = supplierResult.customer.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = extendedColors.textSecondary
                    )
                }
            }
            
            // Balance - For suppliers, negative = we owe them
            if (supplierResult.balance != 0.0) {
                val (helperText, balanceColor) = if (supplierResult.balance < 0) {
                    "You'll pay" to AccentOrange
                } else {
                    "You'll receive" to AccentGreen
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%,.0f", kotlin.math.abs(supplierResult.balance))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.labelSmall,
                        color = balanceColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = extendedColors.textTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
