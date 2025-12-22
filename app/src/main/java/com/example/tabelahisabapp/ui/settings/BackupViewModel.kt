package com.example.tabelahisabapp.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.backup.BackupInfo
import com.example.tabelahisabapp.data.backup.BackupManager
import com.example.tabelahisabapp.data.backup.GoogleDriveBackupManager
import com.example.tabelahisabapp.data.preferences.BackupPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    
    val message: String? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
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
                            googleDriveManager.syncLatestBackup(backupManager)
                            _uiState.update { it.copy(
                                isGoogleDriveSyncing = false,
                                lastGoogleDriveSync = "Just now"
                            )}
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
        _uiState.update { it.copy(showFullRestoreDialog = true) }
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
