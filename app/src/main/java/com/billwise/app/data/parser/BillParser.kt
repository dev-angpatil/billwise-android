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
        val lines = rawData.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return Bill(UUID.randomUUID().toString(), 0.0, "Unknown", System.currentTimeMillis(), rawData)
        }

        var merchant = lines.first()
        var amount = 0.0

        val totalLine = lines.find { it.contains(Regex("(?i)total")) }
        if (totalLine != null) {
            val amountStr = totalLine.replace(Regex("[^0-9.]"), "")
            amount = amountStr.toDoubleOrNull() ?: 0.0
        } else {
            // No "total" found, could be "coffee 200" or a simple text.
            // Extract the last number found as the amount.
            val amountRegex = Regex("""(\d+(?:\.\d{1,2})?)""")
            val allMatches = amountRegex.findAll(rawData).toList()
            if (allMatches.isNotEmpty()) {
                val lastMatch = allMatches.last().value
                amount = lastMatch.toDoubleOrNull() ?: 0.0
                
                // If it's a single line like "coffee 200", remove the number to get merchant
                if (lines.size == 1) {
                    merchant = lines[0].replaceFirst(lastMatch, "").trim()
                        .replace(Regex("""^(paid|for|to)\s+""", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("""\s+(paid|for|to)$""", RegexOption.IGNORE_CASE), "")
                        .trim()
                }
            }
        }
        
        if (merchant.isBlank() || merchant.matches(Regex("""\d+"""))) {
            merchant = "Unknown Merchant"
        }

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
