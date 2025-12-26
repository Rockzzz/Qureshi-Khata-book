package com.example.tabelahisabapp.ui.supplier

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*
import com.example.tabelahisabapp.utils.VoiceNotePlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierLedgerScreen(
    viewModel: SupplierLedgerViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onAddTransaction: (Int, String) -> Unit,
    onEditTransaction: (Int, Int) -> Unit
) {
    val supplier by viewModel.supplier.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var transactionToDelete by remember { mutableStateOf<CustomerTransaction?>(null) }
    
    // Calculate running balance
    // For Supplier (stored as Customer): 
    // Types: "PURCHASE" (You Got Goods) and "PAYMENT"/"CREDIT" (You Gave Money)
    
    val sortedTransactions = transactions.sortedBy { it.date }
    var runningBalance = 0.0
    val transactionsWithBalance = sortedTransactions.map { tx ->
        // Logic: Purchase (Liability) increases what we owe (+). 
        // Payment (Asset/Liability Reduction) decreases what we owe (-).
        // Check ViewModel mapping: 
        // "DEBIT" UI -> "PURCHASE" Logic Type.
        // "CREDIT" UI -> "PAYMENT" or "CREDIT" Logic Type.
        
        val amount = if (tx.type == "PURCHASE" || tx.type == "DEBIT") {
            tx.amount // Increases liability (we owe more)
        } else {
            -tx.amount // Payment (we owe less)
        }
        runningBalance += amount
        tx to runningBalance
    }
    // Reverse for display
    val displayTransactions = transactionsWithBalance.sortedByDescending { it.first.date }
    
    val groupedTransactions = displayTransactions.groupBy { 
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it.first.date))
    }

    Scaffold(
        containerColor = BackgroundGray,
        topBar = {
            Surface(
                color = Purple700,
                contentColor = Color.White,
                shadowElevation = 4.dp
            ) {
                Column {
                   TopAppBar(
                        title = {
                             Row(verticalAlignment = Alignment.CenterVertically) {
                                 supplier?.let { supp ->
                                     val (gradientStart, gradientEnd) = getAvatarGradient(supp.name)
                                     GradientAvatar(
                                         name = supp.name,
                                         size = 40.dp,
                                         gradientStart = gradientStart,
                                         gradientEnd = gradientEnd
                                     )
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Column {
                                         Text(
                                             text = supp.name,
                                             style = MaterialTheme.typography.titleMedium,
                                             fontWeight = FontWeight.Bold
                                         )
                                         supp.phone?.let {
                                             Text(
                                                 text = it,
                                                 style = MaterialTheme.typography.bodySmall,
                                                 color = Color.White.copy(alpha = 0.8f)
                                             )
                                         }
                                     }
                                 }
                             }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackPressed) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = Color.White,
                            navigationIconContentColor = Color.White
                        )
                    )
                }
            }
        },
        bottomBar = {
            supplier?.let { supp ->
                Surface(
                    shadowElevation = 16.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // YOU GAVE Button (Red) - PAYMENT
                        Button(
                            onClick = { onAddTransaction(supp.id, "CREDIT") }, // UI uses "CREDIT" for GAVE/RED
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                        ) {
                            Text(
                                text = "YOU GAVE ₹",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        
                        // YOU GOT Button (Green) - PURCHASE (Goods)
                        Button(
                            onClick = { onAddTransaction(supp.id, "DEBIT") }, // UI uses "DEBIT" for GOT/GREEN
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text(
                                text = "YOU GOT ₹",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
            ) {
            // Summary Header
            supplier?.let { supp ->
                val balance = displayTransactions.firstOrNull()?.second ?: 0.0
                
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(BackgroundGray, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                         Text(
                             text = if (balance > 0) "Usage (Due)" else "Advance Paid",
                             color = TextSecondary,
                             fontSize = 14.sp
                         )
                         Text(
                             text = "₹${String.format("%.0f", abs(balance))}",
                             color = if (balance > 0) DangerRed else SuccessGreen,
                             fontSize = 24.sp,
                             fontWeight = FontWeight.Bold
                         )
                    }
                }
            }

            // Headers for columns
            Surface(
                color = BackgroundGray,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "ENTRIES",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "YOU GAVE",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.End
                    )
                     Text(
                        text = "YOU GOT",
                        modifier = Modifier.weight(0.8f),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.End
                    )
                }
            }

            // Transaction List
            val context = LocalContext.current
            val voiceNotePlayer = remember { VoiceNotePlayer(context) }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                groupedTransactions.forEach { (date, txs) ->
                    // Date Header
                    item {
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
                                         text = date,
                                         modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                         style = MaterialTheme.typography.bodySmall,
                                         color = TextSecondary
                                     )
                                 }
                             }
                        }
                    }
                    
                    items(txs) { (tx, balance) ->
                        SupplierTransactionRow(
                            transaction = tx,
                            runningBalance = balance,
                            voiceNotePlayer = voiceNotePlayer,
                            onEdit = { 
                                supplier?.let { supp -> 
                                    onEditTransaction(supp.id, tx.id) 
                                } 
                            },
                             onDelete = { transactionToDelete = tx }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation
    transactionToDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transaction)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SupplierTransactionRow(
    transaction: CustomerTransaction,
    runningBalance: Double,
    voiceNotePlayer: VoiceNotePlayer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val hasVoiceNote = transaction.voiceNotePath?.let { File(it).exists() } ?: false
    
    // Background color
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit) 
            .padding(horizontal = 16.dp, vertical = 8.dp),
         shape = RoundedCornerShape(8.dp),
         shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                     if (transaction.note?.isNotBlank() == true) {
                         Text(
                             text = transaction.note,
                             style = MaterialTheme.typography.bodyMedium,
                             fontWeight = FontWeight.Medium,
                             maxLines = 2
                         )
                     }
                      if (hasVoiceNote) {
                          Icon(
                              Icons.Default.Mic, 
                              contentDescription = "Voice",
                              tint = InfoBlue,
                              modifier = Modifier.size(16.dp).padding(top = 4.dp)
                          )
                      }
                }
                
                // Amounts
                // You Gave (Payment/Credit) -> Red
                if (transaction.type == "PAYMENT" || transaction.type == "CREDIT") {
                    Text(
                         text = "₹${String.format("%.0f", transaction.amount)}",
                         modifier = Modifier.weight(0.8f),
                         textAlign = TextAlign.End,
                         color = DangerRed,
                         fontWeight = FontWeight.Bold,
                         fontSize = 16.sp
                    )
                     Spacer(modifier = Modifier.weight(0.8f)) 
                } else {
                    // You Got (Purchase/Debit) -> Green
                    Spacer(modifier = Modifier.weight(0.8f))
                    Text(
                         text = "₹${String.format("%.0f", transaction.amount)}",
                         modifier = Modifier.weight(0.8f),
                         textAlign = TextAlign.End,
                         color = SuccessGreen,
                         fontWeight = FontWeight.Bold,
                         fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = BorderGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(4.dp))
            
            // Running Balance
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.End
            ) {
                 Text(
                     text = "Due: ",
                     style = MaterialTheme.typography.bodySmall,
                     color = TextSecondary
                 )
                 Text(
                     text = "₹${String.format("%.0f", abs(runningBalance))}",
                     style = MaterialTheme.typography.bodySmall,
                     fontWeight = FontWeight.Bold,
                     color = TextPrimary
                 )
                 Text(
                     text = if (runningBalance > 0) " (Due)" else " (Adv)",
                     style = MaterialTheme.typography.bodySmall,
                     color = if (runningBalance > 0) DangerRed else SuccessGreen
                 )
            }
        }
    }
}
