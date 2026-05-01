package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.security.MessageDigest
import java.util.Calendar

object SmsParser {

    private val CURRENCY_KEYWORDS = listOf("₹", "rs", "inr")
    private val DEBIT_KEYWORDS = listOf("debited", "spent", "paid", "sent", "purchase", "txn", "withdrawn", "deducted")
    private val CREDIT_KEYWORDS = listOf("credited", "received", "deposit", "added", "refund")
    private val SPAM_KEYWORDS = listOf("otp", "verification code", "offer", "sale", "discount", "loan", "win", "won", "lottery", "rummy", "bet", "free", "click", "approve", "pre-approved")
    private val KNOWN_BANKS = listOf("HDFCBK", "ICICIB", "SBIINB", "AXISBK", "KOTAKB", "PAYTMB", "VK-SBIINB")

    // Normalizes text by lowercasing, standardizing currency, removing commas in numbers, and trimming
    private fun normalize(text: String): String {
        var normalized = text.lowercase().replace("\n", " ").replace("\r", " ")
        normalized = normalized.replace(Regex("""(?i)\b(rs\.?|inr)\b"""), "₹")
        // Remove commas in numbers (e.g. 1,000.50 -> 1000.50)
        normalized = normalized.replace(Regex("""(?<=\d),(?=\d)"""), "")
        return normalized.replace(Regex("""\s+"""), " ").trim()
    }

    private fun isTransactionMessage(normalized: String): Boolean {
        // Must contain currency indicator
        if (!CURRENCY_KEYWORDS.any { normalized.contains(it) }) return false

        // Must contain a financial action
        val allKeywords = DEBIT_KEYWORDS + CREDIT_KEYWORDS
        if (!allKeywords.any { normalized.contains(it) }) return false

        // Filter spam
        if (SPAM_KEYWORDS.any { normalized.contains(it) }) {
            // "cashback" is okay if paired with credit/debit
            return false
        }
        
        // Edge case: Failed transactions
        if (normalized.contains("failed") || normalized.contains("declined") || normalized.contains("unsuccessful")) {
            return false
        }

        return true
    }

    private fun detectType(normalized: String): TransactionType? {
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
        // Priority 1: ₹ 100.50
        val amountRegex1 = Regex("""₹\s?(\d+(?:\.\d{1,2})?)""")
        // Priority 2: 100.50 ₹ (Since we normalized rs to ₹)
        val amountRegex2 = Regex("""(\d+(?:\.\d{1,2})?)\s?₹""")

        val match = amountRegex1.find(normalized) ?: amountRegex2.find(normalized)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractAccountHint(normalized: String): String? {
        // Matches "xx1234", "a/c 1234", "acct ending 1234", "ac 1234"
        val accountRegex = Regex("""(?:a/c|acct|ac|account|ending|xx|x)\s*(?:\w*\s*)?(\d{2,4})\b""")
        val match = accountRegex.find(normalized)
        return match?.groupValues?.get(1)
    }

    private fun extractMerchant(normalized: String): String? {
        // Match "at <merchant>", "to <merchant>", "paid to <merchant>", "via <merchant>"
        val merchantRegex = Regex("""(?:paid to|to|at|via)\s+([a-z0-9@.\-_ ]{3,30}?)(?:\s+(?:on|via|ref|upi|using)|\.|,|$)""")
        val match = merchantRegex.find(normalized)
        var merchant = match?.groupValues?.get(1)?.trim()

        if (merchant != null) {
            // Clean up UPI IDs or trailing punctuation
            merchant = merchant.replace(Regex("""@[a-z]+"""), "").trim()
            merchant = merchant.trimEnd('.', ',', '-', ' ')
            if (merchant.isBlank() || merchant.matches(Regex("""^\d+$"""))) {
                return null
            }
            return merchant
        }
        return null
    }

    private fun generateHash(amount: Double, type: TransactionType, timestamp: Long, sender: String, accountHint: String?): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val roundedDate = cal.timeInMillis

        val rawStr = "${amount}_${type.name}_${roundedDate}_${sender}_${accountHint ?: "NA"}"
        val digest = MessageDigest.getInstance("SHA-1")
        val hashBytes = digest.digest(rawStr.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun parse(smsBody: String, sender: String, timestamp: Long): Transaction? {
        val normalized = normalize(smsBody)

        if (!isTransactionMessage(normalized)) return null

        val type = detectType(normalized) ?: return null
        val amount = extractAmount(normalized) ?: return null
        
        // Edge case: balance-only checking
        if (amount < 1.0) return null

        val accountHint = extractAccountHint(normalized)
        val merchant = extractMerchant(normalized)
        
        // Confidence Scoring
        var score = 0.0
        if (type != null) score += 0.4
        if (amount > 0) score += 0.3
        if (KNOWN_BANKS.any { sender.contains(it, ignoreCase = true) }) score += 0.2
        if (accountHint != null) score += 0.1

        if (score < 0.6) return null

        val hashId = generateHash(amount, type, timestamp, sender, accountHint)

        // Identify Internal Wallet Transfers
        val isWalletTransfer = normalized.contains("paytm") || normalized.contains("phonepe") || 
                               normalized.contains("amazon pay") || normalized.contains("gpay") || 
                               normalized.contains("wallet")
        val isIgnored = isWalletTransfer && type == TransactionType.DEBIT
        val finalMerchant = merchant ?: if (isIgnored) "Wallet Reload" else "Unknown"

        return Transaction(
            id = "sms_$hashId",
            amount = amount,
            merchant = finalMerchant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            datetime = timestamp,
            type = type,
            category = if (normalized.contains("refund")) "Refund" else "Uncategorized",
            source = TransactionSource.SMS,
            isIgnored = isIgnored,
            accountHint = accountHint
        )
    }
}
