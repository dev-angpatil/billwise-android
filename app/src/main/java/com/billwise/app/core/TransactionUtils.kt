package com.billwise.app.core

import com.billwise.app.domain.model.TransactionType
import java.security.MessageDigest
import java.util.Calendar

object TransactionUtils {

    fun generateDeterministicId(
        prefix: String,
        amount: Double,
        type: TransactionType,
        merchant: String,
        timestamp: Long
    ): String {
        // Round timestamp to the nearest minute for robust deduplication across sources
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val roundedDate = cal.timeInMillis

        val rawStr = "${prefix}_${amount}_${type.name}_${merchant.uppercase()}_$roundedDate"
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(rawStr.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    fun formatCurrency(amount: Double): String {
        return "₹${String.format("%,.2f", amount)}"
    }

    fun formatCurrencyNoDec(amount: Double): String {
        return "₹${String.format("%,.0f", amount)}"
    }
}
