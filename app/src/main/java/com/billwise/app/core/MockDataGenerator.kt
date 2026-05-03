package com.billwise.app.core

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import com.billwise.app.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

object MockDataGenerator {
    fun seed(repository: TransactionRepository) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val transactions = mutableListOf<Transaction>()
            val cal = Calendar.getInstance()

            // Current month transactions
            val merchants = listOf("Swiggy", "Zomato", "Amazon", "Uber", "Starbucks", "Reliance Digital", "Airtel")
            val categories = listOf("Food", "Food", "Shopping", "Transport", "Food", "Electronics", "Bills")

            for (i in 1..20) {
                val day = (1..cal.get(Calendar.DAY_OF_MONTH)).random()
                cal.set(Calendar.DAY_OF_MONTH, day)
                val idx = merchants.indices.random()

                transactions.add(
                    Transaction(
                        id = "mock_$i",
                        amount = (100..5000).random().toDouble(),
                        type = TransactionType.DEBIT,
                        merchant = merchants[idx],
                        category = categories[idx],
                        datetime = cal.timeInMillis,
                        source = TransactionSource.MANUAL,
                        transactionId = "TXN${100000 + i}",
                        confidenceScore = 1.0f,
                        isIgnored = false
                    )
                )
            }

            // Add some income
            transactions.add(
                Transaction(
                    id = "mock_salary",
                    amount = 75000.0,
                    type = TransactionType.CREDIT,
                    merchant = "Company Inc",
                    category = "Salary",
                    datetime = System.currentTimeMillis() - 86400000 * 5,
                    source = TransactionSource.MANUAL,
                    confidenceScore = 1.0f,
                    isIgnored = false
                )
            )

            transactions.forEach { repository.addTransaction(it) }
        }
    }
}
