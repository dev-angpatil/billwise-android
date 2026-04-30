package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType

class AnalyzeSpendingUseCase {
    fun getTotalSpent(transactions: List<Transaction>): Double {
        return transactions.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    }

    fun getCategoryBreakdown(transactions: List<Transaction>): Map<String, Double> {
        return transactions
            .filter { it.type == TransactionType.DEBIT }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }
}
