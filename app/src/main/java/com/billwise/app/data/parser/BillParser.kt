package com.billwise.app.data.parser

import com.billwise.app.domain.model.Bill
import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.util.UUID

object BillParser {
    // Placeholder OCR parser for bills
    fun parse(rawData: String): Bill {
        // In a real app, this would use ML Kit or a backend service
        // For now, we return a mock bill based on some basic splits
        val lines = rawData.lines()
        val merchant = lines.firstOrNull() ?: "Unknown Merchant"
        val amountLine = lines.find { it.contains(Regex("(?i)total")) } ?: "0.0"
        val amountStr = amountLine.replace(Regex("[^0-9.]"), "")
        val amount = amountStr.toDoubleOrNull() ?: 0.0

        return Bill(
            id = UUID.randomUUID().toString(),
            amount = amount,
            merchant = merchant,
            datetime = System.currentTimeMillis(),
            rawData = rawData
        )
    }

    fun toTransaction(bill: Bill): Transaction {
        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = bill.amount,
            merchant = bill.merchant,
            datetime = bill.datetime,
            type = TransactionType.DEBIT, // Bills are generally debit
            category = "Uncategorized",
            source = TransactionSource.BILL
        )
    }
}
