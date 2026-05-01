package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import kotlin.math.abs

class DeduplicateTransactionUseCase {
    private val TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000L
    private val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    /**
     * Returns true if [newTransaction] is a duplicate of any transaction in [existingTransactions].
     * Checks by deterministic ID first (covers SMS re-reads), then falls back to
     * amount + merchant + time-window check (covers near-duplicates from different sources).
     */
    operator fun invoke(newTransaction: Transaction, existingTransactions: List<Transaction>): Boolean {
        // Primary: ID match (deterministic hash from SmsParser or UPI ref)
        if (existingTransactions.any { it.id == newTransaction.id }) return true

        return existingTransactions.any { existing ->
            val isSameAmount = existing.amount == newTransaction.amount
            val isSimilarMerchant = existing.merchant.contains(newTransaction.merchant, ignoreCase = true) ||
                                    newTransaction.merchant.contains(existing.merchant, ignoreCase = true) ||
                                    (existing.merchantAlias?.contains(newTransaction.merchant, ignoreCase = true) == true)
                                    
            val timeDiff = abs(existing.datetime - newTransaction.datetime)
            
            if (existing.source == newTransaction.source) {
                // Same source (e.g. two manual entries, or two SMS) -> must be very close in time to be considered duplicate
                isSameAmount && isSimilarMerchant && timeDiff <= TWO_MINUTES_IN_MILLIS
            } else {
                // Different sources (e.g. SMS vs Manual/Bill) -> allow up to 24 hours difference
                isSameAmount && isSimilarMerchant && timeDiff <= ONE_DAY_IN_MILLIS
            }
        }
    }
}

