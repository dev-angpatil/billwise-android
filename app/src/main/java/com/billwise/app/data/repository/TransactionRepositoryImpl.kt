package com.billwise.app.data.repository

import com.billwise.app.data.local.TransactionDao
import com.billwise.app.data.local.TransactionEntity
import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepositoryImpl(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.insertTransaction(transaction.toEntity())
    }

    override suspend fun getTransactionsInRange(startTime: Long, endTime: Long): List<Transaction> {
        return dao.getTransactionsInRange(startTime, endTime).map { it.toDomain() }
    }

    override suspend fun deleteTransaction(id: String) {
        dao.deleteTransactionById(id)
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        amount = amount,
        merchant = merchant,
        datetime = datetime,
        type = type,
        category = category,
        source = source,
        isIgnored = isIgnored,
        merchantAlias = merchantAlias,
        accountHint = accountHint
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        amount = amount,
        merchant = merchant,
        datetime = datetime,
        type = type,
        category = category,
        source = source,
        isIgnored = isIgnored,
        merchantAlias = merchantAlias,
        accountHint = accountHint
    )
}
