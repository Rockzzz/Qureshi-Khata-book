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
        Farm::class
    ],
    version = 6,
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

    companion object {
        const val DATABASE_NAME = "udhaar_ledger.db"
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE customers ADD COLUMN type TEXT NOT NULL DEFAULT 'CUSTOMER'")
            }
        }
        
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
    }
}

