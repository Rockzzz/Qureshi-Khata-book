package com.example.tabelahisabapp.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.backup.BackupInfo
import java.io.File

/**
 * Redesigned Backup & Restore Screen
 * 
 * Features:
 * - Section 1: Backup Status (Top)
 * - Section 2: Auto Backup Settings
 * - Section 3: Google Drive with Sign-In
 * - Section 4: Smart Restore Options
 * - Removed: "Open Backup Folder" button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel = hiltViewModel(),
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.handleGoogleSignInResult(result.data)
        }
    }
    
    // File picker launcher for selecting backup files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.restoreFromFileUri(context, it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ========== SECTION 1: BACKUP STATUS ==========
            BackupStatusSection(
                lastBackupTime = uiState.lastBackupTime,
                lastBackupRelative = uiState.lastBackupRelative,
                totalBackups = uiState.totalBackupsStored,
                backupSize = uiState.backupSize,
                googleDriveConnected = uiState.googleDriveEnabled,
                lastDriveSync = uiState.lastGoogleDriveSync,
                isLoading = uiState.isLoading,
                onBackupNow = { viewModel.createBackupNow() },
                onShareBackup = { viewModel.shareLatestBackup(context) },
                localBackupPath = viewModel.getLocalBackupFolderPath(),
                googleDriveFolderUrl = viewModel.getGoogleDriveFolderUrl()
            )

            // ========== SECTION 2: AUTO BACKUP SETTINGS ==========
            AutoBackupSettingsSection(
                dailyBackupEnabled = uiState.dailyBackupEnabled,
                transactionBackupEnabled = uiState.transactionBackupEnabled,
                appCloseBackupEnabled = uiState.appCloseBackupEnabled,
                onDailyBackupChanged = { viewModel.setDailyBackup(it) },
                onTransactionBackupChanged = { viewModel.setTransactionBackup(it) },
                onAppCloseBackupChanged = { viewModel.setAppCloseBackup(it) }
            )

            // ========== SECTION 3: GOOGLE DRIVE ==========
            GoogleDriveSection(
                enabled = uiState.googleDriveEnabled,
                account = uiState.googleDriveAccount,
                isSyncing = uiState.isGoogleDriveSyncing,
                onConnect = { 
                    val intent = viewModel.getGoogleSignInIntent()
                    googleSignInLauncher.launch(intent)
                },
                onDisconnect = { viewModel.signOutGoogleDrive() },
                onSync = { viewModel.syncToGoogleDrive() }
            )

            // ========== SECTION 4: RESTORE OPTIONS ==========
            RestoreSection(
                googleDriveConnected = uiState.googleDriveEnabled,
                onBrowseFiles = { filePickerLauncher.launch(arrayOf("application/json", "*/*")) },
                onFullRestore = { viewModel.showFullRestoreDialog() },
                onDriveRestore = { viewModel.showDriveRestoreDialog() },
                onTransactionRestore = { viewModel.showTransactionRestoreDialog() },
                onEmergencyRestore = { viewModel.showEmergencyRestoreDialog() }
            )

            // ========== INFO BANNER ==========
            InfoBanner()
        }
    }

    // ========== DIALOGS ==========
    
    // Crash Recovery Dialog
    if (uiState.showRecoveryDialog && uiState.recoveryBackupInfo != null) {
        CrashRecoveryDialog(
            backupInfo = uiState.recoveryBackupInfo!!,
            lastBackupTime = uiState.lastBackupTime ?: "Unknown",
            onRestore = { viewModel.emergencyRestore() },
            onSkip = { viewModel.hideRecoveryDialog() }
        )
    }

    // Full Restore Dialog
    if (uiState.showFullRestoreDialog) {
        FullRestoreDialog(
            backups = uiState.availableBackups,
            onRestore = { viewModel.restoreFromBackup(it.filePath) },
            onDismiss = { viewModel.hideFullRestoreDialog() }
        )
    }

    // Emergency Restore Confirmation
    if (uiState.showEmergencyRestoreDialog) {
        EmergencyRestoreDialog(
            onConfirm = { viewModel.emergencyRestore() },
            onDismiss = { viewModel.hideEmergencyRestoreDialog() }
        )
    }
    
    // Drive Restore Dialog
    if (uiState.showDriveRestoreDialog) {
        DriveRestoreDialog(
            backups = uiState.driveBackups,
            onRestore = { viewModel.restoreFromDrive(it.id) },
            onDismiss = { viewModel.hideDriveRestoreDialog() }
        )
    }

    // Show success/error messages
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }
}

// ========== SECTION COMPONENTS ==========

@Composable
private fun BackupStatusSection(
    lastBackupTime: String?,
    lastBackupRelative: String?,
    totalBackups: Int,
    backupSize: String?,
    googleDriveConnected: Boolean,
    lastDriveSync: String?,
    isLoading: Boolean,
    onBackupNow: () -> Unit,
    onShareBackup: () -> Unit,
    localBackupPath: String,
    googleDriveFolderUrl: String?
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Backup Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Last backup status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (lastBackupRelative != null) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (lastBackupRelative != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Last backup: ${lastBackupRelative ?: "Never"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            if (lastBackupTime != null && lastBackupRelative != lastBackupTime) {
                Text(
                    text = "    ($lastBackupTime)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Google Drive status with clickable link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (googleDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (googleDriveConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (googleDriveConnected) {
                            "Google Drive: Connected${lastDriveSync?.let { " (synced $it)" } ?: ""}"
                        } else {
                            "Google Drive: Not connected"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    // Google Drive folder link
                    if (googleDriveConnected && googleDriveFolderUrl != null) {
                        Text(
                            text = "📂 Open in Drive",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline
                            ),
                            modifier = Modifier
                                .clickable {
                                    try {
                                        uriHandler.openUri(googleDriveFolderUrl)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open Google Drive", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Local storage status with path and share functionality
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local backups: $totalBackups available${backupSize?.let { " ($it)" } ?: ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Backup folder path (clickable to copy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(localBackupPath))
                                Toast.makeText(context, "Path copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = localBackupPath,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy path",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    // Share backup link
                    Text(
                        text = "📤 Share Latest Backup",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        modifier = Modifier
                            .clickable {
                                onShareBackup()
                            }
                            .padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onBackupNow,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Backup, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup Now")
            }
        }
    }
}

@Composable
private fun AutoBackupSettingsSection(
    dailyBackupEnabled: Boolean,
    transactionBackupEnabled: Boolean,
    appCloseBackupEnabled: Boolean,
    onDailyBackupChanged: (Boolean) -> Unit,
    onTransactionBackupChanged: (Boolean) -> Unit,
    onAppCloseBackupChanged: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Auto Backup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Daily backup toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily backup", fontWeight = FontWeight.Medium)
                    Text(
                        "Full backup once per day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = dailyBackupEnabled,
                    onCheckedChange = onDailyBackupChanged
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Transaction backup toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Backup after every transaction", fontWeight = FontWeight.Medium)
                    Text(
                        "Never lose a single entry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = transactionBackupEnabled,
                    onCheckedChange = onTransactionBackupChanged
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // App close backup toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Backup on app close", fontWeight = FontWeight.Medium)
                    Text(
                        "Quick safety snapshot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appCloseBackupEnabled,
                    onCheckedChange = onAppCloseBackupChanged
                )
            }
        }
    }
}

@Composable
private fun GoogleDriveSection(
    enabled: Boolean,
    account: String?,
    isSyncing: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSync: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Google Drive Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (enabled && account != null) {
                        Text(
                            text = account,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (enabled) {
                // Connected state
                Text(
                    "Backups are automatically synced to Google Drive",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSync,
                        modifier = Modifier.weight(1f),
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync Now")
                    }
                    
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Disconnect")
                    }
                }
            } else {
                // Not connected state
                Text(
                    "Connect your Google account to automatically backup to the cloud",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connect Google Drive")
                }
            }
        }
    }
}

@Composable
private fun RestoreSection(
    googleDriveConnected: Boolean,
    onBrowseFiles: () -> Unit,
    onFullRestore: () -> Unit,
    onDriveRestore: () -> Unit,
    onTransactionRestore: () -> Unit,
    onEmergencyRestore: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Restore Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Browse & Select Backup Button (Primary option)
            Button(
                onClick = onBrowseFiles,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Browse & Select Backup")
            }
            
            Text(
                "Open file manager to select a backup file",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp)
            )
            
            // Full Restore Button (from app's backup list)
            OutlinedButton(
                onClick = onFullRestore,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Full Backup")
            }
            
            Text(
                "Restore from app's backup list",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            
            // Drive Restore Button
            if (googleDriveConnected) {
                OutlinedButton(
                    onClick = onDriveRestore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Google Drive")
                }
                
                Text(
                    "Browse and restore from cloud backups",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
                )
            }
            
            // Transaction Restore Button (Coming Soon)
            OutlinedButton(
                onClick = onTransactionRestore,
                modifier = Modifier.fillMaxWidth(),
                enabled = false  // Coming soon
            ) {
                Icon(Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Restore Transactions")
            }
            
            Text(
                "Restore only missing entries (coming soon)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 8.dp)
            )
            
            // Emergency Restore Button
            Button(
                onClick = onEmergencyRestore,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Emergency Restore")
            }
            
            Text(
                "One-tap restore from latest backup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun InfoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "📌 Backups are safely stored locally & synced to Google Drive when connected",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ========== DIALOGS ==========

@Composable
private fun CrashRecoveryDialog(
    backupInfo: BackupInfo,
    lastBackupTime: String,
    onRestore: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },  // Don't dismiss on outside click
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800)) },
        title = { Text("Data Recovery Available") },
        text = {
            Column {
                Text("The app did not close properly last time.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Last safe backup: $lastBackupTime")
                Text(
                    "Customers: ${backupInfo.customerCount}, Transactions: ${backupInfo.transactionCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onRestore) {
                Text("Restore Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    )
}

@Composable
private fun FullRestoreDialog(
    backups: List<BackupInfo>,
    onRestore: (BackupInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBackup by remember { mutableStateOf<BackupInfo?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Restore, contentDescription = null) },
        title = { Text("Select Backup to Restore") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                if (backups.isEmpty()) {
                    Text("No backups available")
                } else {
                    Text(
                        "⚠️ Current data will be backed up first, then replaced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    backups.take(10).forEach { backup ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBackup == backup,
                                onClick = { selectedBackup = backup }
                            )
                            Column {
                                Text(
                                    backup.fileName.removeSuffix(".json"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${backup.customerCount} customers, ${backup.transactionCount} transactions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedBackup?.let { onRestore(it) } },
                enabled = selectedBackup != null
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EmergencyRestoreDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Emergency Restore") },
        text = {
            Column {
                Text("This will restore from the most recent backup.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "⚠️ Current data will be backed up first, then replaced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Restore Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DriveRestoreDialog(
    backups: List<com.example.tabelahisabapp.data.backup.DriveBackupInfo>,
    onRestore: (com.example.tabelahisabapp.data.backup.DriveBackupInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBackup by remember { mutableStateOf<com.example.tabelahisabapp.data.backup.DriveBackupInfo?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
        title = { Text("Restore from Google Drive") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                if (backups.isEmpty()) {
                    Text("No backups found in Google Drive")
                } else {
                    Text(
                        "⚠️ Current data will be backed up first, then replaced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    backups.take(15).forEach { backup ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedBackup == backup,
                                onClick = { selectedBackup = backup }
                            )
                            Column {
                                Text(
                                    backup.name.removeSuffix(".json"),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${backup.sizeBytes / 1024} KB · ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(backup.createdTime))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedBackup?.let { onRestore(it) } },
                enabled = selectedBackup != null
            ) {
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
