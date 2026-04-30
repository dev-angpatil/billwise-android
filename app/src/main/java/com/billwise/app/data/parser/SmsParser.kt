package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.security.MessageDigest

object SmsParser {

    // Amount: "Rs.", "Rs", "INR", "₹" followed by optional space and number
    private val amountRegex = Regex("""(?i)(?:Rs\.?|INR|₹)\s*([0-9,]+(?:\.[0-9]{1,2})?)""")

    // Merchant: "to <vpa>" or "at <name>" — greedy-stop at comma/period/space
    private val merchantRegex = Regex("""(?i)(?:to|at)\s+([A-Za-z0-9@.\-_]+)""")

    // Detect credit/income keywords
    private val creditRegex = Regex("""(?i)\b(credited|received|credit|refund|cashback)\b""")

    // Detect debit/payment keywords
    private val debitRegex = Regex("""(?i)\b(debited|deducted|paid|payment|sent|transferred|debit)\b""")

    // UPI reference / transaction ID pattern
    private val upiRefRegex = Regex("""(?i)(?:UPI Ref|Ref No|Txn ID|Transaction ID)[.:\s]*([A-Z0-9]{8,})""")

    fun parse(smsBody: String, timestamp: Long): Transaction? {
        val amountMatch = amountRegex.find(smsBody) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // Filter out tiny amounts (likely OTP messages with small numbers near "Rs")
        if (amount < 1.0) return null

        val type = when {
            creditRegex.containsMatchIn(smsBody) -> TransactionType.CREDIT
            debitRegex.containsMatchIn(smsBody) -> TransactionType.DEBIT
            else -> TransactionType.DEBIT // default to debit if ambiguous
        }

        val merchant = merchantRegex.find(smsBody)?.groupValues?.get(1) ?: "Unknown"

        // Prefer UPI ref ID for deterministic key; fall back to SHA-256 of body+timestamp
        val upiRef = upiRefRegex.find(smsBody)?.groupValues?.get(1)
        val deterministicId = upiRef?.let { "upi_$it" }
            ?: sha256("${smsBody.trim()}$timestamp")

        return Transaction(
            id = deterministicId,
            amount = amount,
            merchant = merchant,
            datetime = timestamp,
            type = type,
            category = "Uncategorized",
            source = TransactionSource.SMS
        )
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

