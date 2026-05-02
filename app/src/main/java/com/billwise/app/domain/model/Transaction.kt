package com.billwise.app.domain.model

data class Transaction(
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val merchant: String,
    val category: String = "Uncategorized",
    val datetime: Long,
    val source: TransactionSource,
    // Deduplication fields
    val transactionId: String? = null,   // Bank/UPI ref number from PDF or SMS
    val utr: String? = null,             // UTR number from PDF statements
    val accountHint: String? = null,     // Last 4 digits of account
    val merchantAlias: String? = null,   // User-assigned alias
    val balance: Double? = null,         // Available balance after transaction
    val isIgnored: Boolean = false,
    val confidenceScore: Float = 1.0f    // 0.0–1.0; <0.7 means AI-assisted
)

enum class TransactionType { CREDIT, DEBIT }

enum class TransactionSource { SMS, BILL, MANUAL, PDF }
