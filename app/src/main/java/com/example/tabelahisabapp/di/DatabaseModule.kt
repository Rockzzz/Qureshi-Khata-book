package com.example.tabelahisabapp.di

import android.content.Context
import androidx.room.Room
import com.example.tabelahisabapp.data.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2, 
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()
    }

    @Provides
    fun provideCustomerDao(appDatabase: AppDatabase) = appDatabase.customerDao()

    @Provides
    fun provideCustomerTransactionDao(appDatabase: AppDatabase) = appDatabase.customerTransactionDao()

    @Provides
    fun provideDailyBalanceDao(appDatabase: AppDatabase) = appDatabase.dailyBalanceDao()

    @Provides
    fun provideTradeTransactionDao(appDatabase: AppDatabase) = appDatabase.tradeTransactionDao()
    
    @Provides
    fun provideCompanyDao(appDatabase: AppDatabase) = appDatabase.companyDao()

    @Provides
    fun provideDailyExpenseDao(appDatabase: AppDatabase) = appDatabase.dailyExpenseDao()

    @Provides
    fun provideDailyLedgerTransactionDao(appDatabase: AppDatabase) = appDatabase.dailyLedgerTransactionDao()
    
    @Provides
    fun provideFarmDao(appDatabase: AppDatabase) = appDatabase.farmDao()
}
