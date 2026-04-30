package com.billwise.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: Int = 1, // singleton row — always upserted
    val monthlyLimit: Double,
    val month: Int,
    val year: Int
)
