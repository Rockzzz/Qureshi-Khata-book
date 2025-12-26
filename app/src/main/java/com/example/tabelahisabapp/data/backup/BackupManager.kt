package com.example.tabelahisabapp.data.backup

import android.content.Context
import android.os.Environment
import com.example.tabelahisabapp.data.db.AppDatabase
import com.example.tabelahisabapp.data.db.entity.*
import com.example.tabelahisabapp.data.preferences.BackupPreferences
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core backup manager handling all backup operations:
 * - Daily full backups (JSON format with all data)
 * - Transaction-level backups (individual transaction JSON)
 * - Quick snapshots on app close
 * - Backup cleanup and retention management
 * 
 * Backups are stored in: Downloads/Qureshi Khata Book/
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val backupPreferences: BackupPreferences,
    private val googleDriveManager: GoogleDriveBackupManager
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    companion object {
        private const val APP_FOLDER_NAME = "Qureshi Khata Book"
        private const val DAILY_DIR = "daily"
        private const val TRANSACTIONS_DIR = "transactions"
        private const val SNAPSHOT_DIR = "snapshots"
        
        private const val DAILY_BACKUP_PREFIX = "full_"
        private const val TRANSACTION_BACKUP_PREFIX = "tx_"
        private const val SNAPSHOT_BACKUP_PREFIX = "snapshot_"
        
        private const val BACKUP_EXTENSION = ".json"
    }
    
    // ============ DIRECTORY MANAGEMENT ============
    
    /**
     * Gets the backup folder path for external access (e.g., file explorer)
     */
    fun getBackupFolderPath(): String {
        return getBackupRootDir().absolutePath
    }
    
    /**
     * Gets the backup root directory in app's external files
     * Path: /storage/emulated/0/Android/data/com.example.tabelahisabapp/files/Qureshi Khata Book/
     * 
     * This location is:
     * - Accessible via file managers (unlike internal storage)
     * - No permissions required (unlike public Downloads)
     * - Works on all Android versions
     */
    private fun getBackupRootDir(): File {
        // Use app's external files directory - accessible via file manager, no permissions needed
        val externalFilesDir = context.getExternalFilesDir(null)
        val dir = if (externalFilesDir != null) {
            File(externalFilesDir, APP_FOLDER_NAME)
        } else {
            // Fallback to internal storage if external not available
            File(context.filesDir, APP_FOLDER_NAME)
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    private fun getDailyBackupDir(): File {
        val dir = File(getBackupRootDir(), DAILY_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    private fun getTransactionBackupDir(): File {
        val dir = File(getBackupRootDir(), TRANSACTIONS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    private fun getSnapshotBackupDir(): File {
        val dir = File(getBackupRootDir(), SNAPSHOT_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
    
    // ============ DAILY FULL BACKUP ============
    
    /**
     * Creates a complete backup of all data
     * Filename format: full_2025-12-21_06-00.json
     */
    suspend fun createDailyBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val fileName = "$DAILY_BACKUP_PREFIX${dateFormat.format(Date(timestamp))}$BACKUP_EXTENSION"
            val backupFile = File(getDailyBackupDir(), fileName)
            
            // Collect all data from database
            val fullBackupData = collectAllData(timestamp)
            
            // Write to file
            val json = gson.toJson(fullBackupData)
            backupFile.writeText(json)
            
            // Update preferences
            backupPreferences.setLastBackupTime(timestamp)
            backupPreferences.setLastDailyBackupDate(normalizeToMidnight(timestamp))
            backupPreferences.setLastSafeBackupPath(backupFile.absolutePath)
            
            // Cleanup old backups
            cleanupOldBackups()
            
            // Auto-sync to Google Drive if connected
            autoSyncToDrive(backupFile)
            
            BackupResult(
                success = true,
                filePath = backupFile.absolutePath,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                error = e.message ?: "Unknown error creating daily backup"
            )
        }
    }
    
    private suspend fun collectAllData(timestamp: Long): FullBackupData {
        val customers = database.customerDao().getAllCustomersList()
        val customerTransactions = database.customerTransactionDao().getAllTransactionsList()
        val dailyBalances = database.dailyBalanceDao().getAllBalancesList()
        val dailyLedgerTransactions = database.dailyLedgerTransactionDao().getAllTransactionsList()
        val dailyExpenses = database.dailyExpenseDao().getAllExpensesList()
        val tradeTransactions = database.tradeTransactionDao().getAllTransactionsList()
        val companies = database.companyDao().getAllCompaniesList()
        val farms = database.farmDao().getAllFarmsList()
        
        return FullBackupData(
            timestamp = timestamp,
            customers = customers.map { it.toBackup() },
            customerTransactions = customerTransactions.map { it.toBackup() },
            dailyBalances = dailyBalances.map { it.toBackup() },
            dailyLedgerTransactions = dailyLedgerTransactions.map { it.toBackup() },
            dailyExpenses = dailyExpenses.map { it.toBackup() },
            tradeTransactions = tradeTransactions.map { it.toBackup() },
            companies = companies.map { it.toBackup() },
            farms = farms.map { it.toBackup() }
        )
    }
    
    // ============ TRANSACTION-LEVEL BACKUP ============
    
    /**
     * Creates a backup for a single transaction
     * Called after each transaction is saved
     */
    suspend fun createTransactionBackup(
        transactionId: String,
        type: String,
        amount: Double,
        from: String?,
        to: String?,
        voiceInput: String? = null,
        paymentMethod: String = "CASH"
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            if (!backupPreferences.transactionBackupEnabled.first()) {
                return@withContext BackupResult(success = true, error = "Transaction backup disabled")
            }
            
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "$TRANSACTION_BACKUP_PREFIX${dateFormat.format(Date(timestamp))}$BACKUP_EXTENSION"
            val backupFile = File(getTransactionBackupDir(), fileName)
            
            val transactionBackup = TransactionBackupData(
                id = transactionId,
                type = type,
                amount = amount,
                from = from,
                to = to,
                timestamp = timestamp,
                voiceInput = voiceInput,
                paymentMethod = paymentMethod
            )
            
            val json = gson.toJson(transactionBackup)
            backupFile.writeText(json)
            
            backupPreferences.setLastBackupTime(timestamp)
            
            // Auto-sync to Google Drive if connected
            autoSyncToDrive(backupFile)
            
            BackupResult(
                success = true,
                filePath = backupFile.absolutePath,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                error = e.message ?: "Unknown error creating transaction backup"
            )
        }
    }
    
    // ============ QUICK SNAPSHOT ============
    
    /**
     * Creates a quick snapshot on app close/pause
     * Only creates if no backup in last 5 minutes
     */
    suspend fun createQuickSnapshot(): BackupResult = withContext(Dispatchers.IO) {
        try {
            if (!backupPreferences.appCloseBackupEnabled.first()) {
                return@withContext BackupResult(success = true, error = "App close backup disabled")
            }
            
            val lastBackupTime = backupPreferences.lastBackupTime.first() ?: 0
            val timeSinceLastBackup = System.currentTimeMillis() - lastBackupTime
            
            // Skip if backup exists in last 5 minutes
            if (timeSinceLastBackup < 5 * 60 * 1000) {
                return@withContext BackupResult(success = true, error = "Recent backup exists")
            }
            
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
            val fileName = "$SNAPSHOT_BACKUP_PREFIX${dateFormat.format(Date(timestamp))}$BACKUP_EXTENSION"
            val backupFile = File(getSnapshotBackupDir(), fileName)
            
            val fullBackupData = collectAllData(timestamp)
            val json = gson.toJson(fullBackupData)
            backupFile.writeText(json)
            
            backupPreferences.setLastBackupTime(timestamp)
            backupPreferences.setLastSafeBackupPath(backupFile.absolutePath)
            backupPreferences.setAppClosedProperly(true)
            
            // Auto-sync to Google Drive if connected
            autoSyncToDrive(backupFile)
            
            // Keep only last 3 snapshots
            cleanupSnapshots(keepCount = 3)
            
            BackupResult(
                success = true,
                filePath = backupFile.absolutePath,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                error = e.message ?: "Unknown error creating snapshot"
            )
        }
    }
    
    // ============ BACKUP LISTING ============
    
    /**
     * Gets list of all available backups with metadata
     */
    suspend fun getAvailableBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val allBackups = mutableListOf<BackupInfo>()
        
        // Check if backup directory exists and is accessible
        val rootDir = getBackupRootDir()
        android.util.Log.d("BackupManager", "Looking for backups in: ${rootDir.absolutePath}")
        android.util.Log.d("BackupManager", "Root dir exists: ${rootDir.exists()}, canRead: ${rootDir.canRead()}")
        
        // Scan root folder for backup files (for files saved directly there)
        rootDir.listFiles()?.filter { it.isFile && it.extension == "json" }?.forEach { file ->
            try {
                android.util.Log.d("BackupManager", "Found backup file in root: ${file.name}")
                val backupData = gson.fromJson(file.readText(), FullBackupData::class.java)
                allBackups.add(
                    BackupInfo(
                        id = file.nameWithoutExtension,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        timestamp = backupData.timestamp,
                        sizeBytes = file.length(),
                        type = BackupType.FULL_DAILY,
                        customerCount = backupData.customers.size,
                        transactionCount = backupData.customerTransactions.size + 
                                          backupData.dailyLedgerTransactions.size +
                                          backupData.tradeTransactions.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Error reading backup file: ${file.name}", e)
            }
        }
        
        // Daily backups from subdirectory
        val dailyDir = getDailyBackupDir()
        android.util.Log.d("BackupManager", "Daily dir: ${dailyDir.absolutePath}, exists: ${dailyDir.exists()}")
        dailyDir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                android.util.Log.d("BackupManager", "Found daily backup: ${file.name}")
                val backupData = gson.fromJson(file.readText(), FullBackupData::class.java)
                allBackups.add(
                    BackupInfo(
                        id = file.nameWithoutExtension,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        timestamp = backupData.timestamp,
                        sizeBytes = file.length(),
                        type = BackupType.FULL_DAILY,
                        customerCount = backupData.customers.size,
                        transactionCount = backupData.customerTransactions.size + 
                                          backupData.dailyLedgerTransactions.size +
                                          backupData.tradeTransactions.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Error reading daily backup: ${file.name}", e)
            }
        }
        
        // Snapshots from subdirectory
        val snapshotDir = getSnapshotBackupDir()
        android.util.Log.d("BackupManager", "Snapshot dir: ${snapshotDir.absolutePath}, exists: ${snapshotDir.exists()}")
        snapshotDir.listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            try {
                android.util.Log.d("BackupManager", "Found snapshot: ${file.name}")
                val backupData = gson.fromJson(file.readText(), FullBackupData::class.java)
                allBackups.add(
                    BackupInfo(
                        id = file.nameWithoutExtension,
                        fileName = file.name,
                        filePath = file.absolutePath,
                        timestamp = backupData.timestamp,
                        sizeBytes = file.length(),
                        type = BackupType.QUICK_SNAPSHOT,
                        customerCount = backupData.customers.size,
                        transactionCount = backupData.customerTransactions.size + 
                                          backupData.dailyLedgerTransactions.size +
                                          backupData.tradeTransactions.size
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Error reading snapshot: ${file.name}", e)
            }
        }
        
        android.util.Log.d("BackupManager", "Total backups found: ${allBackups.size}")
        allBackups.sortedByDescending { it.timestamp }
    }
    
    /**
     * Gets the latest available backup
     */
    suspend fun getLatestBackup(): BackupInfo? {
        return getAvailableBackups().firstOrNull()
    }
    
    /**
     * Gets total backup size and count
     */
    suspend fun getBackupStats(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        var totalCount = 0
        var totalSize = 0L
        
        getDailyBackupDir().listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            totalCount++
            totalSize += file.length()
        }
        
        getSnapshotBackupDir().listFiles()?.filter { it.extension == "json" }?.forEach { file ->
            totalCount++
            totalSize += file.length()
        }
        
        Pair(totalCount, totalSize)
    }
    
    // ============ RESTORE ============
    
    /**
     * Restores from a full backup file
     */
    suspend fun restoreFromBackup(backupPath: String): BackupResult = withContext(Dispatchers.IO) {
        try {
            val file = File(backupPath)
            if (!file.exists()) {
                return@withContext BackupResult(success = false, error = "Backup file not found")
            }
            
            val json = file.readText()
            val backupData = gson.fromJson(json, FullBackupData::class.java)
            
            // Clear existing data and restore
            restoreAllData(backupData)
            
            BackupResult(
                success = true,
                filePath = backupPath,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                success = false,
                error = e.message ?: "Unknown error restoring backup"
            )
        }
    }
    
    /**
     * Emergency restore from latest safe backup
     */
    suspend fun emergencyRestore(): BackupResult = withContext(Dispatchers.IO) {
        val lastSafePath = backupPreferences.lastSafeBackupPath.first()
        
        if (lastSafePath == null) {
            val latestBackup = getLatestBackup()
            if (latestBackup == null) {
                return@withContext BackupResult(success = false, error = "No backup available for recovery")
            }
            return@withContext restoreFromBackup(latestBackup.filePath)
        }
        
        restoreFromBackup(lastSafePath)
    }
    
    private suspend fun restoreAllData(backupData: FullBackupData) {
        // Clear all tables
        database.customerDao().deleteAllCustomers()
        database.customerTransactionDao().deleteAllTransactions()
        database.dailyBalanceDao().deleteAllBalances()
        database.dailyLedgerTransactionDao().deleteAllTransactions()
        database.dailyExpenseDao().deleteAllExpenses()
        database.tradeTransactionDao().deleteAllTransactions()
        database.companyDao().deleteAllCompanies()
        database.farmDao().deleteAllFarms()
        
        // Restore data
        backupData.customers.forEach { database.customerDao().insertCustomer(it.toEntity()) }
        backupData.customerTransactions.forEach { database.customerTransactionDao().insertTransaction(it.toEntity()) }
        backupData.dailyBalances.forEach { database.dailyBalanceDao().insertBalance(it.toEntity()) }
        backupData.dailyLedgerTransactions.forEach { database.dailyLedgerTransactionDao().insertTransaction(it.toEntity()) }
        backupData.dailyExpenses.forEach { database.dailyExpenseDao().insertExpense(it.toEntity()) }
        backupData.tradeTransactions.forEach { database.tradeTransactionDao().insertTransaction(it.toEntity()) }
        backupData.companies.forEach { database.companyDao().insertCompany(it.toEntity()) }
        backupData.farms.forEach { database.farmDao().insertFarm(it.toEntity()) }
    }
    
    // ============ GOOGLE DRIVE AUTO-SYNC ============
    
    /**
     * Auto-sync backup file to Google Drive if connected
     * Runs in background, does not block on failure
     */
    private suspend fun autoSyncToDrive(backupFile: File) {
        try {
            // Only sync if Google Drive is connected
            if (!googleDriveManager.isSignedIn()) {
                return
            }
            
            // Upload in background without blocking
            val result = googleDriveManager.uploadBackup(backupFile)
            
            // Log success/failure for debugging
            result.fold(
                onSuccess = { 
                    android.util.Log.d("BackupManager", "Auto-synced to Drive: ${backupFile.name}")
                },
                onFailure = { error ->
                    android.util.Log.w("BackupManager", "Auto-sync failed: ${error.message}")
                }
            )
        } catch (e: Exception) {
            // Silently catch errors to prevent backup failures
            android.util.Log.w("BackupManager", "Auto-sync error: ${e.message}")
        }
    }
    
    // ============ CLEANUP ============
    
    private suspend fun cleanupOldBackups() {
        val retentionCount = backupPreferences.retentionCount.first()
        
        getDailyBackupDir().listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(retentionCount)
            ?.forEach { it.delete() }
    }
    
    private fun cleanupSnapshots(keepCount: Int) {
        getSnapshotBackupDir().listFiles()
            ?.filter { it.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(keepCount)
            ?.forEach { it.delete() }
    }
    
    /**
     * Cleans up transaction backups older than retentionDays
     */
    suspend fun cleanupOldTransactionBackups(retentionDays: Int = 7) = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        
        getTransactionBackupDir().listFiles()
            ?.filter { it.extension == "json" && it.lastModified() < cutoffTime }
            ?.forEach { it.delete() }
    }
    
    // ============ CRASH RECOVERY ============
    
    /**
     * Marks app as not closed properly (call on app start)
     */
    suspend fun markAppStarted() {
        backupPreferences.setAppClosedProperly(false)
    }
    
    /**
     * Checks if app needs recovery (crashed last time)
     */
    suspend fun needsRecovery(): Boolean {
        return !backupPreferences.appLastClosedProperly.first()
    }
    
    /**
     * Checks if daily backup is needed
     */
    suspend fun needsDailyBackup(): Boolean {
        if (!backupPreferences.dailyBackupEnabled.first()) return false
        
        val lastDailyBackupDate = backupPreferences.lastDailyBackupDate.first() ?: 0
        val todayMidnight = normalizeToMidnight(System.currentTimeMillis())
        
        return lastDailyBackupDate < todayMidnight
    }
    
    // ============ UTILITIES ============
    
    private fun normalizeToMidnight(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

// ============ EXTENSION FUNCTIONS FOR ENTITY CONVERSION ============

// Entity to Backup
fun Customer.toBackup() = CustomerBackup(id, name, phone, type, email, businessName, category, openingBalance, notes, createdAt)
fun CustomerTransaction.toBackup() = CustomerTransactionBackup(id, customerId, type, amount, date, note, voiceNotePath, paymentMethod, createdAt)
fun DailyBalance.toBackup() = DailyBalanceBackup(id, date, openingCash, openingBank, closingCash, closingBank, note, createdAt)
fun DailyLedgerTransaction.toBackup() = DailyLedgerTransactionBackup(id, date, mode, amount, party, note, createdAt, customerTransactionId)
fun DailyExpense.toBackup() = DailyExpenseBackup(id, date, category, amount, paymentMethod, note, createdAt)
fun TradeTransaction.toBackup() = TradeTransactionBackup(id, farmId, entryNumber, date, deonar, type, itemName, quantity, buyRate, weight, rate, extraBonus, netWeight, fee, tds, totalAmount, profit, pricePerUnit, note, createdAt)
fun Company.toBackup() = CompanyBackup(id, name, code, address, phone, email, gstNumber, createdAt)
fun Farm.toBackup() = FarmBackup(id, name, shortCode, nextNumber, createdAt)

// Backup to Entity
fun CustomerBackup.toEntity() = Customer(id, name, phone, type, email, businessName, category, openingBalance, notes, createdAt)
fun CustomerTransactionBackup.toEntity() = CustomerTransaction(id, customerId, type, amount, date, note, voiceNotePath, paymentMethod, createdAt)
fun DailyBalanceBackup.toEntity() = DailyBalance(id, date, openingCash, openingBank, closingCash, closingBank, note, createdAt)
fun DailyLedgerTransactionBackup.toEntity() = DailyLedgerTransaction(id, date, mode, amount, party, note, createdAt, customerTransactionId)
fun DailyExpenseBackup.toEntity() = DailyExpense(id, date, category, amount, paymentMethod, note, createdAt)
fun TradeTransactionBackup.toEntity() = TradeTransaction(id, farmId, entryNumber, date, deonar, type, itemName, quantity, buyRate, weight, rate, extraBonus, netWeight, fee, tds, totalAmount, profit, pricePerUnit, note, createdAt)
fun CompanyBackup.toEntity() = Company(id, name, code, address, phone, email, gstNumber, createdAt)
fun FarmBackup.toEntity() = Farm(id, name, shortCode, nextNumber, createdAt)
