package com.example.tabelahisabapp.ui.daily

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDailyEntryScreen(
    viewModel: DailySummaryViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle? = null,
    onEntrySaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val entryDate = savedStateHandle?.get<Long>("date")
    val isEditMode = entryDate != null
    
    var openingCash by remember { mutableStateOf("") }
    var openingBank by remember { mutableStateOf("") }
    var closingCash by remember { mutableStateOf("") }
    var closingBank by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(entryDate ?: viewModel.getTodayDateNormalized()) }
    var originalCreatedAt by remember { mutableStateOf<Long?>(null) }

    // Load entry data when editing
    val balanceForDate = if (entryDate != null) {
        viewModel.getBalanceByDate(entryDate)
            .collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }
    
    val balance by balanceForDate
    
    LaunchedEffect(entryDate, balance) {
        if (entryDate != null && balance != null) {
            balance?.let {
                openingCash = it.openingCash.toString()
                openingBank = it.openingBank.toString()
                closingCash = it.closingCash.toString()
                closingBank = it.closingBank.toString()
                note = it.note ?: ""
                originalCreatedAt = it.createdAt
                date = it.date
            }
        }
    }

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            date = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Calculations
    val cashSpent = (openingCash.toDoubleOrNull() ?: 0.0) - (closingCash.toDoubleOrNull() ?: 0.0)
    val bankDiff = (closingBank.toDoubleOrNull() ?: 0.0) - (openingBank.toDoubleOrNull() ?: 0.0)
    val netSpend = cashSpent - bankDiff

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Daily Entry" else "Add Daily Entry") }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            // Date picker
            OutlinedTextField(
                value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date)),
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                enabled = false
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Opening balances
            Text("Opening Balances", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = openingCash,
                onValueChange = { openingCash = it },
                label = { Text("Opening Cash") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = openingBank,
                onValueChange = { openingBank = it },
                label = { Text("Opening Bank") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Closing balances
            Text("Closing Balances", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = closingCash,
                onValueChange = { closingCash = it },
                label = { Text("Closing Cash") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = closingBank,
                onValueChange = { closingBank = it },
                label = { Text("Closing Bank") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calculated values
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Calculated Values", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cash Spent", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                "₹${String.format("%.2f", cashSpent)}",
                                fontWeight = FontWeight.Bold,
                                color = if (cashSpent > 0) Color.Red else Color.Green
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bank Difference", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                "₹${String.format("%.2f", bankDiff)}",
                                fontWeight = FontWeight.Bold,
                                color = if (bankDiff >= 0) Color.Green else Color.Red
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Net Daily Spend", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        "₹${String.format("%.2f", netSpend)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netSpend > 0) Color.Red else Color.Green
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveDailyBalance(
                            date = date,
                            openingCash = openingCash.toDoubleOrNull() ?: 0.0,
                            openingBank = openingBank.toDoubleOrNull() ?: 0.0,
                            closingCash = closingCash.toDoubleOrNull() ?: 0.0,
                            closingBank = closingBank.toDoubleOrNull() ?: 0.0,
                            note = note.ifBlank { null },
                            originalCreatedAt = originalCreatedAt
                        )
                        onEntrySaved()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Cancel button
            Button(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Cancel")
            }
        }
    }
}

