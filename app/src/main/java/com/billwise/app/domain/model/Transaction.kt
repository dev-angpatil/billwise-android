package com.billwise.app.domain.model

data class Transaction(
    val id: String,
    val amount: Double,
    val merchant: String,
    val datetime: Long,
    val type: TransactionType,
    val category: String,
    val source: TransactionSource,
    val isIgnored: Boolean = false,
    val merchantAlias: String? = null,
    val accountHint: String? = null
)

enum class TransactionType {
    CREDIT, DEBIT
}

enum class TransactionSource {
    SMS, BILL, MANUAL
}
