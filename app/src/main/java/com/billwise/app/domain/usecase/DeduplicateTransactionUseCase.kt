package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import kotlin.math.abs

class DeduplicateTransactionUseCase {
    private val TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000L

    /**
     * Returns true if [newTransaction] is a duplicate of any transaction in [existingTransactions].
     * Checks by deterministic ID first (covers SMS re-reads), then falls back to
     * amount + merchant + time-window check (covers near-duplicates from different sources).
     */
    operator fun invoke(newTransaction: Transaction, existingTransactions: List<Transaction>): Boolean {
        // Primary: ID match (deterministic hash from SmsParser or UPI ref)
        if (existingTransactions.any { it.id == newTransaction.id }) return true

        // Fallback: same amount, same merchant (case-insensitive), within 2 minutes
        return existingTransactions.any { existing ->
            val isSameAmount = existing.amount == newTransaction.amount
            val isSameMerchant = existing.merchant.equals(newTransaction.merchant, ignoreCase = true)
            val isWithinTwoMinutes = abs(existing.datetime - newTransaction.datetime) <= TWO_MINUTES_IN_MILLIS
            isSameAmount && isSameMerchant && isWithinTwoMinutes
        }
    }
}

