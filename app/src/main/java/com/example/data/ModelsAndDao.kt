package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "banks")
data class Bank(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconEmoji: String
)

@Entity(
    tableName = "cashbacks",
    foreignKeys = [
        ForeignKey(
            entity = Bank::class,
            parentColumns = ["id"],
            childColumns = ["bankId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bankId"])]
)
data class Cashback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankId: Long,
    val category: String,
    val percentage: Double,
    val iconName: String = "shopping_bag" // Store helper indicator
)

// Data class to represent Cashback with its Bank info for easy querying
data class CashbackWithBank(
    val id: Long,
    val bankId: Long,
    val bankName: String,
    val bankColorHex: String,
    val bankIconEmoji: String,
    val category: String,
    val percentage: Double,
    val iconName: String
)

@Dao
interface CashbackDao {
    @Query("SELECT * FROM banks ORDER BY name ASC")
    fun getAllBanks(): Flow<List<Bank>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBank(bank: Bank): Long

    @Query("DELETE FROM banks WHERE id = :bankId")
    suspend fun deleteBank(bankId: Long)

    @Query("SELECT * FROM cashbacks WHERE bankId = :bankId")
    fun getCashbacksByBank(bankId: Long): Flow<List<Cashback>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashback(cashback: Cashback): Long

    @Update
    suspend fun updateCashback(cashback: Cashback)

    @Query("DELETE FROM cashbacks WHERE id = :cashbackId")
    suspend fun deleteCashback(cashbackId: Long)

    @Query("""
        SELECT 
            c.id, 
            c.bankId, 
            b.name AS bankName, 
            b.colorHex AS bankColorHex, 
            b.iconEmoji AS bankIconEmoji, 
            c.category, 
            c.percentage, 
            c.iconName
        FROM cashbacks c
        INNER JOIN banks b ON c.bankId = b.id
        ORDER BY c.percentage DESC
    """)
    fun getAllCashbacksWithBank(): Flow<List<CashbackWithBank>>
    
    @Query("""
        SELECT 
            c.id, 
            c.bankId, 
            b.name AS bankName, 
            b.colorHex AS bankColorHex, 
            b.iconEmoji AS bankIconEmoji, 
            c.category, 
            c.percentage, 
            c.iconName
        FROM cashbacks c
        INNER JOIN banks b ON c.bankId = b.id
        WHERE c.bankId = :bankId
        ORDER BY c.percentage DESC
    """)
    fun getDetailedCashbacksByBank(bankId: Long): Flow<List<CashbackWithBank>>
}
