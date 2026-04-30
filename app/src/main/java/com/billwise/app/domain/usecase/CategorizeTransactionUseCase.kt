package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction

class CategorizeTransactionUseCase {
    private val categoryKeywords = mapOf(
        "Food"          to listOf("zomato", "swiggy", "restaurant", "cafe", "mcdonalds",
                                   "burger", "kfc", "starbucks", "pizza", "dunkin", "subway",
                                   "dominos", "barbeque", "canteen", "bakery", "biryani"),
        "Transport"     to listOf("uber", "ola", "rapido", "irctc", "petrol", "fuel",
                                   "metro", "bus", "cab", "auto", "redbus", "makemytrip", "indigo"),
        "Shopping"      to listOf("amazon", "flipkart", "myntra", "store", "supermarket",
                                   "dmart", "bigbasket", "blinkit", "zepto", "nykaa", "meesho"),
        "Subscriptions" to listOf("netflix", "prime", "spotify", "hotstar", "zee5", "jio",
                                   "airtel", "apple", "google", "youtube", "disney"),
        "Medical"       to listOf("hospital", "pharmacy", "medplus", "netmeds", "apollomed",
                                   "clinic", "doctor", "health", "pharma", "medical"),
        "Utilities"     to listOf("electricity", "water", "gas", "internet", "wifi",
                                   "broadband", "act", "hathway", "bsnl", "atm", "bill")
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

