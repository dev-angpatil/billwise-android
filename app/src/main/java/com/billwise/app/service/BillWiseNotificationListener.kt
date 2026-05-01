package com.billwise.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.billwise.app.BillWiseApplication
import com.billwise.app.data.parser.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BillWiseNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        sbn ?: return

        val packageName = sbn.packageName
        
        // Listen specifically to PhonePe, GPay, Paytm, etc.
        val targetApps = listOf("com.phonepe.app", "com.google.android.apps.nbu.paisa.user", "net.one97.paytm")
        if (packageName !in targetApps) return

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        
        val fullText = "$title $text"
        
        serviceScope.launch {
            val app = application as BillWiseApplication
            val repository = app.transactionRepository
            
            val sender = when (packageName) {
                "com.phonepe.app" -> "PHONEPE"
                "com.google.android.apps.nbu.paisa.user" -> "GPAY"
                "net.one97.paytm" -> "PAYTM"
                else -> "UNKNOWN"
            }

            // Reuse SmsParser for notification content
            val parsedTx = SmsParser.parse(fullText, sender, sbn.postTime)
            
            if (parsedTx != null) {
                // Ensure duplicate handling via use case or DB REPLACE
                repository.addTransaction(parsedTx)
            }
        }
    }
}
