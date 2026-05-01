package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.security.MessageDigest
import java.util.Calendar

object SmsParser {

    private val CURRENCY_KEYWORDS = listOf("₹", "rs", "inr")
    private val DEBIT_KEYWORDS = listOf("debited", "dr.", "dr ", "spent", "paid", "sent", "withdrawn", "deducted")
    private val CREDIT_KEYWORDS = listOf("credited", "cr.", "cr ", "received", "deposit", "added", "refund")
    private val SPAM_KEYWORDS = listOf("otp", "verification code", "offer", "sale", "discount", "loan", "win", "won", "reward")

    private fun normalize(text: String): String {
        var normalized = text.lowercase().replace("\n", " ").replace("\r", " ")
        normalized = normalized.replace(Regex("""(?i)\b(rs\.?|inr)\b"""), "₹")
        // Remove commas in numbers (e.g. 1,000.50 -> 1000.50)
        normalized = normalized.replace(Regex("""(?<=\d),(?=\d)"""), "")
        return normalized.replace(Regex("""\s+"""), " ").trim()
    }

    private fun isTransactionMessage(normalized: String): Boolean {
        // Must contain a financial action
        val allKeywords = DEBIT_KEYWORDS + CREDIT_KEYWORDS
        if (!allKeywords.any { normalized.contains(it) }) return false

        // Must contain currency or specific numeric action (e.g., "debited by 190.00")
        val hasCurrency = CURRENCY_KEYWORDS.any { normalized.contains(it) }
        val hasDebitedBy = normalized.contains(Regex("""(?:debited|credited|dr|cr)\s*by\s*\d+"""))
        if (!hasCurrency && !hasDebitedBy) return false

        // Filter spam
        if (SPAM_KEYWORDS.any { normalized.contains(it) }) {
            return false
        }
        
        // Edge cases
        if (normalized.contains("failed") || normalized.contains("declined") || normalized.contains("unsuccessful") || normalized.contains("balance only")) {
            return false
        }

        return true
    }

    private fun extractType(normalized: String): TransactionType? {
        var firstDebitIdx = Int.MAX_VALUE
        var firstCreditIdx = Int.MAX_VALUE

        DEBIT_KEYWORDS.forEach { keyword ->
            val idx = normalized.indexOf(keyword)
            if (idx in 0 until firstDebitIdx) {
                firstDebitIdx = idx
            }
        }

        CREDIT_KEYWORDS.forEach { keyword ->
            val idx = normalized.indexOf(keyword)
            if (idx in 0 until firstCreditIdx) {
                firstCreditIdx = idx
            }
        }

        return when {
            firstDebitIdx < firstCreditIdx -> TransactionType.DEBIT
            firstCreditIdx < firstDebitIdx -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun extractAmount(normalized: String): Double? {
        // Priority 1: "debited by 190.00"
        val byRegex = Regex("""(?:debited|credited|dr|cr)\s*(?:by|for)?\s*(?:₹\s*)?(\d+(?:\.\d{1,2})?)""")
        var match = byRegex.find(normalized)
        if (match != null) return match.groupValues[1].toDoubleOrNull()

        // Priority 2: ₹ 100.50
        val amountRegex1 = Regex("""₹\s?(\d+(?:\.\d{1,2})?)""")
        match = amountRegex1.find(normalized)
        if (match != null) return match.groupValues[1].toDoubleOrNull()

        // Priority 3: 100.50 ₹
        val amountRegex2 = Regex("""(\d+(?:\.\d{1,2})?)\s?₹""")
        match = amountRegex2.find(normalized)
        
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractAccount(normalized: String): String? {
        // Matches "a/c x3057", "a/c xxxxx0085", "acct ending 1234"
        val accountRegex = Regex("""(?:a/c|acct|ac|account|ending)\s*x*(\d{2,4})\b""")
        val match = accountRegex.find(normalized)
        return match?.groupValues?.get(1)
    }

    private fun extractMerchant(normalized: String): String? {
        // Case A: SBI-style "trf to <name>"
        val sbiRegex = Regex("""trf to\s+(.*?)(?:\s+refno|\.|,|$)""")
        var match = sbiRegex.find(normalized)
        if (match != null) {
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) return name.uppercase()
        }

        // Case B: UPI-style "to <name>@upi"
        val upiRegex = Regex("""(?:to|paid to|vpa)\s+([a-z0-9.\-_]+)@[a-z0-9]+""")
        match = upiRegex.find(normalized)
        if (match != null) {
            var name = match.groupValues[1].trim()
            name = name.replace(Regex("""-\d+$"""), "") // Remove numbers or suffixes
            if (name.isNotEmpty()) return name.uppercase()
        }

        // Generic fallback
        val genericRegex = Regex("""(?:paid to|to|at|via)\s+([a-z0-9@.\-_ ]{3,30}?)(?:\s+(?:on|via|ref|upi|using)|\.|,|$)""")
        match = genericRegex.find(normalized)
        if (match != null) {
            var name = match.groupValues[1].trim()
            name = name.replace(Regex("""@[a-z]+"""), "").trim()
            if (name.isNotBlank() && !name.matches(Regex("""^\d+$"""))) return name.uppercase()
        }
        
        return null
    }

    private fun generateHash(amount: Double, type: TransactionType, accountHint: String?, timestamp: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val roundedDate = cal.timeInMillis

        val rawStr = "${amount}_${type.name}_${accountHint ?: "NA"}_$roundedDate"
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(rawStr.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun parse(smsBody: String, sender: String, timestamp: Long): Transaction? {
        val normalized = normalize(smsBody)

        if (!isTransactionMessage(normalized)) return null

        val type = extractType(normalized) ?: return null
        val amount = extractAmount(normalized) ?: return null
        val accountHint = extractAccount(normalized)
        val merchant = extractMerchant(normalized)

        val hashId = generateHash(amount, type, accountHint, timestamp)

        // Identify Internal Wallet Transfers to flag as ignored
        val isWalletTransfer = normalized.contains("paytm") || normalized.contains("phonepe") || 
                               normalized.contains("amazon pay") || normalized.contains("gpay") || 
                               normalized.contains("wallet")
        val isIgnored = isWalletTransfer && type == TransactionType.DEBIT
        val finalMerchant = merchant ?: if (isIgnored) "WALLET RELOAD" else "UNKNOWN"

        return Transaction(
            id = "sms_$hashId",
            amount = amount,
            merchant = finalMerchant.uppercase(),
            datetime = timestamp,
            type = type,
            category = if (normalized.contains("refund")) "Refund" else "Uncategorized",
            source = TransactionSource.SMS,
            isIgnored = isIgnored,
            accountHint = accountHint
        )
    }
}
