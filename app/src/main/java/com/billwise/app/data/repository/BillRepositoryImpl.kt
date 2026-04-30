package com.billwise.app.data.repository

import com.billwise.app.data.local.BillDao
import com.billwise.app.data.local.BillEntity
import com.billwise.app.domain.model.Bill
import com.billwise.app.domain.repository.BillRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BillRepositoryImpl(
    private val dao: BillDao
) : BillRepository {

    override fun getAllBills(): Flow<List<Bill>> {
        return dao.getAllBills().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addBill(bill: Bill) {
        dao.insertBill(bill.toEntity())
    }

    private fun BillEntity.toDomain() = Bill(
        id = id,
        amount = amount,
        merchant = merchant,
        datetime = datetime,
        rawData = rawData
    )

    private fun Bill.toEntity() = BillEntity(
        id = id,
        amount = amount,
        merchant = merchant,
        datetime = datetime,
        rawData = rawData
    )
}
