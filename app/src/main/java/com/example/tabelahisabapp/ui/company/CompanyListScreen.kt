package com.example.tabelahisabapp.ui.company

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyListScreen(
    viewModel: CompanyViewModel = hiltViewModel(),
    onAddCompany: () -> Unit,
    onEditCompany: (Int) -> Unit
) {
    val companies by viewModel.companies.collectAsState()
    var companyToDelete by remember { mutableStateOf<com.example.tabelahisabapp.data.db.entity.Company?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Companies") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCompany) {
                Icon(Icons.Default.Add, contentDescription = "Add Company")
            }
        }
    ) { paddingValues ->
        if (companies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏢", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No companies yet", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAddCompany) {
                        Text("+ Add First Company")
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(paddingValues)) {
                items(companies) { company ->
                    CompanyListItem(
                        company = company,
                        onEdit = { onEditCompany(company.id) },
                        onDelete = { companyToDelete = company }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    companyToDelete?.let { company ->
        AlertDialog(
            onDismissRequest = { companyToDelete = null },
            title = { Text("Delete Company") },
            text = { Text("Are you sure you want to delete ${company.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCompany(company)
                        companyToDelete = null
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { companyToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CompanyListItem(
    company: com.example.tabelahisabapp.data.db.entity.Company,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onEdit)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Business,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = company.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                company.code?.let {
                    Text(text = "Code: $it", fontSize = 12.sp, color = Color.Gray)
                }
                company.phone?.let {
                    Text(text = it, fontSize = 12.sp, color = Color.Gray)
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Blue, modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
            }
        }
    }
}

