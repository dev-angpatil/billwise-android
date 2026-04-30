package com.billwise.app.domain.model

data class Budget(
    val id: Int = 1, // singleton row
    val monthlyLimit: Double,
    val month: Int,   // 1-12
    val year: Int
)
