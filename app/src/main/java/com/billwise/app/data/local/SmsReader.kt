package com.billwise.app.data.local

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.billwise.app.data.parser.SmsParser
import com.billwise.app.domain.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsReader(private val context: Context) {
    
    suspend fun readSmsMessages(): List<Transaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.ADDRESS
        )
        
        // Read only recent 500 messages to avoid freezing
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT 500"
        )?.use { cursor ->
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            
            while (cursor.moveToNext()) {
                val body = cursor.getString(bodyIndex)
                val date = cursor.getLong(dateIndex)
                val sender = cursor.getString(addressIndex)
                
                val transaction = SmsParser.parse(body, sender, date)
                if (transaction != null) {
                    transactions.add(transaction)
                }
            }
        }
        
        return@withContext transactions
    }
}
