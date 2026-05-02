package com.billwise.app.domain.repository

import com.billwise.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>
    suspend fun saveBudget(budget: Budget)
    suspend fun getBudgetByCategory(category: String, month: Int, year: Int): Budget?
}
