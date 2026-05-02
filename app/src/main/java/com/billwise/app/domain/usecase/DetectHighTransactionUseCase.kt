package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType

class DetectHighTransactionUseCase {
    
    private val DEFAULT_THRESHOLD = 5000.0 // ₹5000
    
    operator fun invoke(transaction: Transaction, userThreshold: Double? = null): Boolean {
        if (transaction.type != TransactionType.DEBIT) return false
        val threshold = userThreshold ?: DEFAULT_THRESHOLD
        return transaction.amount >= threshold
    }
}
