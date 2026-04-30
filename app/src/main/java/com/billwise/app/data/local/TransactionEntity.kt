package com.billwise.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val merchant: String,
    val datetime: Long,
    val type: TransactionType,
    val category: String,
    val source: TransactionSource
)
