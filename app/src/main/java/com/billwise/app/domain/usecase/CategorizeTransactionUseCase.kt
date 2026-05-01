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

    suspend operator fun invoke(transaction: Transaction, pastTransactions: List<Transaction> = emptyList()): Transaction {
        // Priority 1: Check if we have seen this exact merchant before and inherit its category
        val pastMatch = pastTransactions.find { 
            it.merchant.equals(transaction.merchant, ignoreCase = true) && 
            it.category != "Uncategorized" && 
            it.category != "Others" 
        }
        if (pastMatch != null) {
            return transaction.copy(category = pastMatch.category)
        }

        // Priority 2: Use keyword matching
        val merchantLower = (transaction.merchantAlias ?: transaction.merchant).lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { merchantLower.contains(it) }) {
                return transaction.copy(category = category)
            }
        }
        
        // Priority 3: OpenAI Fallback
        val apiKey = "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"
        try {
            val response = com.billwise.app.data.remote.RetrofitClient.openAiApi.categorizeTransaction(
                "Bearer $apiKey",
                com.billwise.app.data.remote.OpenAiRequest(
                    messages = listOf(
                        com.billwise.app.data.remote.Message(
                            role = "system",
                            content = "You are a financial categorizer. Categorize the given merchant name into one of these: Food & Dining, Transport & Travel, Shopping, Subscriptions & Entertainment, Health & Medical, Utilities & Bills, Groceries, Investment, Others. ONLY return the category name."
                        ),
                        com.billwise.app.data.remote.Message(
                            role = "user",
                            content = "Merchant: \${transaction.merchant}"
                        )
                    )
                )
            )
            val aiCategory = response.choices.firstOrNull()?.message?.content?.trim()
            if (aiCategory != null && categoryKeywords.keys.contains(aiCategory)) {
                return transaction.copy(category = aiCategory)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return transaction.copy(category = "Others")
    }
}

