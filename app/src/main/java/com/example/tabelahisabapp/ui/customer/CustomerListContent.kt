package com.example.tabelahisabapp.ui.customer

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
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*

// Role-based avatar colors
private val CustomerAvatarColor = Color(0xFF0EA5E9) // Blue/Teal
private val SellerAvatarColor = Color(0xFF22C55E) // Green
private val BothAvatarColor = Color(0xFF8B5CF6) // Purple

/**
 * Embeddable Customer List Content - For use inside AccountsScreen tabs
 * This version has NO header (AccountsScreen provides the header)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomerListContent(
    viewModel: CustomerListViewModel = hiltViewModel(),
    filterType: String = "CUSTOMER", // "CUSTOMER" to show only customers
    initialFilter: String? = null, // Filter from home screen navigation
    onAddCustomer: () -> Unit,
    onEditCustomer: (Int) -> Unit,
    onCustomerClick: (Int) -> Unit
) {
    val allContacts by viewModel.customers.collectAsState()
    var customerToDelete by remember { mutableStateOf<com.example.tabelahisabapp.data.db.entity.Customer?>(null) }
    var selectedContact by remember { mutableStateOf<CustomerListResult?>(null) }
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
                "CUSTOMER_WITH_BALANCE" -> "CUSTOMER_OUTSTANDING"
                "CUSTOMER_PAYMENTS" -> "CUSTOMER_PAYMENTS"
                "SUPPLIER_WITH_BALANCE" -> "SUPPLIER_PENDING"
                "SUPPLIER_ADVANCE" -> "SUPPLIER_ADVANCE"
                else -> "ALL"
            }
        ) 
    }
    
    // Filter to only show CUSTOMER or BOTH types, with additional balance filter if needed
    val customerContacts = remember(allContacts, selectedFilter) {
        allContacts.filter { result ->
            // Apply filter based on selected filter type
            when (selectedFilter) {
                "ALL" -> result.customer.type == "CUSTOMER" || result.customer.type == "BOTH"
                "CUSTOMER_OUTSTANDING" -> (result.customer.type == "CUSTOMER" || result.customer.type == "BOTH") && result.balance > 0
                "CUSTOMER_PAYMENTS" -> (result.customer.type == "CUSTOMER" || result.customer.type == "BOTH") && result.balance < 0
                "SUPPLIER_PENDING" -> (result.customer.type == "SELLER" || result.customer.type == "BOTH") && result.balance < 0
                "SUPPLIER_ADVANCE" -> (result.customer.type == "SELLER" || result.customer.type == "BOTH") && result.balance > 0
                else -> result.customer.type == "CUSTOMER" || result.customer.type == "BOTH"
            }
        }
    }
    
    // Calculate summary totals for customers only
    val totalReceivable = customerContacts
        .filter { it.balance > 0 }
        .sumOf { it.balance }
    
    val totalPayable = customerContacts
        .filter { it.balance < 0 }
        .sumOf { kotlin.math.abs(it.balance) }
    
    val netPosition = totalReceivable - totalPayable
    
    val filteredContacts = remember(customerContacts, searchQuery) {
        customerContacts.filter { result ->
            searchQuery.isBlank() || 
            result.customer.name.contains(searchQuery, ignoreCase = true) ||
            (result.customer.phone?.contains(searchQuery) == true)
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
            
            // Summary Card - Customers Only
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
                    // You will receive row
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // You need to pay row
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
                            color = AccentRed
                        )
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = BorderGray
                    )
                    
                    // Net position row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💰", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Net Position",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = extendedColors.textPrimary
                            )
                        }
                        Text(
                            "${if (netPosition >= 0) "+" else ""}₹${String.format("%,.0f", netPosition)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netPosition >= 0) AccentGreen else AccentRed
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customers...") },
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
                    focusedBorderColor = Purple600
                )
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Customer count
            Text(
                "${filteredContacts.size} customers",
                style = MaterialTheme.typography.bodyMedium,
                color = extendedColors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Customer List
            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👤", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No customers found"
                            else "No customers yet",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = extendedColors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to add a customer",
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
                    items(filteredContacts) { customerResult ->
                        CustomerCard(
                            customerResult = customerResult,
                            onClick = { onCustomerClick(customerResult.customer.id) },
                            onLongClick = {
                                selectedContact = customerResult
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
            containerColor = Purple600,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Customer")
        }
    }

    // Delete confirmation dialog
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRed) },
            title = { Text("Delete Customer") },
            text = { 
                Text("Are you sure you want to delete ${customer.name}? This will also delete all related transactions.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Long-press action bottom sheet
    if (showActionSheet && selectedContact != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showActionSheet = false
                selectedContact = null
            },
            containerColor = extendedColors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Header with contact info
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
                            .background(CustomerAvatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            selectedContact!!.customer.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            selectedContact!!.customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Customer",
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
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = Purple600) 
                    },
                    modifier = Modifier.clickable {
                        showActionSheet = false
                        onCustomerClick(selectedContact!!.customer.id)
                        selectedContact = null
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
                        onEditCustomer(selectedContact!!.customer.id)
                        selectedContact = null
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
                        customerToDelete = selectedContact!!.customer
                        selectedContact = null
                    }
                )
            }
        }
    }
    
    // Add customer menu
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
                    "Add Customer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                HorizontalDivider(color = BorderGray)
                
                ListItem(
                    headlineContent = { Text("Add New Customer") },
                    supportingContent = { Text("Someone who owes you money") },
                    leadingContent = { 
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(CustomerAvatarColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CustomerAvatarColor)
                        }
                    },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        onAddCustomer()
                    }
                )
            }
        }
    }
}

/**
 * Individual customer card
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CustomerCard(
    customerResult: CustomerListResult,
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
                    .background(CustomerAvatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customerResult.customer.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerResult.customer.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = extendedColors.textPrimary
                )
                if (customerResult.customer.phone != null) {
                    Text(
                        text = customerResult.customer.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = extendedColors.textSecondary
                    )
                }
            }
            
            // Balance
            if (customerResult.balance != 0.0) {
                val (helperText, balanceColor) = if (customerResult.balance > 0) {
                    "You'll receive" to AccentGreen
                } else {
                    "You'll pay" to AccentRed
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%,.0f", kotlin.math.abs(customerResult.balance))}",
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
