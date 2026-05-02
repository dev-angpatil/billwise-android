package com.billwise.app.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.billwise.app.R

class NotificationHelper(private val context: Context) {

    private val CHANNEL_ID = "budget_alerts"
    private val CHANNEL_NAME = "Budget Alerts"
    private val NOTIFICATION_ID_BASE = 1000

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for spending limits"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendBudgetAlert(
        category: String,
        spent: Double,
        limit: Double,
        isUrgent: Boolean
    ) {
        val remaining = limit - spent
        val emoji = getCategoryEmoji(category)
        val title = if (isUrgent) "🚨 Budget Limit Crossed!" else "⚠️ Budget Alert"
        val message = if (isUrgent) {
            "$emoji $category — ₹${spent.toInt()} spent of ₹${limit.toInt()} limit. You are overspent!"
        } else {
            "$emoji $category — ₹${spent.toInt()} spent of ₹${limit.toInt()} limit. Only ₹${remaining.toInt()} left this month."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID_BASE + category.hashCode(), builder.build())
    }

    private fun getCategoryEmoji(category: String): String {
        return when (category) {
            "Food & Dining" -> "🍔"
            "Transport & Travel" -> "🚗"
            "Groceries" -> "🛒"
            "Shopping" -> "🛍️"
            "Subscriptions & Entertainment" -> "🎬"
            "Health & Medical" -> "💊"
            "Utilities & Bills" -> "💡"
            "Investment" -> "📈"
            "Education" -> "📚"
            else -> "💰"
        }
    }
}
