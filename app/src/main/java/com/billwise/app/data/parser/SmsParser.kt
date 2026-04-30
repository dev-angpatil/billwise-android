package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.util.UUID

object SmsParser {
    // Basic regex for UPI/Bank SMS extraction
    // Example: "Rs. 150.00 debited from a/c **1234 on 05-10-23 to VPA merchant@upi"
    private val amountRegex = Regex("(?i)(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val merchantRegex = Regex("(?i)(?:to|at)\\s+([a-zA-Z0-9@.-]+)")
    private val creditedRegex = Regex("(?i)credited|received")
    
    fun parse(smsBody: String, timestamp: Long): Transaction? {
        val amountMatch = amountRegex.find(smsBody)
        val merchantMatch = merchantRegex.find(smsBody)
        
        if (amountMatch == null) return null
        
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null
        
        val type = if (creditedRegex.containsMatchIn(smsBody)) {
            TransactionType.CREDIT
        } else {
            TransactionType.DEBIT
        }
        
        val merchant = merchantMatch?.groupValues?.get(1) ?: "Unknown"
        
        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            merchant = merchant,
            datetime = timestamp,
            type = type,
            category = "Uncategorized",
            source = TransactionSource.SMS
        )
    }
}
