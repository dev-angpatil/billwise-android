package com.billwise.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "Total" or specific category name
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val hasNotified75: Boolean = false,
    val hasNotified100: Boolean = false,
    val lastMonthSpend: Double = 0.0
)
