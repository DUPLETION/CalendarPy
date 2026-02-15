package com.pylearn.app

import android.app.Application
import android.content.Context
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.pylearn.app.data.repository.ProgressRepository
import com.pylearn.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Python.start(AndroidPlatform(this))
        
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(appModule)
        }
    }
}

object AppHelper {
    fun rescheduleNotifications(context: Context) {
        try {
            val prefs = ProgressRepository.createEncryptedPrefs(context)
            val settings = ProgressRepository(context, prefs).getNotificationSettings()
            
            if (settings.enabled) {
                val intent = android.content.Intent(context, NotificationReceiver::class.java)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    0,
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.cancel(pendingIntent)
                
                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, settings.hour)
                    set(java.util.Calendar.MINUTE, settings.minute)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                
                var triggerTime = calendar.timeInMillis
                if (triggerTime <= System.currentTimeMillis()) {
                    triggerTime += 24 * 60 * 60 * 1000
                }
                
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    android.app.AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
