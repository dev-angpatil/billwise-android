package com.billwise.app.domain.model

data class Transaction(
    val id: String,
    val amount: Double,
    val merchant: String,
    val datetime: Long,
    val type: TransactionType,
    val category: String,
    val source: TransactionSource
)

enum class TransactionType {
    CREDIT, DEBIT
}

enum class TransactionSource {
    SMS, BILL, MANUAL
}
