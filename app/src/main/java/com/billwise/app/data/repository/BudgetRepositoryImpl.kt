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

    override fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> {
        return dao.getBudgetsForMonth(month, year).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveBudget(budget: Budget) {
        dao.upsertBudget(budget.toEntity())
    }

    override suspend fun getBudgetByCategory(category: String, month: Int, year: Int): Budget? {
        return dao.getBudgetByCategory(category, month, year)?.toDomain()
    }

    private fun BudgetEntity.toDomain() = Budget(
        id = id,
        category = category,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year,
        hasNotified75 = hasNotified75,
        hasNotified100 = hasNotified100,
        lastMonthSpend = lastMonthSpend
    )

    private fun Budget.toEntity() = BudgetEntity(
        id = id,
        category = category,
        monthlyLimit = monthlyLimit,
        month = month,
        year = year,
        hasNotified75 = hasNotified75,
        hasNotified100 = hasNotified100,
        lastMonthSpend = lastMonthSpend
    )
}
