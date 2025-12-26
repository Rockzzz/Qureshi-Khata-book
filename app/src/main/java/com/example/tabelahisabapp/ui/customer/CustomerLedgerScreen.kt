package com.example.tabelahisabapp.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
fun CustomerLedgerScreen(
    viewModel: CustomerLedgerViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onAddTransaction: (Int, String) -> Unit, // Added type param
    onEditTransaction: (Int, Int) -> Unit,
    onVoiceClick: () -> Unit = {}
) {
    val customer by viewModel.customer.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    var transactionToDelete by remember { mutableStateOf<CustomerTransaction?>(null) }
    
    // Calculate running balance
    // 1. Sort transactions by date ascending to calculate
    val sortedTransactions = transactions.sortedBy { it.date }
    var runningBalance = 0.0
    val transactionsWithBalance = sortedTransactions.map { tx ->
        val amount = if (tx.type == "CREDIT") -tx.amount else tx.amount // Credit = Gave (-), Debit = Got (+)
        runningBalance += amount
        tx to runningBalance
    }
    // 2. Reverse back for display (newest on top)
    val displayTransactions = transactionsWithBalance.sortedByDescending { it.first.date }
    
    // Group by Date
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
                                 customer?.let { cust ->
                                     val (gradientStart, gradientEnd) = getAvatarGradient(cust.name)
                                     GradientAvatar(
                                         name = cust.name,
                                         size = 40.dp,
                                         gradientStart = gradientStart,
                                         gradientEnd = gradientEnd
                                     )
                                     Spacer(modifier = Modifier.width(12.dp))
                                     Column {
                                         Text(
                                             text = cust.name,
                                             style = MaterialTheme.typography.titleMedium,
                                             fontWeight = FontWeight.Bold
                                         )
                                         cust.phone?.let {
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
                        ),
                        actions = {
                             IconButton(onClick = { /* Report */ }) {
                                 Icon(androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_sort_by_size), contentDescription = "Report")
                             }
                        }
                    )
                }
            }
        },
        bottomBar = {
            customer?.let { cust ->
                // Check if this is a supplier
                val isSupplier = cust.type == "SELLER" || cust.type == "BOTH"
                
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
                        // YOU GAVE Button (Red) - CREDIT (Payment)
                        Button(
                            onClick = { onAddTransaction(cust.id, "CREDIT") },
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
                        
                        // YOU GOT Button (Green) 
                        // For SUPPLIER: PURCHASE (goods received)
                        // For CUSTOMER: DEBIT (money received)
                        Button(
                            onClick = { 
                                val txType = if (isSupplier) "PURCHASE" else "DEBIT"
                                onAddTransaction(cust.id, txType) 
                            },
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
            customer?.let { cust ->
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
                             text = if (balance < 0) "You will get" else "You will give",
                             color = TextSecondary,
                             fontSize = 14.sp
                         )
                         Text(
                             text = "₹${String.format("%.0f", abs(balance))}",
                             color = if (balance < 0) DangerRed else SuccessGreen,
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
                        KhataTransactionRow(
                            transaction = tx,
                            runningBalance = balance,
                            voiceNotePlayer = voiceNotePlayer,
                            onEdit = { 
                                customer?.let { cust -> 
                                    onEditTransaction(cust.id, tx.id) 
                                } 
                            },
                             // Long press handling could be added here
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
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this transaction?") },
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KhataTransactionRow(
    transaction: CustomerTransaction,
    runningBalance: Double,
    voiceNotePlayer: VoiceNotePlayer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val hasVoiceNote = transaction.voiceNotePath?.let { File(it).exists() } ?: false
    
    // Determine if Cash or Bank
    val isCash = transaction.paymentMethod == "CASH"
    
    // Background color: White usually
    Surface(
        color = Color.White,
        modifier = Modifier
             .fillMaxWidth()
             .combinedClickable(
                 onClick = {}, // No action on regular click
                 onLongClick = { showMenu = true } // Show menu on long press
             )
             .padding(horizontal = 16.dp, vertical = 8.dp),
         shape = RoundedCornerShape(8.dp),
         shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Date/Time, Payment Method & Note
                Column(modifier = Modifier.weight(1f)) {
                    // Date + Payment Method Row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(transaction.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Cash/Bank Badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isCash) Color(0xFFE8F5E9) else Color(0xFFE3F2FD)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isCash) Icons.Default.Money else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isCash) SuccessGreen else InfoBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCash) "Cash" else "Bank",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = if (isCash) SuccessGreen else InfoBlue
                                )
                            }
                        }
                    }
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
                // Gave (Credit)
                if (transaction.type == "CREDIT") {
                    Text(
                         text = "₹${String.format("%.0f", transaction.amount)}",
                         modifier = Modifier.weight(0.8f),
                         textAlign = TextAlign.End,
                         color = DangerRed,
                         fontWeight = FontWeight.Bold,
                         fontSize = 16.sp
                    )
                     Spacer(modifier = Modifier.weight(0.8f)) // Empty Debit side
                } else {
                    Spacer(modifier = Modifier.weight(0.8f)) // Empty Credit side
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
                     text = "Balance: ",
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
                     text = if (runningBalance < 0) " (Due)" else " (Adv)",
                     style = MaterialTheme.typography.bodySmall,
                     color = if (runningBalance < 0) DangerRed else SuccessGreen
                 )
            }
        }
    }
    
    // Bottom Sheet Menu
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Edit Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenu = false
                            onEdit()
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = InfoBlue
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "✏️ Edit Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Divider()
                
                // Delete Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenu = false
                            onDelete()
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DangerRed
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "🗑️ Delete Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DangerRed
                    )
                }
            }
        }
    }
}
