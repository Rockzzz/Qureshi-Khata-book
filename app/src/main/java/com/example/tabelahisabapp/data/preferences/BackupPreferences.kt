package com.example.tabelahisabapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_settings")

/**
 * DataStore for backup preferences
 * Persists all auto-backup settings and Google Drive configuration
 */
@Singleton
class BackupPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Auto-backup settings
        private val DAILY_BACKUP_ENABLED = booleanPreferencesKey("daily_backup_enabled")
        private val TRANSACTION_BACKUP_ENABLED = booleanPreferencesKey("transaction_backup_enabled")
        private val APP_CLOSE_BACKUP_ENABLED = booleanPreferencesKey("app_close_backup_enabled")
        
        // Google Drive settings
        private val GOOGLE_DRIVE_ENABLED = booleanPreferencesKey("google_drive_enabled")
        private val GOOGLE_DRIVE_ACCOUNT = stringPreferencesKey("google_drive_account")
        private val LAST_GOOGLE_DRIVE_SYNC = longPreferencesKey("last_google_drive_sync")
        
        // Backup tracking
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        private val LAST_DAILY_BACKUP_DATE = longPreferencesKey("last_daily_backup_date")
        
        // Retention settings
        private val RETENTION_COUNT = intPreferencesKey("retention_count")
        
        // Crash recovery
        private val APP_LAST_CLOSED_PROPERLY = booleanPreferencesKey("app_last_closed_properly")
        private val LAST_SAFE_BACKUP_PATH = stringPreferencesKey("last_safe_backup_path")
    }
    
    // ============ PREFERENCES FLOW ============
    
    val dailyBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { prefs -> prefs[DAILY_BACKUP_ENABLED] ?: true }
    
    val transactionBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { prefs -> prefs[TRANSACTION_BACKUP_ENABLED] ?: true }
    
    val appCloseBackupEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { prefs -> prefs[APP_CLOSE_BACKUP_ENABLED] ?: true }
    
    val googleDriveEnabled: Flow<Boolean> = context.backupDataStore.data
        .map { prefs -> prefs[GOOGLE_DRIVE_ENABLED] ?: false }
    
    val googleDriveAccount: Flow<String?> = context.backupDataStore.data
        .map { prefs -> prefs[GOOGLE_DRIVE_ACCOUNT] }
    
    val lastGoogleDriveSync: Flow<Long?> = context.backupDataStore.data
        .map { prefs -> prefs[LAST_GOOGLE_DRIVE_SYNC] }
    
    val lastBackupTime: Flow<Long?> = context.backupDataStore.data
        .map { prefs -> prefs[LAST_BACKUP_TIME] }
    
    val lastDailyBackupDate: Flow<Long?> = context.backupDataStore.data
        .map { prefs -> prefs[LAST_DAILY_BACKUP_DATE] }
    
    val retentionCount: Flow<Int> = context.backupDataStore.data
        .map { prefs -> prefs[RETENTION_COUNT] ?: 30 }
    
    val appLastClosedProperly: Flow<Boolean> = context.backupDataStore.data
        .map { prefs -> prefs[APP_LAST_CLOSED_PROPERLY] ?: true }
    
    val lastSafeBackupPath: Flow<String?> = context.backupDataStore.data
        .map { prefs -> prefs[LAST_SAFE_BACKUP_PATH] }
    
    // ============ UPDATE METHODS ============
    
    suspend fun setDailyBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[DAILY_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun setTransactionBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[TRANSACTION_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun setAppCloseBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[APP_CLOSE_BACKUP_ENABLED] = enabled
        }
    }
    
    suspend fun setGoogleDriveEnabled(enabled: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[GOOGLE_DRIVE_ENABLED] = enabled
        }
    }
    
    suspend fun setGoogleDriveAccount(account: String?) {
        context.backupDataStore.edit { prefs ->
            if (account != null) {
                prefs[GOOGLE_DRIVE_ACCOUNT] = account
            } else {
                prefs.remove(GOOGLE_DRIVE_ACCOUNT)
            }
        }
    }
    
    suspend fun setLastGoogleDriveSync(timestamp: Long) {
        context.backupDataStore.edit { prefs ->
            prefs[LAST_GOOGLE_DRIVE_SYNC] = timestamp
        }
    }
    
    suspend fun setLastBackupTime(timestamp: Long) {
        context.backupDataStore.edit { prefs ->
            prefs[LAST_BACKUP_TIME] = timestamp
        }
    }
    
    suspend fun setLastDailyBackupDate(date: Long) {
        context.backupDataStore.edit { prefs ->
            prefs[LAST_DAILY_BACKUP_DATE] = date
        }
    }
    
    suspend fun setRetentionCount(count: Int) {
        context.backupDataStore.edit { prefs ->
            prefs[RETENTION_COUNT] = count
        }
    }
    
    suspend fun setAppClosedProperly(properly: Boolean) {
        context.backupDataStore.edit { prefs ->
            prefs[APP_LAST_CLOSED_PROPERLY] = properly
        }
    }
    
    suspend fun setLastSafeBackupPath(path: String?) {
        context.backupDataStore.edit { prefs ->
            if (path != null) {
                prefs[LAST_SAFE_BACKUP_PATH] = path
            } else {
                prefs.remove(LAST_SAFE_BACKUP_PATH)
            }
        }
    }
}
