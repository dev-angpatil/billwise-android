package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.Calendar

class GenerateInsightsUseCase {

    fun getInsights(transactions: List<Transaction>, budgetLimit: Double? = null): List<String> {
        if (transactions.isEmpty()) return listOf("No transactions yet. Add some to get insights!")

        val insights = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentYear  = cal.get(Calendar.YEAR)

        val debits = transactions.filter {
            !it.isIgnored && it.type == TransactionType.DEBIT &&
                run {
                    cal.timeInMillis = it.datetime
                    cal.get(Calendar.MONTH) + 1 == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
        }

        val totalSpent = debits.sumOf { it.amount }

        if (totalSpent > 0) {
            insights.add("💸 You've spent ₹${String.format("%,.2f", totalSpent)} this month.")
        }

        // Budget alert
        if (budgetLimit != null && budgetLimit > 0 && totalSpent > 0) {
            val pct = (totalSpent / budgetLimit * 100).toInt()
            when {
                totalSpent >= budgetLimit ->
                    insights.add("🚨 You've exceeded your ₹${String.format("%,.0f", budgetLimit)} budget by ₹${String.format("%,.2f", totalSpent - budgetLimit)}!")
                pct >= 80 ->
                    insights.add("⚠️ You've used ${pct}% of your ₹${String.format("%,.0f", budgetLimit)} monthly budget.")
            }
        }

        // Top category
        val categoryBreakdown = debits.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
        val topCategory = categoryBreakdown.maxByOrNull { it.value }
        if (topCategory != null && totalSpent > 0) {
            val pct = (topCategory.value / totalSpent * 100).toInt()
            insights.add("📊 ${topCategory.key} is your biggest spend at ${pct}% (₹${String.format("%,.2f", topCategory.value)}).")
        }

        // Food alert
        val foodSpent = categoryBreakdown["Food"] ?: 0.0
        if (foodSpent > 5000) {
            insights.add("🍕 Your food spending (₹${String.format("%,.2f", foodSpent)}) is quite high this month.")
        }

        // Average daily spend
        val uniqueDays = debits.map {
            cal.timeInMillis = it.datetime
            cal.get(Calendar.DAY_OF_MONTH)
        }.toSet().size
        if (uniqueDays > 0) {
            val avgDaily = totalSpent / uniqueDays
            insights.add("📅 You spend an average of ₹${String.format("%,.2f", avgDaily)} per day.")
        }

        // Biggest single transaction
        val biggestTx = debits.maxByOrNull { it.amount }
        if (biggestTx != null) {
            insights.add("🏆 Biggest purchase: ₹${String.format("%,.2f", biggestTx.amount)} at ${biggestTx.merchantAlias ?: biggestTx.merchant}.")
        }

        // Recurring subscription check
        val recurring = debits.groupBy { it.merchantAlias ?: it.merchant }
            .filter { it.value.size >= 2 && it.value.all { tx -> tx.amount == it.value.first().amount } }
        if (recurring.isNotEmpty()) {
            insights.add("🔄 You have potential recurring payments for: ${recurring.keys.joinToString(", ")}.")
        }

        // Safe daily spend to hit budget
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH) + 1 == currentMonth && today.get(Calendar.YEAR) == currentYear) {
            val daysInMonth = today.getActualMaximum(Calendar.DAY_OF_MONTH)
            val dayOfMonth = today.get(Calendar.DAY_OF_MONTH)
            val daysRemaining = daysInMonth - dayOfMonth + 1
            if (budgetLimit != null && budgetLimit > totalSpent && daysRemaining > 0) {
                val safeDailySpend = (budgetLimit - totalSpent) / daysRemaining
                insights.add("💡 To stay within budget, aim to spend no more than ₹${String.format("%,.2f", safeDailySpend)} per day.")
            } else if (budgetLimit != null && budgetLimit <= totalSpent) {
                insights.add("🚫 You have 0 safe daily spend left. Try to minimize expenses.")
            }
        }

        // Frequent Merchant (by count)
        val merchantCounts = debits.groupBy { it.merchantAlias ?: it.merchant }.mapValues { it.value.size }
        val mostFrequent = merchantCounts.maxByOrNull { it.value }
        if (mostFrequent != null && mostFrequent.value >= 3) {
            insights.add("🏪 You shop frequently at ${mostFrequent.key} (${mostFrequent.value} times this month).")
        }

        // Weekend vs Weekday spending
        var weekendSpend = 0.0
        var weekdaySpend = 0.0
        debits.forEach { tx ->
            cal.timeInMillis = tx.datetime
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                weekendSpend += tx.amount
            } else {
                weekdaySpend += tx.amount
            }
        }
        if (weekendSpend > weekdaySpend && weekendSpend > 0) {
            insights.add("🎉 You're a Weekend Spender! (₹${String.format("%,.2f", weekendSpend)} on weekends vs ₹${String.format("%,.2f", weekdaySpend)} on weekdays).")
        }

        // Late Night Spender
        var lateNightSpend = 0.0
        debits.forEach { tx ->
            cal.timeInMillis = tx.datetime
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour >= 22 || hour <= 4) {
                lateNightSpend += tx.amount
            }
        }
        if (lateNightSpend > 2000) {
            insights.add("🌙 Night Owl Alert! You've spent ₹${String.format("%,.2f", lateNightSpend)} between 10 PM and 4 AM.")
        }

        // Income vs Expense Ratio
        val credits = transactions.filter {
            !it.isIgnored && it.type == TransactionType.CREDIT &&
                run {
                    cal.timeInMillis = it.datetime
                    cal.get(Calendar.MONTH) + 1 == currentMonth && cal.get(Calendar.YEAR) == currentYear
                }
        }
        val totalIncome = credits.sumOf { it.amount }
        if (totalIncome > 0 && totalSpent > 0) {
            val saved = totalIncome - totalSpent
            if (saved > 0) {
                val savingRate = (saved / totalIncome * 100).toInt()
                insights.add("📈 Great job! You've saved ${savingRate}% of your income (₹${String.format("%,.2f", saved)}) so far.")
            } else {
                insights.add("⚠️ You are spending more than you are earning this month!")
            }
        }

        return insights
    }
}

