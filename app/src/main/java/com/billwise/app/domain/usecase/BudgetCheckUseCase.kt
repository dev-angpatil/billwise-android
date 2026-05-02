package com.billwise.app.domain.usecase

import com.billwise.app.core.NotificationHelper
import com.billwise.app.data.local.BudgetDao
import com.billwise.app.data.local.BudgetEntity
import com.billwise.app.data.local.TransactionDao
import com.billwise.app.domain.model.TransactionType
import java.util.*

class BudgetCheckUseCase(
    private val budgetDao: BudgetDao,
    private val transactionDao: TransactionDao,
    private val notificationHelper: NotificationHelper
) {

    suspend operator fun invoke(category: String) {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH) + 1
        val year = cal.get(Calendar.YEAR)

        // Get start and end of month
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startTime = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endTime = cal.timeInMillis

        // Get all transactions for this month in this category
        val transactions = transactionDao.getTransactionsInRange(startTime, endTime)
        val categoryTransactions = transactions.filter { 
            it.category.equals(category, ignoreCase = true) && it.type == TransactionType.DEBIT 
        }
        val spent = categoryTransactions.sumOf { it.amount }

        // Get budget
        val budget = budgetDao.getBudgetByCategory(category, month, year)
        
        if (budget != null) {
            checkAndNotify(budget, spent)
        } else {
            // Smart Default: 20% over last month's spend
            handleSmartDefault(category, spent, month, year)
        }
    }

    private suspend fun checkAndNotify(budget: BudgetEntity, spent: Double) {
        val limit = budget.monthlyLimit
        if (spent >= limit && !budget.hasNotified100) {
            notificationHelper.sendBudgetAlert(budget.category, spent, limit, true)
            budgetDao.updateNotified100(budget.id, true)
        } else if (spent >= limit * 0.75 && !budget.hasNotified75 && !budget.hasNotified100) {
            notificationHelper.sendBudgetAlert(budget.category, spent, limit, false)
            budgetDao.updateNotified75(budget.id, true)
        }
    }

    private suspend fun handleSmartDefault(category: String, spent: Double, month: Int, year: Int) {
        // Calculate last month's time range
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val startLM = cal.timeInMillis
        
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endLM = cal.timeInMillis

        // Get actual spend from last month
        val lastMonthSpend = transactionDao.getCategorySpend(category, startLM, endLM) ?: 0.0
        
        // Smart Default: 120% of last month's spend
        val smartLimit = if (lastMonthSpend > 0) lastMonthSpend * 1.2 else 0.0

        if (smartLimit > 0) {
            if (spent >= smartLimit) {
                notificationHelper.sendBudgetAlert(category, spent, smartLimit, true)
            } else if (spent >= smartLimit * 0.75) {
                notificationHelper.sendBudgetAlert(category, spent, smartLimit, false)
            }
        }
    }
}
