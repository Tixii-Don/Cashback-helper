package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar

class CashbackNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("cashback_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notifications_enabled", false)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (isEnabled) {
                scheduleMonthlyAlarm(context)
            }
            return
        }

        if (isEnabled) {
            showNotification(context)
            scheduleMonthlyAlarm(context)
        }
    }

    private fun showNotification(context: Context) {
        val channelId = "cashback_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Кешбэк напоминания",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о необходимости внести кешбэк на следующий месяц"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📝 Время обновить кешбэк!")
            .setContentText("Наступает новый месяц! Самое время внести распределение кешбэка по вашим картам, чтобы получать максимальную выгоду!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Наступает новый месяц! Самое время внести и обновить распределение кешбэка по всем вашим картам, чтобы не упустить максимальную процентную ставку!"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }

    companion object {
        fun scheduleMonthlyAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, CashbackNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                val today = get(Calendar.DAY_OF_MONTH)
                if (today >= 28) {
                    add(Calendar.MONTH, 1)
                }
                set(Calendar.DAY_OF_MONTH, 28)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Schedule the alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        fun cancelMonthlyAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, CashbackNotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        fun sendImmediateTestNotification(context: Context) {
            val receiver = CashbackNotificationReceiver()
            receiver.showNotification(context)
        }
    }
}
