package com.billwise.app.domain.repository

import com.billwise.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudget(): Flow<Budget?>
    suspend fun saveBudget(budget: Budget)
}
