package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CashbackViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CashbackDatabase.getDatabase(application, viewModelScope)
    private val repository = CashbackRepository(database.cashbackDao())

    val allBanks: StateFlow<List<Bank>> = repository.allBanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCashbacks: StateFlow<List<CashbackWithBank>> = repository.allCashbacksWithBank
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI filters and search state
    val selectedBankFilter = MutableStateFlow<Bank?>(null)
    val searchQuery = MutableStateFlow("")

    // Filtered cashbacks matching user's search query or selected bank
    val filteredCashbacks: StateFlow<List<CashbackWithBank>> = combine(
        allCashbacks,
        searchQuery,
        selectedBankFilter
    ) { cashbacks, query, bankFilter ->
        cashbacks.filter { cb ->
            val matchesBank = bankFilter == null || cb.bankId == bankFilter.id
            val matchesQuery = query.isEmpty() || cb.category.contains(query, ignoreCase = true)
            matchesBank && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category lists helper with corresponding icons
    val presetCategories = listOf(
        CategoryPreset("Все покупки", "percent"),
        CategoryPreset("Супермаркеты", "shopping_cart"),
        CategoryPreset("Рестораны", "restaurant"),
        CategoryPreset("Автоуслуги", "directions_car"),
        CategoryPreset("Такси", "local_taxi"),
        CategoryPreset("Аптеки", "local_pharmacy"),
        CategoryPreset("Одежда и Обувь", "checkroom"),
        CategoryPreset("Электроника", "devices"),
        CategoryPreset("Топливо и АЗС", "local_gas_station"),
        CategoryPreset("Развлечения", "theater_comedy"),
        CategoryPreset("Кино и искусство", "movie"),
        CategoryPreset("Красота и Спа", "spa"),
        CategoryPreset("Дом и ремонт", "construction"),
        CategoryPreset("Книги и хобби", "menu_book"),
        CategoryPreset("Спорттовары", "sports_soccer"),
        CategoryPreset("Зоотовары", "pets"),
        CategoryPreset("Цветы", "redeem"),
        CategoryPreset("Авиабилеты", "flight")
    )

    fun addBank(name: String, colorHex: String, iconEmoji: String) {
        viewModelScope.launch {
            repository.insertBank(Bank(name = name, colorHex = colorHex, iconEmoji = iconEmoji))
        }
    }

    fun removeBank(bankId: Long) {
        viewModelScope.launch {
            repository.deleteBank(bankId)
        }
    }

    fun addCashback(bankId: Long, category: String, percentage: Double, iconName: String) {
        viewModelScope.launch {
            repository.insertCashback(
                Cashback(
                    bankId = bankId,
                    category = category,
                    percentage = percentage,
                    iconName = iconName
                )
            )
        }
    }

    fun updateCashback(cashbackId: Long, bankId: Long, category: String, percentage: Double, iconName: String) {
        viewModelScope.launch {
            repository.updateCashback(
                Cashback(
                    id = cashbackId,
                    bankId = bankId,
                    category = category,
                    percentage = percentage,
                    iconName = iconName
                )
            )
        }
    }

    fun removeCashback(cashbackId: Long) {
        viewModelScope.launch {
            repository.deleteCashback(cashbackId)
        }
    }
}

data class CategoryPreset(val name: String, val iconName: String)
