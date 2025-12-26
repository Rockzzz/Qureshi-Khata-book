package com.example.tabelahisabapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.backup.BackupInfo
import com.example.tabelahisabapp.data.backup.BackupManager
import com.example.tabelahisabapp.data.backup.DriveBackupInfo
import com.example.tabelahisabapp.data.backup.GoogleDriveBackupManager
import com.example.tabelahisabapp.data.preferences.BackupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * UI State for the new Backup & Restore screen
 * Supports 3-layer backup (local auto, Google Drive, crash recovery),
 * event-based backups, and smart restore options
 */
data class BackupUiState(
    val isLoading: Boolean = false,
    
    // Backup Status
    val lastBackupTime: String? = null,
    val lastBackupRelative: String? = null,  // "2 hours ago"
    val backupSize: String? = null,          // "1.2 MB"
    val totalBackupsStored: Int = 0,
    
    // Auto-backup settings
    val dailyBackupEnabled: Boolean = true,
    val transactionBackupEnabled: Boolean = true,
    val appCloseBackupEnabled: Boolean = true,
    
    // Google Drive
    val googleDriveEnabled: Boolean = false,
    val googleDriveAccount: String? = null,
    val lastGoogleDriveSync: String? = null,
    val isGoogleDriveSyncing: Boolean = false,
    
    // Restore 
    val availableBackups: List<BackupInfo> = emptyList(),
    val showRecoveryDialog: Boolean = false,
    val recoveryBackupInfo: BackupInfo? = null,
    
    // Dialogs
    val showFullRestoreDialog: Boolean = false,
    val showTransactionRestoreDialog: Boolean = false,
    val showEmergencyRestoreDialog: Boolean = false,
    val showDriveRestoreDialog: Boolean = false,
    val driveBackups: List<DriveBackupInfo> = emptyList(),
    
    val message: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager,
    private val backupPreferences: BackupPreferences,
    private val googleDriveManager: GoogleDriveBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        loadBackupSettings()
        loadBackupStatus()
        checkGoogleDriveStatus()
    }

    private fun loadBackupSettings() {
        viewModelScope.launch {
            try {
                backupPreferences.dailyBackupEnabled.collect { enabled ->
                    _uiState.update { it.copy(dailyBackupEnabled = enabled) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        viewModelScope.launch {
            try {
                backupPreferences.transactionBackupEnabled.collect { enabled ->
                    _uiState.update { it.copy(transactionBackupEnabled = enabled) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        viewModelScope.launch {
            try {
                backupPreferences.appCloseBackupEnabled.collect { enabled ->
                    _uiState.update { it.copy(appCloseBackupEnabled = enabled) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadBackupStatus() {
        viewModelScope.launch {
            try {
                val stats = backupManager.getBackupStats()
                val backups = backupManager.getAvailableBackups()
                val latestBackup = backups.firstOrNull()
                
                _uiState.update { state ->
                    state.copy(
                        totalBackupsStored = stats.first,
                        backupSize = formatFileSize(stats.second),
                        availableBackups = backups,
                        lastBackupTime = latestBackup?.let { formatTimestamp(it.timestamp) },
                        lastBackupRelative = latestBackup?.let { getRelativeTime(it.timestamp) }
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkGoogleDriveStatus() {
        viewModelScope.launch {
            try {
                val isSignedIn = googleDriveManager.isSignedIn()
                val email = googleDriveManager.getSignedInEmail()
                
                _uiState.update { it.copy(
                    googleDriveEnabled = isSignedIn,
                    googleDriveAccount = email
                )}
                
                // Get last sync time
                backupPreferences.lastGoogleDriveSync.collect { timestamp ->
                    timestamp?.let {
                        _uiState.update { state ->
                            state.copy(lastGoogleDriveSync = getRelativeTime(it))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============ BACKUP ACTIONS ============

    fun createBackupNow() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val result = backupManager.createDailyBackup()
                
                if (result.success) {
                    // Update backup status inline
                    try {
                        val stats = backupManager.getBackupStats()
                        val backups = backupManager.getAvailableBackups()
                        val latestBackup = backups.firstOrNull()
                        
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                message = "Backup created successfully!",
                                totalBackupsStored = stats.first,
                                backupSize = formatFileSize(stats.second),
                                availableBackups = backups,
                                lastBackupTime = latestBackup?.let { formatTimestamp(it.timestamp) },
                                lastBackupRelative = latestBackup?.let { getRelativeTime(it.timestamp) }
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        _uiState.update { it.copy(isLoading = false, message = "Backup created!") }
                    }
                    
                    // Auto-sync to Google Drive if enabled (don't crash if this fails)
                    if (_uiState.value.googleDriveEnabled) {
                        try {
                            _uiState.update { it.copy(isGoogleDriveSyncing = true) }
                            
                            // Check the Result to ensure upload actually succeeded
                            val syncResult = googleDriveManager.syncLatestBackup(backupManager)
                            
                            syncResult.fold(
                                onSuccess = {
                                    // Only update sync time when upload actually succeeds
                                    _uiState.update { it.copy(
                                        isGoogleDriveSyncing = false,
                                        lastGoogleDriveSync = "Just now"
                                    )}
                                },
                                onFailure = { error ->
                                    // Don't update sync time on failure
                                    _uiState.update { it.copy(isGoogleDriveSyncing = false) }
                                    // Log error silently
                                    error.printStackTrace()
                                }
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            _uiState.update { it.copy(isGoogleDriveSyncing = false) }
                        }
                    }
                } else {
                    _uiState.update { it.copy(
                        isLoading = false,
                        message = "Backup failed: ${result.error}"
                    )}
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Backup failed: ${e.message}"
                )}
            }
        }
    }

    // ============ GOOGLE DRIVE ============

    /**
     * Get the Google Sign-In intent to launch
     */
    fun getGoogleSignInIntent(): Intent {
        return googleDriveManager.getSignInIntent()
    }

    /**
     * Handle the Google Sign-In result
     */
    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val result = googleDriveManager.handleSignInResult(data)
                
                result.fold(
                    onSuccess = { email ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            googleDriveEnabled = true,
                            googleDriveAccount = email,
                            message = "Connected to Google Drive: $email"
                        )}
                        // Sync backup after connecting
                        syncToGoogleDrive()
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            message = "Google Sign-In failed: ${error.message}"
                        )}
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Google Sign-In failed: ${e.message}"
                )}
            }
        }
    }

    /**
     * Sign out from Google Drive
     */
    fun signOutGoogleDrive() {
        viewModelScope.launch {
            try {
                googleDriveManager.signOut()
                _uiState.update { it.copy(
                    googleDriveEnabled = false,
                    googleDriveAccount = null,
                    lastGoogleDriveSync = null,
                    message = "Disconnected from Google Drive"
                )}
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(message = "Failed to sign out: ${e.message}") }
            }
        }
    }

    /**
     * Sync latest backup to Google Drive
     */
    fun syncToGoogleDrive() {
        viewModelScope.launch {
            if (!_uiState.value.googleDriveEnabled) {
                _uiState.update { it.copy(message = "Please connect to Google Drive first") }
                return@launch
            }
            
            _uiState.update { it.copy(isGoogleDriveSyncing = true) }
            
            try {
                val result = googleDriveManager.syncLatestBackup(backupManager)
                
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(
                            isGoogleDriveSyncing = false,
                            lastGoogleDriveSync = "Just now",
                            message = "Synced to Google Drive"
                        )}
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(
                            isGoogleDriveSyncing = false,
                            message = "Sync failed: ${error.message}"
                        )}
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isGoogleDriveSyncing = false,
                    message = "Sync failed: ${e.message}"
                )}
            }
        }
    }

    // ============ SETTINGS TOGGLES ============

    fun setDailyBackup(enabled: Boolean) {
        viewModelScope.launch {
            try {
                backupPreferences.setDailyBackupEnabled(enabled)
                _uiState.update { it.copy(dailyBackupEnabled = enabled) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setTransactionBackup(enabled: Boolean) {
        viewModelScope.launch {
            try {
                backupPreferences.setTransactionBackupEnabled(enabled)
                _uiState.update { it.copy(transactionBackupEnabled = enabled) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setAppCloseBackup(enabled: Boolean) {
        viewModelScope.launch {
            try {
                backupPreferences.setAppCloseBackupEnabled(enabled)
                _uiState.update { it.copy(appCloseBackupEnabled = enabled) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============ RESTORE ACTIONS ============

    fun showFullRestoreDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Refresh the backup list before showing dialog
                val backups = backupManager.getAvailableBackups()
                android.util.Log.d("BackupViewModel", "Loaded ${backups.size} backups for restore dialog")
                
                _uiState.update { it.copy(
                    showFullRestoreDialog = true,
                    availableBackups = backups,
                    isLoading = false
                )}
            } catch (e: Exception) {
                android.util.Log.e("BackupViewModel", "Error loading backups", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Error loading backups: ${e.message}"
                )}
            }
        }
    }

    fun hideFullRestoreDialog() {
        _uiState.update { it.copy(showFullRestoreDialog = false) }
    }

    fun showTransactionRestoreDialog() {
        _uiState.update { it.copy(showTransactionRestoreDialog = true) }
    }

    fun hideTransactionRestoreDialog() {
        _uiState.update { it.copy(showTransactionRestoreDialog = false) }
    }

    fun showEmergencyRestoreDialog() {
        _uiState.update { it.copy(showEmergencyRestoreDialog = true) }
    }

    fun hideEmergencyRestoreDialog() {
        _uiState.update { it.copy(showEmergencyRestoreDialog = false) }
    }

    fun hideRecoveryDialog() {
        _uiState.update { it.copy(showRecoveryDialog = false) }
    }
    
    // ============ GOOGLE DRIVE RESTORE ============
    
    fun showDriveRestoreDialog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val result = googleDriveManager.listDriveBackups()
                result.fold(
                    onSuccess = { backups ->
                        _uiState.update { it.copy(
                            showDriveRestoreDialog = true,
                            driveBackups = backups,
                            isLoading = false
                        )}
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            message = "Failed to load Drive backups: ${error.message}"
                        )}
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Error loading backups: ${e.message}"
                )}
            }
        }
    }
    
    fun hideDriveRestoreDialog() {
        _uiState.update { it.copy(showDriveRestoreDialog = false) }
    }
    
    fun restoreFromDrive(driveFileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDriveRestoreDialog = false) }
            
            try {
                // Download from Drive to temp file
                val tempFile = File(context.cacheDir, "temp_restore.json")
                val downloadResult = googleDriveManager.downloadBackup(driveFileId, tempFile)
                
                downloadResult.fold(
                    onSuccess = { file ->
                        // Restore from downloaded file
                        val restoreResult = backupManager.restoreFromBackup(file.absolutePath)
                        
                        _uiState.update { it.copy(
                            isLoading = false,
                            message = if (restoreResult.success) 
                                "Restored from Drive! Please restart the app." 
                            else 
                                "Restore failed: ${restoreResult.error}"
                        )}
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(
                            isLoading = false,
                            message = "Download failed: ${error.message}"
                        )}
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Error: ${e.message}"
                )}
            }
        }
    }

    fun restoreFromBackup(backupPath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showFullRestoreDialog = false) }
            
            try {
                // Create a backup first before restoring
                backupManager.createDailyBackup()
                
                val result = backupManager.restoreFromBackup(backupPath)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    message = if (result.success) "Data restored successfully! Please restart the app." else "Restore failed: ${result.error}"
                )}
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Restore failed: ${e.message}"
                )}
            }
        }
    }

    fun emergencyRestore() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true, 
                showEmergencyRestoreDialog = false,
                showRecoveryDialog = false
            )}
            
            try {
                val result = backupManager.emergencyRestore()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    message = if (result.success) "Data restored successfully! Please restart the app." else "Restore failed: ${result.error}"
                )}
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Restore failed: ${e.message}"
                )}
            }
        }
    }
    
    /**
     * Restore from a file Uri selected via file picker
     */
    fun restoreFromFileUri(activityContext: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // Read the file content from Uri
                val inputStream = activityContext.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _uiState.update { it.copy(
                        isLoading = false,
                        message = "Could not open the selected file"
                    )}
                    return@launch
                }
                
                val jsonContent = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()
                
                // Save to temp file
                val tempFile = File(activityContext.cacheDir, "temp_restore_${System.currentTimeMillis()}.json")
                tempFile.writeText(jsonContent)
                
                // Create backup of current data first
                backupManager.createDailyBackup()
                
                // Restore from the temp file
                val result = backupManager.restoreFromBackup(tempFile.absolutePath)
                
                // Clean up temp file
                tempFile.delete()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    message = if (result.success) 
                        "Data restored successfully! Please restart the app." 
                    else 
                        "Restore failed: ${result.error}"
                )}
                
            } catch (e: Exception) {
                android.util.Log.e("BackupViewModel", "Error restoring from file", e)
                _uiState.update { it.copy(
                    isLoading = false,
                    message = "Restore failed: ${e.message}"
                )}
            }
        }
    }

    // ============ FOLDER ACCESS ============
    
    /**
     * Get the local backup folder path
     */
    fun getLocalBackupFolderPath(): String {
        return backupManager.getBackupFolderPath()
    }
    
    /**
     * Share the latest backup file using Android's share intent
     */
    fun shareLatestBackup(activityContext: android.content.Context) {
        viewModelScope.launch {
            try {
                val backups = backupManager.getAvailableBackups()
                val latestBackup = backups.firstOrNull()
                
                if (latestBackup == null) {
                    _uiState.update { it.copy(message = "No backup available to share") }
                    return@launch
                }
                
                val file = File(latestBackup.filePath)
                if (!file.exists()) {
                    _uiState.update { it.copy(message = "Backup file not found") }
                    return@launch
                }
                
                // Use FileProvider to get a content URI
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    activityContext,
                    "${activityContext.packageName}.fileprovider",
                    file
                )
                
                // Create share intent
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Qureshi Khata Book Backup")
                    putExtra(android.content.Intent.EXTRA_TEXT, "Backup file: ${file.name}")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                // Launch chooser
                val chooserIntent = android.content.Intent.createChooser(shareIntent, "Share Backup")
                activityContext.startActivity(chooserIntent)
                
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(message = "Error sharing backup: ${e.message}") }
            }
        }
    }
    
    /**
     * Get the Google Drive backup folder URL
     * Opens the "Qureshi Khata Book" folder in Google Drive web
     */
    fun getGoogleDriveFolderUrl(): String? {
        // Google Drive folder URL format for app data folder
        // The app stores backups in "Qureshi Khata Book/Daily" folder
        return if (_uiState.value.googleDriveEnabled) {
            "https://drive.google.com/drive/search?q=type:folder%20%22Qureshi%20Khata%20Book%22"
        } else {
            null
        }
    }
    
    // ============ UTILITY ============

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("dd MMM, h:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    private fun getRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            hours < 24 -> "$hours hours ago"
            days < 7 -> "$days days ago"
            else -> formatTimestamp(timestamp)
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
}
