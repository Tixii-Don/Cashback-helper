package com.example.data

import kotlinx.coroutines.flow.Flow

class CashbackRepository(private val cashbackDao: CashbackDao) {
    val allBanks: Flow<List<Bank>> = cashbackDao.getAllBanks()
    val allCashbacksWithBank: Flow<List<CashbackWithBank>> = cashbackDao.getAllCashbacksWithBank()

    fun getCashbacksByBank(bankId: Long): Flow<List<Cashback>> = 
        cashbackDao.getCashbacksByBank(bankId)

    fun getDetailedCashbacksByBank(bankId: Long): Flow<List<CashbackWithBank>> = 
        cashbackDao.getDetailedCashbacksByBank(bankId)

    suspend fun insertBank(bank: Bank): Long = cashbackDao.insertBank(bank)
    
    suspend fun deleteBank(bankId: Long) = cashbackDao.deleteBank(bankId)

    suspend fun insertCashback(cashback: Cashback): Long = cashbackDao.insertCashback(cashback)
    
    suspend fun updateCashback(cashback: Cashback) = cashbackDao.updateCashback(cashback)
    
    suspend fun deleteCashback(cashbackId: Long) = cashbackDao.deleteCashback(cashbackId)
}
