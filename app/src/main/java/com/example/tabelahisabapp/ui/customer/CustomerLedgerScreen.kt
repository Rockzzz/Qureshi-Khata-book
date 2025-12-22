package com.example.tabelahisabapp.ui.customer

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*
import com.example.tabelahisabapp.utils.VoiceNotePlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    viewModel: CustomerLedgerViewModel = hiltViewModel(),
    onBackPressed: () -> Unit,
    onAddTransaction: (Int) -> Unit,
    onEditTransaction: (Int, Int) -> Unit,
    onVoiceClick: () -> Unit = {}
) {
    val customer by viewModel.customer.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val balanceSummary by viewModel.balanceSummary.collectAsState()
    var transactionToDelete by remember { mutableStateOf<CustomerTransaction?>(null) }

    Scaffold(
        containerColor = BackgroundGray,
        floatingActionButton = {
            customer?.let { cust ->
                GradientFAB(
                    onClick = { onAddTransaction(cust.id) },
                    icon = Icons.Default.Add,
                    contentDescription = "Add Transaction"
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Gradient Header with Avatar
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CardBackground
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    
                    customer?.let { cust ->
                        val (gradientStart, gradientEnd) = getAvatarGradient(cust.name)
                        GradientAvatar(
                            name = cust.name,
                            size = 56.dp,
                            gradientStart = gradientStart,
                            gradientEnd = gradientEnd
                        )
                        
                        Spacer(modifier = Modifier.width(Spacing.md))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = cust.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = CardBackground
                            )
                            cust.phone?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CardBackground.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardSpacing)
            ) {
                // Balance Summary Card
                balanceSummary?.let { summary ->
                    val balance = summary.totalCredit - summary.totalDebit
                    
                    WhiteCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 8.dp
                    ) {
                        SectionHeader(text = "Outstanding Balance")
                        
                        Spacer(modifier = Modifier.height(Spacing.md))
                        
                        // Large Outstanding Amount
                        DisplayAmount(
                            amount = "₹${String.format("%.0f", balance)}",
                            color = if (balance >= 0) DangerRed else SuccessGreen
                        )
                        
                        Divider(
                            modifier = Modifier.padding(vertical = Spacing.md),
                            color = BorderGray
                        )
                        
                        // Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                LabelText(text = "Total Udhaar Diya")
                                Text(
                                    text = "₹${String.format("%.0f", summary.totalCredit)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                LabelText(text = "Total Paisa Mila")
                                Text(
                                    text = "₹${String.format("%.0f", summary.totalDebit)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
                
                // Transaction History Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(text = "Transaction History")
                    customer?.let { cust ->
                        TextButton(onClick = { onAddTransaction(cust.id) }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Text("Add")
                        }
                    }
                }
                
                // Transactions List
                val context = LocalContext.current
                val voiceNotePlayer = remember { VoiceNotePlayer(context) }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.cardSpacing),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(transactions) { transaction ->
                        ModernTransactionCard(
                            transaction = transaction,
                            voiceNotePlayer = voiceNotePlayer,
                            onEdit = {
                                customer?.let { cust ->
                                    onEditTransaction(cust.id, transaction.id)
                                }
                            },
                            onDelete = { transactionToDelete = transaction }
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

@Composable
fun ModernTransactionCard(
    transaction: CustomerTransaction,
    voiceNotePlayer: VoiceNotePlayer,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    val formattedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(transaction.date))
    val hasVoiceNote = transaction.voiceNotePath?.let { File(it).exists() } ?: false
    
    WhiteCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Left Border Accent (Red for debit, Green for credit)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .background(
                        color = if (transaction.type == "CREDIT") DangerRed else SuccessGreen,
                        shape = RoundedCornerShape(Spacing.radiusSmall)
                    )
            )
            
            Spacer(modifier = Modifier.width(Spacing.md))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (transaction.type == "CREDIT") "Udhaar Diya" else "Paisa Mila",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == "CREDIT") DangerRed else SuccessGreen
                    )
                    
                    // Voice indicator badge if voice note exists
                    if (hasVoiceNote) {
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Surface(
                            shape = RoundedCornerShape(Spacing.radiusSmall),
                            color = InfoBlue.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Entry",
                                    tint = InfoBlue,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Voice",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = InfoBlue
                                )
                            }
                        }
                    }
                    
                    // Payment mode label (Cash/Bank)
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(
                        text = if (transaction.paymentMethod == "BANK") "Bank" else "Cash",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                
                transaction.note?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
            
            // Amount
            Text(
                text = "₹${String.format("%.0f", transaction.amount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "CREDIT") DangerRed else SuccessGreen
            )
            
            // Actions
            Column {
                // Voice playback button if exists
                transaction.voiceNotePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    voiceNotePlayer.stop()
                                    isPlaying = false
                                } else {
                                    voiceNotePlayer.play(file) {
                                        isPlaying = false
                                    }
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.PlayArrow else Icons.Default.VolumeUp,
                                contentDescription = "Play Voice",
                                tint = InfoBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = InfoBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DangerRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
