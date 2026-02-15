package com.pylearn.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                context.getString(R.string.channel_id),
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val messages = listOf(
            "Код не работает? Отлично. Ты только что нашёл ещё один способ, как НЕ надо делать.",
            "Программист — это человек, который превращает кофе в код.",
            "Ошибка — это не провал, это бесплатный урок.",
            "Каждый баг — это мини-квест.",
            "Если код заработал с первого раза — ты что-то забыл.",
            "Stack Overflow — лучший друг интроверта.",
            "Программирование — это спорт. Только вместо гантелей — переменные.",
            "Сегодня больно, завтра — senior.",
            "Чем больше ошибок, тем ближе успех.",
            "Настоящий разработчик не сдаётся — он гуглит."
        )
        
        val notification = android.app.Notification.Builder(context, context.getString(R.string.channel_id))
            .setContentTitle(context.getString(R.string.python_learning))
            .setContentText(messages.random())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
}
