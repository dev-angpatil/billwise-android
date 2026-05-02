package com.billwise.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE category = :category AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetByCategory(category: String, month: Int, year: Int): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET hasNotified75 = :notified WHERE id = :id")
    suspend fun updateNotified75(id: Long, notified: Boolean)

    @Query("UPDATE budgets SET hasNotified100 = :notified WHERE id = :id")
    suspend fun updateNotified100(id: Long, notified: Boolean)
}
