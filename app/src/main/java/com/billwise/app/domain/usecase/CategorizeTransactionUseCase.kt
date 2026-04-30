package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction

class CategorizeTransactionUseCase {
    private val categoryKeywords = mapOf(
        "Food" to listOf("zomato", "swiggy", "restaurant", "cafe", "mcdonalds", "burger"),
        "Transport" to listOf("uber", "ola", "irctc", "petrol", "fuel", "metro"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "store", "supermarket", "dmart"),
        "Subscriptions" to listOf("netflix", "prime", "spotify", "hotstar")
    )

    operator fun invoke(transaction: Transaction): Transaction {
        val merchantLower = transaction.merchant.lowercase()
        
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { merchantLower.contains(it) }) {
                return transaction.copy(category = category)
            }
        }
        
        return transaction.copy(category = "Others")
    }
}
