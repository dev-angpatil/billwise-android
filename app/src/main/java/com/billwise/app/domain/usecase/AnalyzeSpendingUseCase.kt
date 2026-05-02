package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit

class AnalyzeSpendingUseCase {

    fun getTotalSpent(transactions: List<Transaction>): Double {
        return transactions.filter { !it.isIgnored && it.type == TransactionType.DEBIT }.sumOf { it.amount }
    }

    fun getTotalSpentInMonth(transactions: List<Transaction>, month: Int, year: Int): Double {
        return transactions
            .filter { !it.isIgnored && it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
            .sumOf { it.amount }
    }

    fun getTotalIncomeInMonth(transactions: List<Transaction>, month: Int, year: Int): Double {
        return transactions
            .filter { !it.isIgnored && it.type == TransactionType.CREDIT && isSameMonthYear(it.datetime, month, year) }
            .sumOf { it.amount }
    }

    fun getCategoryBreakdownForMonth(
        transactions: List<Transaction>,
        month: Int,
        year: Int
    ): Map<String, Double> {
        return transactions
            .filter { !it.isIgnored && it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
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
            .filter { !it.isIgnored && it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
            .groupBy { tx ->
                cal.timeInMillis = tx.datetime
                cal.get(Calendar.DAY_OF_MONTH)
            }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    fun getMonthlySpendTrend(transactions: List<Transaction>): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val m = c.get(Calendar.MONTH) + 1
            val y = c.get(Calendar.YEAR)
            val name = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault()) ?: "???"
            result[name] = getTotalSpentInMonth(transactions, m, y)
        }
        return result
    }

    fun getTopMerchants(transactions: List<Transaction>, month: Int, year: Int, limit: Int = 5): List<Pair<String, Double>> {
        return transactions
            .filter { !it.isIgnored && it.type == TransactionType.DEBIT && isSameMonthYear(it.datetime, month, year) }
            .groupBy { it.merchantAlias ?: it.merchant }
            .mapValues { it.value.sumOf { txn -> txn.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    fun getWeeklyComparison(transactions: List<Transaction>): Pair<Double, Double> {
        val now = System.currentTimeMillis()
        val oneWeekMs = TimeUnit.DAYS.toMillis(7)
        
        val thisWeek = transactions.filter { 
            !it.isIgnored && it.type == TransactionType.DEBIT && it.datetime > (now - oneWeekMs) 
        }.sumOf { it.amount }
        
        val lastWeek = transactions.filter { 
            !it.isIgnored && it.type == TransactionType.DEBIT && 
            it.datetime <= (now - oneWeekMs) && it.datetime > (now - 2 * oneWeekMs)
        }.sumOf { it.amount }
        
        return thisWeek to lastWeek
    }

    fun getIncomeVsExpenseTrend(transactions: List<Transaction>): List<Triple<String, Double, Double>> {
        val result = mutableListOf<Triple<String, Double, Double>>()
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val m = c.get(Calendar.MONTH) + 1
            val y = c.get(Calendar.YEAR)
            val name = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, java.util.Locale.getDefault()) ?: ""
            val expense = getTotalSpentInMonth(transactions, m, y)
            val income = getTotalIncomeInMonth(transactions, m, y)
            result.add(Triple(name, income, expense))
        }
        return result
    }

    fun getAverageDailySpend(transactions: List<Transaction>, month: Int, year: Int): Double {
        val spent = getTotalSpentInMonth(transactions, month, year)
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        
        val daysPassed = if (month == currentMonth && year == currentYear) {
            cal.get(Calendar.DAY_OF_MONTH)
        } else {
            cal.set(year, month - 1, 1)
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        return if (daysPassed > 0) spent / daysPassed else 0.0
    }

    fun getProjectedMonthEnd(transactions: List<Transaction>, month: Int, year: Int): Double {
        val avg = getAverageDailySpend(transactions, month, year)
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return avg * totalDays
    }

    fun getCategoryAveragePastMonths(transactions: List<Transaction>, category: String, numMonths: Int = 3): Double {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear = cal.get(Calendar.YEAR)
        
        var total = 0.0
        var count = 0
        
        for (i in 1..numMonths) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val m = c.get(Calendar.MONTH) + 1
            val y = c.get(Calendar.YEAR)
            
            val monthSpent = transactions
                .filter { !it.isIgnored && it.type == TransactionType.DEBIT && it.category == category && isSameMonthYear(it.datetime, m, y) }
                .sumOf { it.amount }
            
            if (monthSpent > 0) {
                total += monthSpent
                count++
            }
        }
        return if (count > 0) total / count else 0.0
    }

    fun getDailyBudget(totalBudget: Double, month: Int, year: Int): Double {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return if (totalDays > 0) totalBudget / totalDays else 0.0
    }

    private fun isSameMonthYear(timestamp: Long, month: Int, year: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.MONTH) + 1 == month && cal.get(Calendar.YEAR) == year
    }
}
