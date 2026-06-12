package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Bank::class, Cashback::class], version = 1, exportSchema = false)
abstract class CashbackDatabase : RoomDatabase() {
    abstract fun cashbackDao(): CashbackDao

    companion object {
        @Volatile
        private var INSTANCE: CashbackDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): CashbackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CashbackDatabase::class.java,
                    "cashback_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.cashbackDao())
                }
            }
        }

        suspend fun populateDatabase(dao: CashbackDao) {
            // Emptied on user request to start with a clean state without any pre-defined bank/cashback entries.
        }
    }
}
