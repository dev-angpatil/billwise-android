package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import kotlin.math.abs

class DeduplicateTransactionUseCase {
    private val TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000L

    operator fun invoke(newTransaction: Transaction, existingTransactions: List<Transaction>): Boolean {
        return existingTransactions.any { existing ->
            val isSameAmount = existing.amount == newTransaction.amount
            val isWithinTwoMinutes = abs(existing.datetime - newTransaction.datetime) <= TWO_MINUTES_IN_MILLIS
            isSameAmount && isWithinTwoMinutes
        }
    }
}
