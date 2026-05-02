package com.billwise.app.data.local

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.billwise.app.data.parser.SmsParser
import com.billwise.app.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsReader(private val context: Context) {
    
    suspend fun readSmsMessages(sinceTimestamp: Long = 0L): List<Transaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.ADDRESS
        )
        
        val selection = if (sinceTimestamp > 0) "${Telephony.Sms.DATE} > ?" else null
        val selectionArgs = if (sinceTimestamp > 0) arrayOf(sinceTimestamp.toString()) else null
        
        val rawMessages = mutableListOf<Triple<String, String, Long>>()
        
        // Read SMS messages efficiently
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${Telephony.Sms.DATE} DESC"
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIndex)
                val date = cursor.getLong(dateIndex)
                val sender = cursor.getString(addressIndex)
                rawMessages.add(Triple(body, sender, date))
            }
        }
        
        for ((body, sender, date) in rawMessages) {
            try {
                // For bulk reading, we disable AI to ensure speed and cost-efficiency
                val transaction = SmsParser.parse(body, sender, date, useAi = false)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            } catch (e: Exception) {
                // Never let one bad SMS break the whole sync
                e.printStackTrace()
            }
        }
        
        return@withContext transactions
    }
}
