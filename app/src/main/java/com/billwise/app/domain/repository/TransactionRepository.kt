package com.billwise.app.domain.repository

import com.billwise.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<Transaction>
}
