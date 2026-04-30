package com.billwise.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val merchant: String,
    val datetime: Long,
    val rawData: String
)
