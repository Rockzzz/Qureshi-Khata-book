package com.example.tabelahisabapp.ui.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    viewModel: AddCustomerViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle? = null,
    onCustomerAdded: () -> Unit,
    onCancel: () -> Unit
) {
    val customerId = savedStateHandle?.get<Int>("customerId")
    val isEditMode = customerId != null
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var contactType by remember { mutableStateOf("CUSTOMER") } // CUSTOMER, SELLER, BOTH
    var originalCreatedAt by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load customer data when editing
    LaunchedEffect(customerId) {
        if (customerId != null) {
            scope.launch {
                viewModel.getCustomer(customerId)?.let { customer ->
                    name = customer.name
                    phone = customer.phone ?: ""
                    contactType = customer.type
                    originalCreatedAt = customer.createdAt
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Contact" else "Add Contact") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Contact Type Selector
            Text(
                "Contact Type",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactTypeChip(
                    label = "Customer",
                    emoji = "👤",
                    description = "Jisko aap paisa dete ho",
                    isSelected = contactType == "CUSTOMER",
                    onClick = { contactType = "CUSTOMER" },
                    modifier = Modifier.weight(1f)
                )
                ContactTypeChip(
                    label = "Seller",
                    emoji = "🏪",
                    description = "Jisse aap khareedtey ho",
                    isSelected = contactType == "SELLER",
                    onClick = { contactType = "SELLER" },
                    modifier = Modifier.weight(1f)
                )
                ContactTypeChip(
                    label = "Both",
                    emoji = "👤🏪",
                    description = "Dono kaam karta hai",
                    isSelected = contactType == "BOTH",
                    onClick = { contactType = "BOTH" },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("e.g. Aijaz, Gaffar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    when (contactType) {
                        "CUSTOMER" -> Icon(Icons.Default.Person, contentDescription = null)
                        "SELLER" -> Icon(Icons.Default.Store, contentDescription = null)
                        else -> Row {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Phone field
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Help text based on type
            Text(
                text = when (contactType) {
                    "CUSTOMER" -> "💡 Customer: Jo aapko paisa deta hai ya jisko aap udhaar dete ho (jaise Aijaz)"
                    "SELLER" -> "💡 Seller: Jisse aap maal khareedtey ho (jaise Gaffar - buffalo seller)"
                    else -> "💡 Both: Jo customer bhi hai aur seller bhi"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Save button
            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    errorMessage = null
                    scope.launch {
                        val result = viewModel.saveCustomer(
                            customerId, 
                            name, 
                            phone.ifBlank { null }, 
                            contactType, 
                            originalCreatedAt
                        )
                        result.fold(
                            onSuccess = { onCustomerAdded() },
                            onFailure = { error ->
                                errorMessage = error.message ?: "Failed to save contact"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ContactTypeChip(
    label: String,
    emoji: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .selectable(selected = isSelected, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Text(
                label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp
            )
        }
    }
}
