package com.billwise.app.domain.usecase

import com.billwise.app.domain.model.Transaction

class CategorizeTransactionUseCase {

    /**
     * Exhaustive keyword map.
     * Rules are checked top-to-bottom; first match wins.
     * Each category has PRIMARY keywords (strong signal) and
     * CONTEXTUAL keywords (weaker, matched after primary pass).
     */
    private val categoryKeywords: Map<String, List<String>> = linkedMapOf(
        // ── FOOD & DINING ─────────────────────────────────────────────────
        "Food & Dining" to listOf(
            // Delivery apps
            "zomato", "swiggy", "dunzo", "eatsure", "fasoos", "box8",
            // Chains & brands
            "mcdonalds", "mcdonald", "burger king", "kfc", "pizza hut",
            "dominos", "domino", "subway", "starbucks", "costa", "chaayos",
            "barista", "dunkin", "haldiram", "bikanervala", "wow momo",
            "naturals ice", "keventers", "behrouz", "freshmenu",
            // Generic food words
            "restaurant", "cafe", "bistro", "canteen", "dhaba", "diner",
            "eatery", "kitchen", "grill", "bakery", "biryani", "tiffin",
            "udupi", "thali", "misal", "vada pav", "pav bhaji", "chaat",
            "sweets", "mithai", "juice", "barbeque", "barbeque nation",
            "food court", "food plaza", "chinese", "sushi", "burger",
            "sandwich", "rolls", "wraps", "noodles", "pasta", "pizza",
            "shawarma", "paratha", "lassi", "tea stall", "chai",
            // Generic suffixes often used in small restaurant names
            "foods", "eats", "bites", "treats", "delights", "corner"
        ),

        // ── TRANSPORT & TRAVEL ────────────────────────────────────────────
        "Transport & Travel" to listOf(
            // Cab / ride
            "uber", "ola", "rapido", "meru", "BluSmart", "blusmart",
            // Rail / bus / flight
            "irctc", "redbus", "abhibus", "makemytrip", "goibibo",
            "yatra", "cleartrip", "indigo", "spicejet", "air india",
            "airindia", "vistara", "akasa", "easemytrip", "ixigo",
            "railyatri", "railwire",
            // Local transport
            "metro", "dmrc", "bmtc", "best bus", "apsrtc", "ksrtc",
            "auto", "rickshaw",
            // Fuel
            "petrol", "diesel", "fuel", "hp petrol", "iocl", "bpcl",
            "hindustan petroleum", "indian oil", "bharat petroleum",
            "reliance petro", "essar fuel",
            // Road
            "fastag", "toll", "parking", "valet",
            // Accommodation
            "hotel", "oyo", "gorooms", "fabhotels", "treebo",
            "airbnb", "hostel", "resort", "lodge", "inn"
        ),

        // ── GROCERIES ────────────────────────────────────────────────────
        "Groceries" to listOf(
            // Apps
            "bigbasket", "blinkit", "zepto", "jiomart", "grofers",
            "supr daily", "milkbasket", "dunzo grocery", "swiggy instamart",
            // Stores
            "dmart", "reliance fresh", "reliance smart", "more supermarket",
            "spencers", "star bazaar", "nature basket", "spar", "auchan",
            "easy day", "food world", "nilgiris", "heritage fresh",
            // Generic
            "grocery", "kirana", "provision", "supermarket", "general store",
            "vegetables", "fruits", "dairy", "milk", "eggs", "bread",
            "ration", "sabziwala", "sabzi"
        ),

        // ── SHOPPING ─────────────────────────────────────────────────────
        "Shopping" to listOf(
            // E-commerce
            "amazon", "flipkart", "meesho", "snapdeal", "paytm mall",
            "tata cliq", "ajio", "nykaa", "myntra", "bewakoof",
            "limeroad", "purplle", "mamaearth", "boat", "noise",
            // Fashion / lifestyle
            "zara", "h&m", "uniqlo", "max fashion", "lifestyle",
            "westside", "pantaloons", "shoppers stop", "central",
            "reliance trends", "v mart", "brand factory",
            "levis", "nike", "adidas", "puma", "reebok", "skechers",
            "bata", "metro shoes", "woodland", "liberty shoes",
            // Electronics
            "croma", "vijay sales", "reliance digital", "samsung store",
            "apple store", "oneplus", "mi store", "realme",
            // Others
            "mall", "retail", "boutique", "emporium", "showroom",
            "decathlon", "trends", "bazaar store", "mart store"
        ),

        // ── SUBSCRIPTIONS & ENTERTAINMENT ────────────────────────────────
        "Subscriptions & Entertainment" to listOf(
            // Streaming
            "netflix", "hotstar", "disney", "prime video", "amazon prime",
            "zee5", "sonyliv", "voot", "mxplayer", "alt balaji",
            "jiocinema", "apple tv", "youtube premium",
            "spotify", "gaana", "jiosaavn", "wynk", "hungama",
            "apple music", "amazon music",
            // Gaming
            "steam", "playstation", "xbox", "battleground", "pubg",
            "valorant", "mobile premier", "dream11", "mpl", "gameskraft",
            // Entertainment
            "pvr", "inox", "cinepolis", "bookmyshow", "paytm insider",
            "district by zomato", "lbb",
            // Subscriptions
            "subscription", "google one", "icloud", "dropbox", "adobe"
        ),

        // ── HEALTH & MEDICAL ──────────────────────────────────────────────
        "Health & Medical" to listOf(
            // Pharmacies
            "medplus", "netmeds", "1mg", "pharmeasy", "apollo pharmacy",
            "health meds", "wellness",
            // Hospitals / labs
            "hospital", "clinic", "nursing home", "healthcare", "lifecare",
            "thyrocare", "dr lal", "metropolis", "srl diagnostics",
            "vijaya diagnostics", "healthians",
            // Fitness
            "gym", "cult.fit", "cult fit", "cure.fit", "curefit",
            "gold's gym", "anytime fitness", "snap fitness",
            "yoga", "fitness centre", "crossfit",
            // Medical services
            "doctor", "practo", "apollo 247", "mfine", "lybrate",
            "tata 1mg", "pharma", "medical", "pathology", "diagnostic",
            "dental", "optician", "optometrist"
        ),

        // ── UTILITIES & BILLS ─────────────────────────────────────────────
        "Utilities & Bills" to listOf(
            // Electricity
            "bescom", "bses", "tata power", "msedcl", "mahavitaran",
            "adani electricity", "torrent power", "wesco", "cesc",
            "electricity", "bijlee",
            // Water & gas
            "water board", "water supply", "piped gas", "mahanagar gas",
            "indraprastha gas", "igl", "mgl", "adani gas", "gujarat gas",
            // Internet
            "airtel broadband", "jio fiber", "act fibernet", "hathway",
            "you broadband", "tata play fiber", "bsnl broadband",
            "broadband", "internet", "wifi",
            // Mobile recharge
            "airtel", "jio", "vi ", "vodafone", "bsnl", "postpaid",
            "prepaid", "recharge", "mobile bill",
            // DTH
            "tata sky", "tata play", "dish tv", "sun direct",
            "videocon d2h", "dth",
            // Insurance
            "lic", "hdfc life", "icici prudential", "sbi life",
            "bajaj allianz", "star health", "care insurance",
            "niva bupa", "insurance premium",
            // Other bills
            "maintenance", "society", "rent", "landlord", "pg charges",
            "municipal", "property tax", "mcd", "bbmp"
        ),

        // ── INVESTMENT & FINANCE ──────────────────────────────────────────
        "Investment" to listOf(
            // Brokers
            "zerodha", "groww", "upstox", "angelone", "angel broking",
            "5paisa", "kotak securities", "motilal oswal", "iifl securities",
            "paytm money", "indmoney", "scripbox", "kuvera",
            // Crypto
            "coindcx", "wazirx", "coinswitch", "zebpay",
            // Savings / FD
            "mutual fund", "sip", "elss", "nps", "ppf", "fd invest",
            "recurring deposit", "sbi invest", "hdfc invest",
            // Generic
            "stocks", "shares", "portfolio", "demat", "trading"
        ),

        // ── EDUCATION ────────────────────────────────────────────────────
        "Education" to listOf(
            "byju", "unacademy", "vedantu", "physicswallah", "physics wallah",
            "coursera", "udemy", "skillshare", "linkedin learning",
            "duolingo", "school fee", "college fee", "tuition",
            "coaching", "institute", "academy", "university", "cbse",
            "books", "stationery", "pen", "notebook", "library"
        ),

        // ── TRANSFERS & PAYMENTS ──────────────────────────────────────────
        "Transfer" to listOf(
            "wallet reload", "wallet transfer", "sent to", "phonepe transfer",
            "gpay transfer", "paytm transfer", "upi transfer",
            "neft", "rtgs", "imps", "fund transfer", "self transfer",
            "bank transfer"
        )
    )

    suspend operator fun invoke(
        transaction: Transaction,
        pastTransactions: List<Transaction> = emptyList()
    ): Transaction {
        val merchant = (transaction.merchantAlias ?: transaction.merchant).trim().lowercase()
        
        // Priority 1: Merchant memory — inherit known category from past
        val pastMatch = pastTransactions.find {
            val pastMerchant = (it.merchantAlias ?: it.merchant).trim().lowercase()
            (pastMerchant == merchant || isFuzzyMatch(pastMerchant, merchant)) &&
            it.category != "Uncategorized" &&
            it.category != "Others" &&
            it.category != "UNCATEGORISED"
        }
        if (pastMatch != null) return transaction.copy(category = pastMatch.category)

        // Priority 2: Exhaustive keyword matching (case-insensitive substring)
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { merchant.contains(it.lowercase()) || isFuzzyMatch(merchant, it.lowercase()) }) {
                return transaction.copy(category = category)
            }
        }

        // Priority 3: Keyword-based inference from merchant name
        val inferredCategory = inferCategoryFromName(merchant)
        if (inferredCategory != null) return transaction.copy(category = inferredCategory)

        // Priority 4: OpenAI fallback — only called when keywords fail
        val apiKey = com.billwise.app.core.Config.OPENAI_API_KEY
        try {
            val response = com.billwise.app.data.remote.RetrofitClient.openAiApi.categorizeTransaction(
                "Bearer $apiKey",
                com.billwise.app.data.remote.OpenAiRequest(
                    messages = listOf(
                        com.billwise.app.data.remote.Message(
                            role = "system",
                            content = "You are a financial transaction categorizer for an Indian user. " +
                                "Categorize the merchant into EXACTLY one of: " +
                                "Food & Dining, Transport & Travel, Groceries, Shopping, " +
                                "Subscriptions & Entertainment, Health & Medical, " +
                                "Utilities & Bills, Investment, Education, Transfer, Others. " +
                                "Reply with ONLY the category name, nothing else."
                        ),
                        com.billwise.app.data.remote.Message(
                            role = "user",
                            content = "Merchant: $merchant"
                        )
                    )
                )
            )
            val aiCategory = response.choices.firstOrNull()?.message?.content?.trim()
            if (!aiCategory.isNullOrBlank() && categoryKeywords.keys.any { it.equals(aiCategory, ignoreCase = true) }) {
                return transaction.copy(category = aiCategory)
            }
            if (!aiCategory.isNullOrBlank() && aiCategory != "Others") {
                return transaction.copy(category = aiCategory)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return transaction.copy(category = "Others")
    }

    private fun isFuzzyMatch(s1: String, s2: String): Boolean {
        if (s1.length < 4 || s2.length < 4) return false
        val distance = levenshtein(s1, s2)
        val threshold = (kotlin.math.max(s1.length, s2.length) * 0.2).toInt() // 20% error margin
        return distance <= threshold
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = kotlin.math.min(
                    kotlin.math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    private fun inferCategoryFromName(name: String): String? {
        val rules = mapOf(
            "Food & Dining" to listOf("food", "restaurant", "cafe", "eats", "kitchen", "bakery", "bistro"),
            "Groceries" to listOf("mart", "store", "grocery", "kirana", "supermarket", "provision"),
            "Health & Medical" to listOf("clinic", "hospital", "pharmacy", "medical", "health", "dr "),
            "Utilities & Bills" to listOf("electricity", "bill", "recharge", "broadband", "telecom", "water"),
            "Transport & Travel" to listOf("cabs", "travel", "tours", "petrol", "fuel", "motors")
        )
        for ((category, keywords) in rules) {
            if (keywords.any { name.contains(it) }) return category
        }
        return null
    }
}
