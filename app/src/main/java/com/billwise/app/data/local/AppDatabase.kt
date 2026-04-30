package com.billwise.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TransactionEntity::class, BillEntity::class, BudgetEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun billDao(): BillDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `budgets` (
                        `id` INTEGER NOT NULL DEFAULT 1,
                        `monthlyLimit` REAL NOT NULL,
                        `month` INTEGER NOT NULL,
                        `year` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )"""
                )
            }
        }
    }
}

