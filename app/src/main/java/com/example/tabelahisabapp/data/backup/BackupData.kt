package com.example.tabelahisabapp.data.backup

/**
 * Data classes for backup JSON structure
 * Uses simple data classes that mirror database entities for clean JSON serialization
 */

// Backup result wrapper
data class BackupResult(
    val success: Boolean,
    val filePath: String? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Backup info for listing available backups
data class BackupInfo(
    val id: String,
    val fileName: String,
    val filePath: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val type: BackupType,
    val customerCount: Int = 0,
    val transactionCount: Int = 0
)

enum class BackupType {
    FULL_DAILY,           // Daily full backup
    TRANSACTION,          // Single transaction backup
    QUICK_SNAPSHOT,       // App close/crash snapshot
    EMERGENCY             // Recovery backup
}

// Full backup data structure (mirrors all database tables)
data class FullBackupData(
    val version: Int = 1,
    val timestamp: Long,
    val appVersion: String = "1.0",
    val customers: List<CustomerBackup>,
    val customerTransactions: List<CustomerTransactionBackup>,
    val dailyBalances: List<DailyBalanceBackup>,
    val dailyLedgerTransactions: List<DailyLedgerTransactionBackup>,
    val dailyExpenses: List<DailyExpenseBackup>,
    val tradeTransactions: List<TradeTransactionBackup>,
    val companies: List<CompanyBackup>,
    val farms: List<FarmBackup>
)

// Individual entity backup classes (plain data, no Room annotations)
data class CustomerBackup(
    val id: Int,
    val name: String,
    val phone: String?,
    val type: String,
    val email: String? = null,
    val businessName: String? = null,
    val category: String? = null,
    val openingBalance: Double = 0.0,
    val notes: String? = null,
    val createdAt: Long
)

data class CustomerTransactionBackup(
    val id: Int,
    val customerId: Int,
    val type: String,
    val amount: Double,
    val date: Long,
    val note: String?,
    val voiceNotePath: String?,
    val paymentMethod: String,
    val createdAt: Long
)

data class DailyBalanceBackup(
    val id: Int,
    val date: Long,
    val openingCash: Double,
    val openingBank: Double,
    val closingCash: Double,
    val closingBank: Double,
    val note: String?,
    val createdAt: Long
)

data class DailyLedgerTransactionBackup(
    val id: Int,
    val date: Long,
    val mode: String,
    val amount: Double,
    val party: String?,
    val note: String?,
    val createdAt: Long,
    val customerTransactionId: Int?
)

data class DailyExpenseBackup(
    val id: Int,
    val date: Long,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val note: String? = null,
    val createdAt: Long
)

data class TradeTransactionBackup(
    val id: Int,
    val farmId: Int?,
    val entryNumber: String?,
    val date: Long,
    val deonar: String?,
    val type: String,
    val itemName: String,
    val quantity: Int,
    val buyRate: Double,
    val weight: Double?,
    val rate: Double?,
    val extraBonus: Double?,
    val netWeight: Double? = null,
    val fee: Double? = null,
    val tds: Double? = null,
    val totalAmount: Double,
    val profit: Double?,
    val pricePerUnit: Double,
    val note: String?,
    val createdAt: Long
)

data class CompanyBackup(
    val id: Int,
    val name: String,
    val code: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val gstNumber: String?,
    val createdAt: Long
)

data class FarmBackup(
    val id: Int,
    val name: String,
    val shortCode: String,
    val nextNumber: Int,
    val createdAt: Long
)

// Transaction-level backup (for incremental backups)
data class TransactionBackupData(
    val id: String,
    val type: String, // CREDIT, DEBIT, CASH_IN, CASH_OUT, etc.
    val amount: Double,
    val from: String?,
    val to: String?,
    val timestamp: Long,
    val voiceInput: String?,
    val paymentMethod: String = "CASH"
)
