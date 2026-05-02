package com.billwise.app.domain.model

data class Budget(
    val id: Long = 0,
    val category: String = "Total",
    val monthlyLimit: Double,
    val month: Int,   // 1-12
    val year: Int,
    val hasNotified75: Boolean = false,
    val hasNotified100: Boolean = false,
    val lastMonthSpend: Double = 0.0
)
