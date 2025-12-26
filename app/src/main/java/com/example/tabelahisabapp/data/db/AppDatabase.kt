package com.example.tabelahisabapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tabelahisabapp.data.db.dao.*
import com.example.tabelahisabapp.data.db.entity.*

@Database(
    entities = [
        Customer::class,
        CustomerTransaction::class,
        DailyBalance::class,
        TradeTransaction::class,
        Company::class,
        DailyExpense::class,
        DailyLedgerTransaction::class,
        Farm::class,
        ExpenseCategory::class
    ],
    version = 9,  // Updated from 8 to 9
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun customerTransactionDao(): CustomerTransactionDao
    abstract fun dailyBalanceDao(): DailyBalanceDao
    abstract fun tradeTransactionDao(): TradeTransactionDao
    abstract fun companyDao(): CompanyDao
    abstract fun dailyExpenseDao(): DailyExpenseDao
    abstract fun dailyLedgerTransactionDao(): DailyLedgerTransactionDao
    abstract fun farmDao(): FarmDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao

    companion object {
        const val DATABASE_NAME = "udhaar_ledger.db"
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 1 → 2: Added customer type field
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN type TEXT NOT NULL DEFAULT 'CUSTOMER'")
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 2 → 3: Added trade transaction fields and companies table
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE trade_transactions ADD COLUMN deonar TEXT")
                    db.execSQL("ALTER TABLE trade_transactions ADD COLUMN buyRate REAL")
                    db.execSQL("ALTER TABLE trade_transactions ADD COLUMN weight REAL")
                    db.execSQL("ALTER TABLE trade_transactions ADD COLUMN rate REAL")
                    db.execSQL("ALTER TABLE trade_transactions ADD COLUMN extraBonus REAL")
                    db.execSQL("ALTER TABLE trade_transactions ADD COLUMN profit REAL")
                    db.execSQL("UPDATE trade_transactions SET buyRate = pricePerUnit WHERE buyRate IS NULL")
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS companies (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            code TEXT,
                            address TEXT,
                            phone TEXT,
                            email TEXT,
                            gstNumber TEXT,
                            createdAt INTEGER NOT NULL
                        )
                    """.trimIndent())
                } catch (e: Exception) { e.printStackTrace() }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 3 → 4: Added daily_expenses table
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_expenses (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        category TEXT NOT NULL,
                        amount REAL NOT NULL,
                        paymentMethod TEXT NOT NULL DEFAULT 'CASH',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_expenses_date ON daily_expenses (date)")
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 4 → 5: Added daily_ledger_transactions table
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_ledger_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date INTEGER NOT NULL,
                        mode TEXT NOT NULL,
                        amount REAL NOT NULL,
                        party TEXT,
                        note TEXT,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_ledger_transactions_date ON daily_ledger_transactions (date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_ledger_transactions_date_created ON daily_ledger_transactions (date, createdAt)")
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 5 → 6: Added farms table and farmId to trade_transactions
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create farms table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS farms (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        shortCode TEXT NOT NULL,
                        nextNumber INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Add farmId and entryNumber to trade_transactions
                db.execSQL("ALTER TABLE trade_transactions ADD COLUMN farmId INTEGER")
                db.execSQL("ALTER TABLE trade_transactions ADD COLUMN entryNumber TEXT")
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 6 → 7: PRD Redesign - Enhanced Customer, DailyLedger, Expense
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ─────────────────────────────────────────────────────────────────
                // 1. Enhance customers table for unified Customer/Supplier model
                // ─────────────────────────────────────────────────────────────────
                db.execSQL("ALTER TABLE customers ADD COLUMN email TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN businessName TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN category TEXT")
                db.execSQL("ALTER TABLE customers ADD COLUMN openingBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE customers ADD COLUMN notes TEXT")
                
                // ─────────────────────────────────────────────────────────────────
                // 2. Add source tracking to daily_ledger_transactions for sync
                // ─────────────────────────────────────────────────────────────────
                db.execSQL("ALTER TABLE daily_ledger_transactions ADD COLUMN customerTransactionId INTEGER")
                db.execSQL("ALTER TABLE daily_ledger_transactions ADD COLUMN sourceType TEXT")
                db.execSQL("ALTER TABLE daily_ledger_transactions ADD COLUMN sourceId INTEGER")
                
                // Create index for faster source lookups
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_ledger_source ON daily_ledger_transactions (sourceType, sourceId)")
                
                // ─────────────────────────────────────────────────────────────────
                // 3. Add note field to daily_expenses
                // ─────────────────────────────────────────────────────────────────
                db.execSQL("ALTER TABLE daily_expenses ADD COLUMN note TEXT")
                
                // Create category index for filtering
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_expenses_category ON daily_expenses (category)")
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 7 → 8: Added expense_categories table
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS expense_categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        iconName TEXT,
                        colorHex TEXT,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // ═══════════════════════════════════════════════════════════════════════
        // MIGRATION 8 → 9: Added Allana/Al Qureshi specific fields to trade_transactions
        // ═══════════════════════════════════════════════════════════════════════
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add netWeight, fee, and tds columns for Allana/Al Qureshi trades
                db.execSQL("ALTER TABLE trade_transactions ADD COLUMN netWeight REAL")
                db.execSQL("ALTER TABLE trade_transactions ADD COLUMN fee REAL")
                db.execSQL("ALTER TABLE trade_transactions ADD COLUMN tds REAL")
            }
        }
        
        // List of all migrations for easy access
        val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9
        )
    }
}

