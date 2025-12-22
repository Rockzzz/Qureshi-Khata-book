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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CustomerListScreen(
    viewModel: CustomerListViewModel = hiltViewModel(),
    initialFilter: String? = null,
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
    
    // Determine initial filter based on navigation parameter
    var selectedFilter by remember { 
        mutableStateOf(
            when (initialFilter) {
                "CUSTOMER_WITH_BALANCE" -> "CUSTOMER_OUTSTANDING"
                "SELLER_WITH_BALANCE" -> "SELLER_PENDING"
                else -> "ALL"
            }
        ) 
    }
    
    // Calculate counts for filters
    val allCount = allContacts.size
    val customerCount = allContacts.count { it.customer.type == "CUSTOMER" || it.customer.type == "BOTH" }
    val sellerCount = allContacts.count { it.customer.type == "SELLER" || it.customer.type == "BOTH" }
    
    // Calculate summary totals
    // DR = they owe us (positive balance for customers, or positive for sellers who owe us)
    // CR = we owe them (negative balance for customers, or negative for sellers)
    val totalReceivable = allContacts
        .filter { it.balance > 0 && (it.customer.type == "CUSTOMER" || it.customer.type == "BOTH") }
        .sumOf { it.balance } + 
        allContacts.filter { it.balance > 0 && it.customer.type == "SELLER" }.sumOf { it.balance }
    
    val totalPayable = allContacts
        .filter { it.balance < 0 }
        .sumOf { kotlin.math.abs(it.balance) }
    
    val netPosition = totalReceivable - totalPayable
    
    val filteredContacts = remember(allContacts, searchQuery, selectedFilter) {
        allContacts.filter { result ->
            val matchesSearch = searchQuery.isBlank() || 
                result.customer.name.contains(searchQuery, ignoreCase = true) ||
                (result.customer.phone?.contains(searchQuery) == true)
            
            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "CUSTOMER" -> result.customer.type == "CUSTOMER" || result.customer.type == "BOTH"
                "SELLER" -> result.customer.type == "SELLER" || result.customer.type == "BOTH"
                "CUSTOMER_OUTSTANDING" -> (result.customer.type == "CUSTOMER" || result.customer.type == "BOTH") && result.balance > 0
                "SELLER_PENDING" -> (result.customer.type == "SELLER" || result.customer.type == "BOTH") && result.balance < 0
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        containerColor = BackgroundGray,
        floatingActionButton = {
            GradientFAB(
                onClick = { showAddMenu = true },
                icon = Icons.Default.Add,
                contentDescription = "Add Contact"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Modern gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                        )
                    )
                    .padding(horizontal = Spacing.screenPadding, vertical = Spacing.lg)
            ) {
                Text(
                    text = "Customers & Sellers",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.md)
            ) {
                // Summary Bar - Business Dashboard
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Spacing.radiusMedium),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md)
                    ) {
                        // Receivable row
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
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "₹${String.format("%,.0f", totalReceivable)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Payable row
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
                                    color = TextSecondary
                                )
                            }
                            Text(
                                "₹${String.format("%,.0f", totalPayable)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }
                        
                        Divider(
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
                                    color = TextPrimary
                                )
                            }
                            Text(
                                "${if (netPosition >= 0) "+" else ""}₹${String.format("%,.0f", netPosition)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (netPosition >= 0) SuccessGreen else DangerRed
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Modern Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Aijaz, Gaffar...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { 
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                            }
                        }
                    },
                    shape = AppShapes.textField,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CardBackground,
                        focusedContainerColor = CardBackground,
                        unfocusedBorderColor = BorderGray,
                        focusedBorderColor = Purple600
                    )
                )
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Modern Filter Pills with counts
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("All ($allCount)") },
                        leadingIcon = if (selectedFilter == "ALL") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple600,
                            selectedLabelColor = CardBackground
                        ),
                        modifier = Modifier.height(36.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "CUSTOMER",
                        onClick = { selectedFilter = "CUSTOMER" },
                        label = { Text("Customers ($customerCount)") },
                        leadingIcon = if (selectedFilter == "CUSTOMER") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CustomerAvatarColor,
                            selectedLabelColor = CardBackground
                        ),
                        modifier = Modifier.height(36.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "SELLER",
                        onClick = { selectedFilter = "SELLER" },
                        label = { Text("Sellers ($sellerCount)") },
                        leadingIcon = if (selectedFilter == "SELLER") {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else {
                            { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SellerAvatarColor,
                            selectedLabelColor = CardBackground
                        ),
                        modifier = Modifier.height(36.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(Spacing.md))
                
                // Contacts List
                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                if (searchQuery.isNotBlank()) "No customer or seller found"
                                else "No contacts yet",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                "Tap + to add new",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.cardSpacing),
                        contentPadding = PaddingValues(bottom = 80.dp) // For FAB
                    ) {
                        items(filteredContacts) { customerResult ->
                            CleanContactCard(
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
        }
    }

    // Delete confirmation dialog
    customerToDelete?.let { customer ->
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete Contact") },
            text = { 
                Text("Are you sure you want to delete ${customer.name}? This will also delete all related transactions.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        customerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
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
            containerColor = CardBackground
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
                    val avatarColor = when (selectedContact!!.customer.type) {
                        "CUSTOMER" -> CustomerAvatarColor
                        "SELLER" -> SellerAvatarColor
                        "BOTH" -> BothAvatarColor
                        else -> CustomerAvatarColor
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
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
                            when (selectedContact!!.customer.type) {
                                "CUSTOMER" -> "Customer"
                                "SELLER" -> "Seller"
                                "BOTH" -> "Customer & Seller"
                                else -> "Customer"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
                
                Divider(color = BorderGray)
                
                // Action: View Ledger
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
                
                // Action: Edit
                ListItem(
                    headlineContent = { Text("Edit") },
                    leadingContent = { 
                        Icon(Icons.Default.Edit, contentDescription = null, tint = InfoBlue) 
                    },
                    modifier = Modifier.clickable {
                        showActionSheet = false
                        onEditCustomer(selectedContact!!.customer.id)
                        selectedContact = null
                    }
                )
                
                // Action: Delete
                ListItem(
                    headlineContent = { Text("Delete", color = DangerRed) },
                    leadingContent = { 
                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) 
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
    
    // Smart FAB menu - Add options
    if (showAddMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAddMenu = false },
            containerColor = CardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Add New",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                
                Divider(color = BorderGray)
                
                ListItem(
                    headlineContent = { Text("Add Customer") },
                    supportingContent = { Text("Someone who buys from you") },
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
                
                ListItem(
                    headlineContent = { Text("Add Seller") },
                    supportingContent = { Text("Someone you buy from") },
                    leadingContent = { 
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SellerAvatarColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = SellerAvatarColor)
                        }
                    },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        onAddCustomer() // Will handle type in form
                    }
                )
                
                ListItem(
                    headlineContent = { Text("Add Both") },
                    supportingContent = { Text("Customer + Seller combined") },
                    leadingContent = { 
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(BothAvatarColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.People, contentDescription = null, tint = BothAvatarColor)
                        }
                    },
                    modifier = Modifier.clickable {
                        showAddMenu = false
                        onAddCustomer() // Will handle type in form
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CleanContactCard(
    customerResult: CustomerListResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val typeLabel = when (customerResult.customer.type) {
        "CUSTOMER" -> "Customer"
        "SELLER" -> "Seller"
        "BOTH" -> "Both"
        else -> "Customer"
    }
    
    // Role-based avatar color
    val avatarColor = when (customerResult.customer.type) {
        "CUSTOMER" -> CustomerAvatarColor
        "SELLER" -> SellerAvatarColor
        "BOTH" -> BothAvatarColor
        else -> CustomerAvatarColor
    }
    
    val badgeColor = avatarColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(Spacing.radiusMedium),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            // Role-based colored Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = customerResult.customer.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            // Name and Badge
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customerResult.customer.name.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        typeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Balance with helper text
            if (customerResult.balance != 0.0) {
                // Determine indicator and color
                val (helperText, balanceColor, icon) = when {
                    customerResult.customer.type == "SELLER" && customerResult.balance < 0 -> 
                        Triple("You need to pay", DangerRed, "⬆️")
                    customerResult.customer.type == "SELLER" && customerResult.balance > 0 -> 
                        Triple("You will receive", SuccessGreen, "⬇️")
                    customerResult.balance > 0 -> 
                        Triple("You will receive", SuccessGreen, "⬇️")
                    else -> 
                        Triple("You need to pay", DangerRed, "⬆️")
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%,.0f", kotlin.math.abs(customerResult.balance))}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = balanceColor
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(icon, fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.labelSmall,
                            color = balanceColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Chevron to indicate tap action
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
