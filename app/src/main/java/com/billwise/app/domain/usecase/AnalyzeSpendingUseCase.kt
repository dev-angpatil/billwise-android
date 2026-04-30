package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.Calendar

class AnalyzeSpendingUseCase {

    fun getTotalSpent(transactions: List<Transaction>): Double {
        return transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    }

    fun getTotalSpentInMonth(transactions: List<Transaction>, month: Int, year: Int): Double {
        return transactions
            .filter { it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
            .sumOf { it.amount }
    }

    fun getTotalIncomeInMonth(transactions: List<Transaction>, month: Int, year: Int): Double {
        return transactions
            .filter { it.type == TransactionType.CREDIT && isSameMonthYear(it.datetime, month, year) }
            .sumOf { it.amount }
    }

    fun getCategoryBreakdown(transactions: List<Transaction>): Map<String, Double> {
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    fun getCategoryBreakdownForMonth(
        transactions: List<Transaction>,
        month: Int,
        year: Int
    ): Map<String, Double> {
        return transactions
            .filter { it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    fun getDailySpendForMonth(
        transactions: List<Transaction>,
        month: Int,
        year: Int
    ): Map<Int, Double> {
        val cal = Calendar.getInstance()
        return transactions
            .filter { it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
            .groupBy { tx ->
                cal.timeInMillis = tx.datetime
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    private fun isSameMonthYear(timestamp: Long, month: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.YEAR) == year
    }
}

