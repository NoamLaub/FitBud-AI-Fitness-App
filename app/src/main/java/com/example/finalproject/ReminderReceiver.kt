package com.example.finalproject

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "onReceive called: intent=$intent")
        val title = intent.getStringExtra("title") ?: "Reminder"
        val text = intent.getStringExtra("text") ?: "It's time!"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    val channel = NotificationChannel(
        "reminder_channel",
        "Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
    )
    channel.setSound(soundUri, null)
    notificationManager.createNotificationChannel(channel)
}
val notification = NotificationCompat.Builder(context, "reminder_channel")
    .setSmallIcon(android.R.drawable.ic_dialog_info)
    .setContentTitle(title)
    .setContentText(text)
    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    .setAutoCancel(true)
    .setSound(soundUri)
    .build()
notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
