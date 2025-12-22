package com.example.tabelahisabapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.transactionDataStore: DataStore<Preferences> by preferencesDataStore(name = "transaction_preferences")

class TransactionPreferences(private val context: Context) {
    companion object {
        private val PAYMENT_METHOD_KEY = stringPreferencesKey("last_payment_method")
    }

    val lastPaymentMethod: Flow<String> = context.transactionDataStore.data.map { preferences ->
        preferences[PAYMENT_METHOD_KEY] ?: "CASH"
    }

    suspend fun setPaymentMethod(method: String) {
        context.transactionDataStore.edit { preferences ->
            preferences[PAYMENT_METHOD_KEY] = method
        }
    }
}
