package com.billwise.app.domain.model

data class Bill(
    val id: String,
    val amount: Double,
    val merchant: String,
    val datetime: Long,
    val rawData: String
)
