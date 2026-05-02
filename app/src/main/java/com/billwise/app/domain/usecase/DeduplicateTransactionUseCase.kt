package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import kotlin.math.abs

class DeduplicateTransactionUseCase {

    private val TWO_MINUTES_MS = 2 * 60 * 1000L
    private val ONE_DAY_MS     = 24 * 60 * 60 * 1000L

    /**
     * Returns true if [new] is a duplicate of any transaction in [existing].
     *
     * Priority:
     *   1. ID match (deterministic hash — catches SMS re-reads instantly)
     *   2. UTR match (bank-assigned, globally unique)
     *   3. transactionId match (UPI/bank ref number)
     *   4. amount + merchant + ±2 min (same source)
     *   5. amount + merchant + same calendar day (cross-source SMS↔PDF)
     */
    operator fun invoke(new: Transaction, existing: List<Transaction>): Boolean {
        // Priority 1 — deterministic ID
        if (existing.any { it.id == new.id }) return true

        // Priority 2 — UTR (unique per bank transaction)
        if (!new.utr.isNullOrBlank()) {
            if (existing.any { !it.utr.isNullOrBlank() && it.utr == new.utr }) return true
        }

        // Priority 3 — bank/UPI transaction ID
        if (!new.transactionId.isNullOrBlank()) {
            if (existing.any { !it.transactionId.isNullOrBlank() && it.transactionId == new.transactionId }) return true
        }

        // Priority 4 & 5 — fuzzy match on amount + merchant + time
        return existing.any { ex ->
            val sameAmount   = ex.amount == new.amount
            val sameMerchant = ex.merchant.contains(new.merchant, ignoreCase = true) ||
                               new.merchant.contains(ex.merchant, ignoreCase = true)
            val timeDiff     = abs(ex.datetime - new.datetime)

            if (!sameAmount || !sameMerchant) return@any false

            if (ex.source == new.source) {
                timeDiff <= TWO_MINUTES_MS            // same source → must be very close
            } else {
                timeDiff <= ONE_DAY_MS                // cross-source → same calendar day OK
            }
        }
    }
}
