package com.pylearn.app.ui.main

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pylearn.app.NotificationReceiver
import com.pylearn.app.data.model.DayInfo
import com.pylearn.app.data.model.NotificationSettings
import com.pylearn.app.data.model.ProgressData
import com.pylearn.app.data.model.WeekInfo
import com.pylearn.app.data.repository.ProgressRepository
import com.chaquo.python.Python
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(
    private val repository: ProgressRepository,
    private val context: Context
) : ViewModel() {
    
    private val python by lazy { Python.getInstance() }
    private val pyModule by lazy { python.getModule("main") }
    
    private val _progressData = MutableLiveData<ProgressData>()
    val progressData: LiveData<ProgressData> = _progressData
    
    private val _weeks = MutableLiveData<List<WeekInfo>>()
    val weeks: LiveData<List<WeekInfo>> = _weeks
    
    private val _currentDayInfo = MutableLiveData<DayInfo>()
    val currentDayInfo: LiveData<DayInfo> = _currentDayInfo
    
    private val _notificationSettings = MutableLiveData<NotificationSettings>()
    val notificationSettings: LiveData<NotificationSettings> = _notificationSettings
    
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage
    
    init {
        loadProgress()
        loadWeeks()
        loadNotificationSettings()
    }
    
    private fun loadProgress() {
        viewModelScope.launch {
            _progressData.value = repository.loadProgress()
        }
    }
    
    private fun loadWeeks() {
        viewModelScope.launch {
            val weeksList = repository.getWeeks().map { week ->
                val maxDay = try {
                    pyModule.callAttr("get_max_day", week.name).toInt()
                } catch (e: Exception) {
                    6
                }
                WeekInfo(week.name, maxDay)
            }
            _weeks.value = weeksList
        }
    }
    
    private fun loadNotificationSettings() {
        _notificationSettings.value = repository.getNotificationSettings()
    }
    
    fun loadDayInfo(week: String, day: Int) {
        viewModelScope.launch {
            val dayInfo = repository.getDayInfo(week, day) { w, d ->
                pyModule.callAttr("get_day_info", w, d)
            }
            _currentDayInfo.value = dayInfo
        }
    }
    
    fun toggleDayCompletion(week: String, day: Int) {
        viewModelScope.launch {
            val current = _progressData.value ?: return@launch
            val key = "${week}_$day"
            val isCompleted = current.completedDays[key] ?: false
            
            val newCompletedDays = current.completedDays.toMutableMap()
            newCompletedDays[key] = !isCompleted
            
            val updated = current.copy(completedDays = newCompletedDays)
            repository.saveProgress(updated)
            _progressData.value = updated
            
            _toastMessage.value = if (!isCompleted) "День $day выполнен!" else "Выполнение отменено"
        }
    }
    
    fun isDayCompleted(week: String, day: Int): Boolean {
        val key = "${week}_$day"
        return _progressData.value?.completedDays?.get(key) ?: false
    }
    
    fun isCurrentDay(week: String, day: Int): Boolean {
        val current = _progressData.value ?: return false
        return current.currentWeek == week && current.currentDay == day
    }
    
    fun saveNotificationSettings(settings: NotificationSettings) {
        repository.saveNotificationSettings(settings)
        _notificationSettings.value = settings
        
        if (settings.enabled) {
            scheduleNotifications(settings)
        } else {
            cancelNotifications()
        }
    }
    
    fun scheduleNotifications(settings: NotificationSettings = repository.getNotificationSettings()) {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
            
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, settings.hour)
                set(Calendar.MINUTE, settings.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            var triggerTime = calendar.timeInMillis
            if (triggerTime <= System.currentTimeMillis()) {
                triggerTime += 24 * 60 * 60 * 1000
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
            
            _toastMessage.value = "Уведомление запланировано на ${String.format("%02d:%02d", settings.hour, settings.minute)}"
        } catch (e: Exception) {
            _toastMessage.value = "Ошибка планирования: ${e.message}"
            e.printStackTrace()
        }
    }
    
    fun cancelNotifications() {
        try {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun resetProgress() {
        viewModelScope.launch {
            repository.resetProgress()
            loadProgress()
            _toastMessage.value = "Прогресс сброшен"
        }
    }
    
    fun clearToastMessage() {
        _toastMessage.value = null
    }
    
    fun rescheduleAfterBoot() {
        viewModelScope.launch {
            val settings = repository.getNotificationSettings()
            if (settings.enabled) {
                scheduleNotifications(settings)
            }
        }
    }
}
