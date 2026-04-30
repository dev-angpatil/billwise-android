package com.billwise.app.data.repository

import com.billwise.app.data.local.BudgetDao
import com.billwise.app.data.local.BudgetEntity
import com.billwise.app.domain.model.Budget
import com.billwise.app.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BudgetRepositoryImpl(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun getBudget(): Flow<Budget?> {
        return dao.getBudget().map { it?.toDomain() }
    }

    override suspend fun saveBudget(budget: Budget) {
        dao.upsertBudget(budget.toEntity())
    }

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year
    )
}
