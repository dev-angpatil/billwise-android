package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction

class CategorizeTransactionUseCase {
    private val categoryKeywords = mapOf(
        "Food & Dining" to listOf("zomato", "swiggy", "restaurant", "cafe", "mcdonalds", "food",
                                   "burger", "kfc", "starbucks", "pizza", "dunkin", "subway",
                                   "dominos", "barbeque", "canteen", "bakery", "biryani", "eatery",
                                   "hotel", "dhaba", "kitchen", "bites", "sweet", "chaat", "tea", "coffee"),
        "Transport & Travel" to listOf("uber", "ola", "rapido", "irctc", "petrol", "fuel", "gas",
                                   "metro", "bus", "cab", "auto", "redbus", "makemytrip", "indigo", 
                                   "air", "flight", "train", "toll", "fastag", "parking", "yatra", "goibibo"),
        "Shopping"      to listOf("amazon", "flipkart", "myntra", "store", "supermarket", "mall",
                                   "dmart", "bigbasket", "blinkit", "zepto", "nykaa", "meesho", 
                                   "ajio", "reliance", "lifestyle", "zara", "h&m", "retail", "mart", "bazaar"),
        "Subscriptions & Entertainment" to listOf("netflix", "prime", "spotify", "hotstar", "zee5", "jio",
                                   "airtel", "apple", "google", "youtube", "disney", "sony", "voot", 
                                   "cinema", "pvr", "inox", "bookmyshow", "game", "steam"),
        "Health & Medical" to listOf("hospital", "pharmacy", "medplus", "netmeds", "apollomed",
                                   "clinic", "doctor", "health", "pharma", "medical", "care", 
                                   "diagnostic", "pathology", "1mg", "practo"),
        "Utilities & Bills" to listOf("electricity", "water", "gas", "internet", "wifi", "bescom", "mahavitaran",
                                   "broadband", "act", "hathway", "bsnl", "atm", "bill", "recharge", "postpaid"),
        "Groceries"     to listOf("grocery", "kirana", "provision", "vegetable", "fruit", "dairy", "milk"),
        "Investment"    to listOf("zerodha", "groww", "upstox", "mutual", "sip", "stocks", "angelone", "coin", "indmoney")
    )

    operator fun invoke(transaction: Transaction): Transaction {
        val merchantLower = (transaction.merchantAlias ?: transaction.merchant).lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { merchantLower.contains(it) }) {
                return transaction.copy(category = category)
            }
        }
        return transaction.copy(category = "Others")
    }
}

