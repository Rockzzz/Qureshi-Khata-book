package com.example.tabelahisabapp.ui.supplier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.ui.customer.CustomerListViewModel
import com.example.tabelahisabapp.ui.theme.*

/**
 * Add/Edit Supplier Screen
 * 
 * Simplified version with only Business Name and Notes
 * Suppliers are contacts you BUY FROM (type = "SELLER")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierScreen(
    viewModel: CustomerListViewModel = hiltViewModel(),
    editSupplierId: Int? = null,
    onSupplierSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    // Get theme colors
    val extendedColors = AppTheme.colors
    val isDark = AppTheme.isDark
    
    // Load existing supplier if editing
    LaunchedEffect(editSupplierId) {
        if (editSupplierId != null) {
            viewModel.loadCustomerForEdit(editSupplierId) { customer ->
                businessName = customer.businessName ?: customer.name
                notes = customer.notes ?: ""
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
                                Color(0xFF22C55E), // Green for suppliers
                                Color(0xFF16A34A)
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
                        text = if (editSupplierId != null) "Edit Supplier" else "Add Supplier",
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
            // Business Name (Required)
            OutlinedTextField(
                value = businessName,
                onValueChange = { businessName = it },
                label = { Text("Business Name *") },
                placeholder = { Text("e.g. Khan Supplies") },
                leadingIcon = {
                    Icon(Icons.Default.Store, contentDescription = null, tint = extendedColors.textSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = Color(0xFF22C55E)
                ),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes (Optional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add any notes about this supplier...") },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = extendedColors.textSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = Color(0xFF22C55E)
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = {
                    if (businessName.isNotBlank()) {
                        isLoading = true
                        viewModel.saveSupplier(
                            id = editSupplierId,
                            name = businessName.trim(), // Use business name as the name
                            phone = null,
                            email = null,
                            businessName = businessName.trim(),
                            category = null,
                            openingBalance = 0.0,
                            notes = notes.ifBlank { null }
                        )
                        onSupplierSaved()
                    }
                },
                enabled = businessName.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
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
                        if (editSupplierId != null) "Update Supplier" else "Save Supplier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
