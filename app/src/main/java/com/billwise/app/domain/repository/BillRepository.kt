package com.billwise.app.domain.repository

import com.billwise.app.domain.model.Bill
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun getAllBills(): Flow<List<Bill>>
    suspend fun addBill(bill: Bill)
}
