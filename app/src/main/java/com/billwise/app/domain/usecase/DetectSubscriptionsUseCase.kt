package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionType
import java.util.*
import kotlin.math.abs

class DetectSubscriptionsUseCase {

    data class Subscription(
        val merchant: String,
        val amount: Double,
        val frequency: String, // "Monthly"
        val nextOccurrence: Long
    )

    operator fun invoke(transactions: List<Transaction>): List<Subscription> {
        val debits = transactions.filter { !it.isIgnored && it.type == TransactionType.DEBIT }
        
        val subscriptions = mutableListOf<Subscription>()
        
        // 1. Keyword-based detection (EMI, Rent)
        val specialMerchants = debits.filter { 
            it.merchant.contains("EMI", ignoreCase = true) || 
            it.merchant.contains("RENT", ignoreCase = true) ||
            it.category == "Rent" || it.category == "EMI"
        }.groupBy { it.merchant.uppercase() }
        
        for ((merchant, txns) in specialMerchants) {
            val lastTx = txns.maxByOrNull { it.datetime } ?: continue
            val type = if (merchant.contains("EMI")) "EMI" else "Rent"
            subscriptions.add(
                Subscription(
                    merchant = merchant,
                    amount = lastTx.amount,
                    frequency = type,
                    nextOccurrence = lastTx.datetime + (30L * 24 * 60 * 60 * 1000)
                )
            )
        }

        // 2. Pattern-based detection
        val merchantGroups = debits.groupBy { it.merchant.uppercase() }
        for ((merchant, txns) in merchantGroups) {
            if (txns.size < 2 || subscriptions.any { it.merchant == merchant }) continue
            
            val sorted = txns.sortedByDescending { it.datetime }
            val intervals = mutableListOf<Long>()
            for (i in 0 until sorted.size - 1) {
                intervals.add(sorted[i].datetime - sorted[i+1].datetime)
            }
            
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
            val toleranceMillis = 5L * 24 * 60 * 60 * 1000 // 5 days tolerance
            
            val isMonthly = intervals.all { abs(it - thirtyDaysMillis) <= toleranceMillis }
            val consistentAmount = txns.all { abs(it.amount - txns[0].amount) / txns[0].amount < 0.05 } // 5% tolerance
            
            if (isMonthly && consistentAmount) {
                val lastTx = sorted[0]
                subscriptions.add(
                    Subscription(
                        merchant = merchant,
                        amount = lastTx.amount,
                        frequency = "Monthly",
                        nextOccurrence = lastTx.datetime + thirtyDaysMillis
                    )
                )
            }
        }
        
        return subscriptions
    }
}
