package com.billwise.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.billwise.app.BillWiseApplication
import com.billwise.app.data.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    private val receiverScope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val body = sms.displayMessageBody
            val sender = sms.displayOriginatingAddress
            val timestamp = sms.timestampMillis

            receiverScope.launch {
                val transaction = SmsParser.parse(body, sender, timestamp)
                if (transaction != null) {
                    val app = context.applicationContext as BillWiseApplication
                    app.transactionRepository.addTransaction(transaction)
                }
            }
        }
    }
}
