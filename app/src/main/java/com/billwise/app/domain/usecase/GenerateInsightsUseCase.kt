package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.Calendar

class GenerateInsightsUseCase {

    fun getInsights(
        transactions: List<Transaction>, 
        budgetLimit: Double? = null,
        targetMonth: Int? = null, // 1-indexed (1=Jan, 12=Dec)
        targetYear: Int? = null
    ): List<String> {
        if (transactions.isEmpty()) return listOf("No transactions yet. Add some to get insights!")

        val insights = mutableListOf<String>()
        val cal = Calendar.getInstance()
        
        val activeMonth = targetMonth ?: (cal.get(Calendar.MONTH) + 1)
        val activeYear = targetYear ?: cal.get(Calendar.YEAR)
        
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val monthName = monthNames.getOrNull(activeMonth - 1) ?: "This month"

        val allDebits = transactions.filter { !it.isIgnored && it.type == TransactionType.DEBIT }
        val activeMonthDebits = allDebits.filter {
            cal.timeInMillis = it.datetime
            cal.get(Calendar.MONTH) + 1 == activeMonth && cal.get(Calendar.YEAR) == activeYear
        }

        val totalSpent = activeMonthDebits.sumOf { it.amount }

        if (totalSpent > 0) {
            insights.add("💸 You spent ₹${String.format("%,.2f", totalSpent)} in $monthName.")
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

        // Top category for active month
        val categoryBreakdown = activeMonthDebits.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
        val topCategory = categoryBreakdown.maxByOrNull { it.value }
        if (topCategory != null && totalSpent > 0) {
            val pct = (topCategory.value / totalSpent * 100).toInt()
            insights.add("📊 ${topCategory.key} is your biggest spend at ${pct}% (₹${String.format("%,.2f", topCategory.value)}).")
        }

        // Food alert
        val foodSpent = categoryBreakdown["Food"] ?: 0.0
        if (foodSpent > 5000) {
            insights.add("🍕 Your food spending (₹${String.format("%,.2f", foodSpent)}) was quite high in $monthName.")
        }

        // Average daily spend for active month
        val uniqueDays = activeMonthDebits.map {
            cal.timeInMillis = it.datetime
            cal.get(Calendar.DAY_OF_MONTH)
        }.toSet().size
        if (uniqueDays > 0) {
            val avgDaily = totalSpent / uniqueDays
            insights.add("📅 You spend an average of ₹${String.format("%,.2f", avgDaily)} per day.")
        }

        // Biggest single transaction active month
        val biggestTx = activeMonthDebits.maxByOrNull { it.amount }
        if (biggestTx != null) {
            insights.add("🏆 Biggest purchase: ₹${String.format("%,.2f", biggestTx.amount)} at ${biggestTx.merchantAlias ?: biggestTx.merchant}.")
        }

        // Recurring subscription check (using ALL history)
        val recurring = allDebits.groupBy { it.merchantAlias ?: it.merchant }
            .filter { it.value.size >= 2 && it.value.all { tx -> tx.amount == it.value.first().amount } }
        if (recurring.isNotEmpty()) {
            insights.add("🔄 You have potential recurring payments for: ${recurring.keys.joinToString(", ")}. Tap them in Transactions to tag them as Subscriptions or Rent!")
        }

        // Safe daily spend to hit budget (Only relevant if looking at the CURRENT month)
        val today = Calendar.getInstance()
        if (today.get(Calendar.MONTH) + 1 == activeMonth && today.get(Calendar.YEAR) == activeYear) {
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

        // Frequent Merchant (using ALL history)
        val merchantCounts = allDebits.groupBy { it.merchantAlias ?: it.merchant }.mapValues { it.value.size }
        val mostFrequent = merchantCounts.maxByOrNull { it.value }
        if (mostFrequent != null && mostFrequent.value >= 3) {
            insights.add("🏪 You shop frequently at ${mostFrequent.key} (${mostFrequent.value} times historically).")
        }

        // Weekend vs Weekday spending (using ALL history for better pattern detection)
        var weekendSpend = 0.0
        var weekdaySpend = 0.0
        allDebits.forEach { tx ->
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

        // Late Night Spender (using ALL history)
        var lateNightSpend = 0.0
        allDebits.forEach { tx ->
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
                    cal.get(Calendar.MONTH) + 1 == activeMonth && cal.get(Calendar.YEAR) == activeYear
                }
        }
        val totalIncome = credits.sumOf { it.amount }
        if (totalIncome > 0 && totalSpent > 0) {
            val saved = totalIncome - totalSpent
            if (saved > 0) {
                val savingRate = (saved / totalIncome * 100).toInt()
                insights.add("📈 Great job! You saved ${savingRate}% of your income (₹${String.format("%,.2f", saved)}) in $monthName.")
            } else {
                insights.add("⚠️ You spent more than you earned in $monthName!")
            }
        }

        return insights
    }
}

