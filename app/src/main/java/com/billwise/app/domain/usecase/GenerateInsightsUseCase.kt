package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.Calendar

class GenerateInsightsUseCase {
    fun getInsights(transactions: List<Transaction>): List<String> {
        if (transactions.isEmpty()) return listOf("No transactions yet. Add some to get insights!")

        val insights = mutableListOf<String>()
        val debits = transactions.filter { it.type == TransactionType.DEBIT }
        val totalSpent = debits.sumOf { it.amount }
        
        if (totalSpent > 0) {
            insights.add("You have spent Rs. ${String.format("%.2f", totalSpent)} in total.")
        }
        
        val categoryBreakdown = debits.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        val topCategory = categoryBreakdown.maxByOrNull { it.value }
        if (topCategory != null && totalSpent > 0) {
            val percentage = (topCategory.value / totalSpent) * 100
            insights.add("${topCategory.key} accounts for ${String.format("%.0f", percentage)}% of your expenses.")
        }

        // Example: Check if a specific category spending is high
        val foodSpent = categoryBreakdown["Food"] ?: 0.0
        if (foodSpent > 5000) {
            insights.add("Your spending on Food is quite high this month.")
        }

        return insights
    }
}
