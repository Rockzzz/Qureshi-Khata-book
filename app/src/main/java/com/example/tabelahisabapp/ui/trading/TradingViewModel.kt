package com.example.tabelahisabapp.ui.trading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.dao.FarmDao
import com.example.tabelahisabapp.data.db.entity.Farm
import com.example.tabelahisabapp.data.db.entity.TradeTransaction
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class TradingViewModel @Inject constructor(
    private val repository: MainRepository,
    private val farmDao: FarmDao
) : ViewModel() {

    fun getCurrentMonthRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.add(Calendar.MONTH, 1)
        val end = calendar.timeInMillis
        
        return Pair(start, end)
    }

    val allTrades = repository.getAllTrades()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFarms = farmDao.getAllFarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val thisMonthProfit = run {
        val (start, end) = getCurrentMonthRange()
        repository.getMonthlyProfitSummary(start, end)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

    val overallProfit = repository.getOverallProfitSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun getTradesByTypeFlow(type: String) = repository.getTradesByType(type)
    
    fun getTradesForFarm(farmId: Int): List<TradeTransaction> {
        return allTrades.value.filter { it.farmId == farmId }
    }

    fun addFarm(name: String, shortCode: String) {
        viewModelScope.launch {
            val farm = Farm(
                name = name,
                shortCode = shortCode.uppercase(),
                nextNumber = 1,
                createdAt = System.currentTimeMillis()
            )
            farmDao.insertOrUpdateFarm(farm)
        }
    }
    
    fun deleteFarm(farm: Farm) {
        viewModelScope.launch {
            farmDao.deleteFarm(farm)
        }
    }
    
    fun updateFarm(farm: Farm) {
        viewModelScope.launch {
            farmDao.insertOrUpdateFarm(farm)
        }
    }

    fun saveTrade(
        type: String,
        deonar: String? = null,
        itemName: String,
        quantity: Int,
        buyRate: Double,
        weight: Double? = null,
        rate: Double? = null,
        extraBonus: Double? = null,
        netWeight: Double? = null,
        fee: Double? = null,
        tds: Double? = null,
        totalAmount: Double,
        profit: Double? = null,
        pricePerUnit: Double,
        date: Long,
        note: String?,
        transactionId: Int? = null,
        originalCreatedAt: Long? = null,
        farmId: Int? = null,
        entryNumber: String? = null
    ) {
        viewModelScope.launch {
            val createdAt = originalCreatedAt ?: System.currentTimeMillis()
            
            val trade = TradeTransaction(
                id = transactionId ?: 0,
                farmId = farmId,
                entryNumber = entryNumber,
                date = date,
                deonar = deonar,
                type = type,
                itemName = itemName,
                quantity = quantity,
                buyRate = buyRate,
                weight = weight,
                rate = rate,
                extraBonus = extraBonus,
                netWeight = netWeight,
                fee = fee,
                tds = tds,
                totalAmount = totalAmount,
                profit = profit,
                pricePerUnit = pricePerUnit,
                note = note,
                createdAt = createdAt
            )
            repository.insertOrUpdateTrade(trade)
        }
    }

    fun deleteTrade(trade: TradeTransaction) {
        viewModelScope.launch {
            repository.deleteTrade(trade)
        }
    }

    fun getUniqueItemNames(): List<String> {
        return allTrades.value.map { it.itemName }.distinct().sorted()
    }
    
    /**
     * Refresh function for pull-to-refresh
     * Room StateFlows auto-update when data changes
     */
    fun refresh() {
        viewModelScope.launch {
            // Room StateFlows auto-update when data changes
            // This just forces a recomposition
        }
    }
}

