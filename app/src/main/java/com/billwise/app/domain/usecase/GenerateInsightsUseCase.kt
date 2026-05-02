package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit

class GenerateInsightsUseCase {

    fun getInsights(
        transactions: List<Transaction>, 
        budgetLimit: Double? = null,
        targetMonth: Int? = null,
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
            insights.add("💸 You spent ₹${String.format("%,.0f", totalSpent)} in $monthName.")
        }

        // 1. Budget Monitoring
        if (budgetLimit != null && budgetLimit > 0 && totalSpent > 0) {
            val pct = (totalSpent / budgetLimit * 100).toInt()
            val calToday = Calendar.getInstance()
            val dayOfMonth = if (calToday.get(Calendar.MONTH) + 1 == activeMonth) calToday.get(Calendar.DAY_OF_MONTH) else 30
            val totalDays = calToday.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            val projected = (totalSpent / dayOfMonth) * totalDays
            
            when {
                totalSpent >= budgetLimit ->
                    insights.add("🚨 Budget Alert: You've exceeded your ₹${String.format("%,.0f", budgetLimit)} limit!")
                projected > budgetLimit -> {
                    val daysRemaining = totalDays - dayOfMonth
                    val remainingBudget = budgetLimit - totalSpent
                    val allowPerDay = remainingBudget / daysRemaining.coerceAtLeast(1)
                    insights.add("⚠️ Pace Alert: You're on track to exceed your budget. Limit spending to ₹${String.format("%.0f", allowPerDay)}/day to stay safe.")
                }
                pct >= 85 ->
                    insights.add("⚠️ Budget Warning: You've consumed ${pct}% of your ₹${String.format("%,.0f", budgetLimit)} budget.")
            }
        }

        // 2. Weekly Anomaly Detection
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - TimeUnit.DAYS.toMillis(7)
        val recentSpend = allDebits.filter { it.datetime >= oneWeekAgo }.sumOf { it.amount }
        
        // Compare to 4-week average
        val fourWeekSpend = allDebits.filter { it.datetime >= (now - TimeUnit.DAYS.toMillis(28)) }.sumOf { it.amount }
        val fourWeekAvg = fourWeekSpend / 4.0
        
        if (recentSpend > (fourWeekAvg * 1.5) && fourWeekAvg > 1000) {
            insights.add("📉 Spending Spike: This week's spend (₹${String.format("%.0f", recentSpend)}) is 50% higher than your 4-week average.")
        }

        // 3. Category Intelligence
        val categoryBreakdown = activeMonthDebits.groupBy { it.category }
            .mapValues { e -> e.value.sumOf { it.amount } }
        val topCategory = categoryBreakdown.maxByOrNull { it.value }
        if (topCategory != null && totalSpent > 0) {
            val pct = (topCategory.value / totalSpent * 100).toInt()
            val icon = when(topCategory.key) {
                "Food & Dining" -> "🍕"
                "Shopping" -> "🛍️"
                "Groceries" -> "🛒"
                "Transport & Travel" -> "🚗"
                else -> "📊"
            }
            insights.add("$icon Top Expense: ${topCategory.key} accounts for ${pct}% of your spend (₹${String.format("%,.0f", topCategory.value)}).")
        }

        // 4. Subscription Detection
        val subscriptionDetector = DetectSubscriptionsUseCase()
        val detectedSubscriptions = subscriptionDetector(transactions)
        if (detectedSubscriptions.isNotEmpty()) {
            val names = detectedSubscriptions.take(3).joinToString(", ") { it.merchant }
            insights.add("🏷️ Fixed Costs: Detected recurring payments for $names.")
        }

        // 5. Daily Burn Heuristic
        val daysPassed = if (cal.get(Calendar.MONTH) + 1 == activeMonth) cal.get(Calendar.DAY_OF_MONTH) else 30
        val avgDaily = totalSpent / daysPassed
        if (avgDaily > 0) {
            insights.add("📅 Burn Rate: You spend an average of ₹${String.format("%,.0f", avgDaily)} per day.")
        }

        // 6. Savings Rate
        val activeMonthCredits = transactions.filter {
            !it.isIgnored && it.type == TransactionType.CREDIT &&
                run {
                    cal.timeInMillis = it.datetime
                    cal.get(Calendar.MONTH) + 1 == activeMonth && cal.get(Calendar.YEAR) == activeYear
                }
        }
        val totalIncome = activeMonthCredits.sumOf { it.amount }
        if (totalIncome > 0 && totalSpent > 0) {
            val saved = totalIncome - totalSpent
            if (saved > 0) {
                val savingRate = (saved / totalIncome * 100).toInt()
                insights.add("📈 Savings: You saved ${savingRate}% of your income (₹${String.format("%,.0f", saved)}) so far.")
            } else {
                insights.add("⚠️ Cashflow: Your spending has exceeded your income for $monthName.")
            }
        }

        // 7. Category Habit Comparison
        val analyzer = AnalyzeSpendingUseCase()
        topCategory?.let { (cat, amt) ->
            val avg = analyzer.getCategoryAveragePastMonths(transactions, cat)
            if (avg > 0) {
                val diff = amt - avg
                val pct = (abs(diff) / avg * 100).toInt()
                if (pct >= 20) {
                    val msg = if (diff > 0) "📈 You spent ${pct}% more on $cat than your 3-month average."
                             else "📉 Great job! You spent ${pct}% less on $cat than your usual average."
                    insights.add(msg)
                }
            }
        }

        // 8. Merchant Frequency
        val merchantCounts = allDebits
            .map { it.merchantAlias ?: it.merchant }
            .filter { it != "UNCATEGORISED" && it != "UNKNOWN" }
            .groupBy { it }
            .mapValues { it.value.size }
        
        val mostFrequent = merchantCounts.maxByOrNull { it.value }
        if (mostFrequent != null && mostFrequent.value >= 4) {
            insights.add("🏪 Habit Loop: You've shopped at ${mostFrequent.key} ${mostFrequent.value} times. Consider if there are bulk-buy options!")
        }

        return insights
    }

    private fun abs(n: Double) = if (n < 0) -n else n

    fun formatTransactionsForAi(transactions: List<Transaction>): String {
        val subscriptionDetector = DetectSubscriptionsUseCase()
        val subscriptions = subscriptionDetector(transactions).map { it.merchant.uppercase() }
        
        return transactions.filter { !it.isIgnored && it.type == TransactionType.DEBIT }
            .takeLast(50) // Last 50 transactions for context
            .joinToString("\n") { tx ->
                val recurring = if (subscriptions.contains(tx.merchant.uppercase())) " (Recurring)" else ""
                val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date(tx.datetime))
                "- $date: ${tx.merchant} | Category: ${tx.category} | Amount: ₹${tx.amount}$recurring"
            }
    }
}
