package com.example.tabelahisabapp.ui.settings

import android.content.Context
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportPrintScreen(
    viewModel: ExportViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showPrintSettingsDialog by remember { mutableStateOf(false) }
    var selectedPrintType by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export & Print") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export Options
            Text(
                text = "Export Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Export to Excel
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.exportToExcel(context) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export to Excel", fontWeight = FontWeight.Medium)
                        Text(
                            "Complete data with multiple sheets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (uiState.isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // Export to CSV
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.exportToCsv(context) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Export to CSV", fontWeight = FontWeight.Medium)
                        Text(
                            "Simple spreadsheet format",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            Divider()

            // Print Reports
            Text(
                text = "Print Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Print Customer Ledger
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { 
                    selectedPrintType = "customer"
                    showPrintSettingsDialog = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Print Customer Ledger", fontWeight = FontWeight.Medium)
                }
            }

            // Print Daily Register
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { 
                    selectedPrintType = "daily"
                    showPrintSettingsDialog = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Print Daily Register", fontWeight = FontWeight.Medium)
                }
            }

            // Print Trading Report
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { 
                    selectedPrintType = "trading"
                    showPrintSettingsDialog = true
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Print,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Print Trading Report", fontWeight = FontWeight.Medium)
                }
            }

            Divider()

            // Export Location Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Export Location",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "/Download/UdhaarLedger/Exports/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Print Settings Dialog
    if (showPrintSettingsDialog) {
        PrintSettingsDialog(
            printType = selectedPrintType,
            onDismiss = { showPrintSettingsDialog = false },
            onPrint = { settings ->
                showPrintSettingsDialog = false
                viewModel.printReport(context, selectedPrintType, settings)
            }
        )
    }

    // Show messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
}

data class PrintSettings(
    val paperSize: String = "A4",
    val orientation: String = "Portrait",
    val includeTimestamps: Boolean = true,
    val includePhoneNumbers: Boolean = true,
    val includeSummary: Boolean = true,
    val businessName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintSettingsDialog(
    printType: String,
    onDismiss: () -> Unit,
    onPrint: (PrintSettings) -> Unit
) {
    var paperSize by remember { mutableStateOf("A4") }
    var orientation by remember { mutableStateOf("Portrait") }
    var includeTimestamps by remember { mutableStateOf(true) }
    var includePhoneNumbers by remember { mutableStateOf(true) }
    var includeSummary by remember { mutableStateOf(true) }
    var businessName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Print Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Paper Size
                Text("Paper Size", fontWeight = FontWeight.Medium)
                Row {
                    FilterChip(
                        selected = paperSize == "A4",
                        onClick = { paperSize = "A4" },
                        label = { Text("A4") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = paperSize == "Letter",
                        onClick = { paperSize = "Letter" },
                        label = { Text("Letter") }
                    )
                }

                // Orientation
                Text("Orientation", fontWeight = FontWeight.Medium)
                Row {
                    FilterChip(
                        selected = orientation == "Portrait",
                        onClick = { orientation = "Portrait" },
                        label = { Text("Portrait") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = orientation == "Landscape",
                        onClick = { orientation = "Landscape" },
                        label = { Text("Landscape") }
                    )
                }

                Divider()

                // Include options
                Text("Include in Report", fontWeight = FontWeight.Medium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeTimestamps,
                        onCheckedChange = { includeTimestamps = it }
                    )
                    Text("Transaction timestamps")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includePhoneNumbers,
                        onCheckedChange = { includePhoneNumbers = it }
                    )
                    Text("Customer phone numbers")
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = includeSummary,
                        onCheckedChange = { includeSummary = it }
                    )
                    Text("Summary totals")
                }

                Divider()

                // Business Header
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business Name (Optional)") },
                    placeholder = { Text("Your Business Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPrint(PrintSettings(
                        paperSize = paperSize,
                        orientation = orientation,
                        includeTimestamps = includeTimestamps,
                        includePhoneNumbers = includePhoneNumbers,
                        includeSummary = includeSummary,
                        businessName = businessName
                    ))
                }
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Print")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

