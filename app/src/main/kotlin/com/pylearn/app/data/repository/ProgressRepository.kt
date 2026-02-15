package com.pylearn.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pylearn.app.data.model.DayInfo
import com.pylearn.app.data.model.NotificationSettings
import com.pylearn.app.data.model.ProgressData
import com.pylearn.app.data.model.WeekInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

class ProgressRepository(
    private val context: Context,
    private val prefs: SharedPreferences
) {
    private val gson = Gson()
    
    companion object {
        const val PREFS_NAME = "python_learn_prefs"
        const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val PREF_NOTIFICATION_HOUR = "notification_hour"
        const val PREF_NOTIFICATION_MINUTE = "notification_minute"
        
        fun createEncryptedPrefs(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
    
    suspend fun loadProgress(): ProgressData = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "progress.json")
            if (file.exists()) {
                val json = file.readText()
                val jsonObj = JSONObject(json)
                
                val completedDaysMap = mutableMapOf<String, Boolean>()
                val completedObj = jsonObj.optJSONObject("completed_days")
                completedObj?.let { obj ->
                    obj.keys().forEach { key ->
                        completedDaysMap[key] = obj.getBoolean(key)
                    }
                }
                
                ProgressData(
                    currentWeek = jsonObj.optString("current_week", "Неделя 1 — База Python"),
                    currentDay = jsonObj.optInt("current_day", 1),
                    completedDays = completedDaysMap
                )
            } else {
                ProgressData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ProgressData()
        }
    }
    
    suspend fun saveProgress(progress: ProgressData) = withContext(Dispatchers.IO) {
        try {
            val jsonObj = JSONObject()
            jsonObj.put("current_week", progress.currentWeek)
            jsonObj.put("current_day", progress.currentDay)
            
            val completedObj = JSONObject()
            progress.completedDays.forEach { (key, value) ->
                completedObj.put(key, value)
            }
            jsonObj.put("completed_days", completedObj)
            
            val file = File(context.filesDir, "progress.json")
            file.writeText(jsonObj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun resetProgress() = withContext(Dispatchers.IO) {
        saveProgress(ProgressData())
    }
    
    fun getNotificationSettings(): NotificationSettings {
        return NotificationSettings(
            enabled = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true),
            hour = prefs.getInt(PREF_NOTIFICATION_HOUR, 9),
            minute = prefs.getInt(PREF_NOTIFICATION_MINUTE, 0)
        )
    }
    
    fun saveNotificationSettings(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean(PREF_NOTIFICATIONS_ENABLED, settings.enabled)
            .putInt(PREF_NOTIFICATION_HOUR, settings.hour)
            .putInt(PREF_NOTIFICATION_MINUTE, settings.minute)
            .apply()
    }
    
    fun getWeeks(): List<WeekInfo> = listOf(
        WeekInfo("Неделя 1 — База Python", 6),
        WeekInfo("Неделя 2 — Структуры данных и функции", 6),
        WeekInfo("Неделя 3 — ООП", 6),
        WeekInfo("Неделя 4 — Практика и алгоритмы", 6),
        WeekInfo("Неделя 5 — Библиотеки", 6),
        WeekInfo("Неделя 6 — Telegram-бот", 6),
        WeekInfo("Неделя 7 — База данных", 6),
        WeekInfo("Неделя 8 — Финальный проект", 6)
    )
    
    suspend fun getDayInfo(week: String, day: Int, getDayInfoFromPython: (String, Int) -> Any?): DayInfo {
        return try {
            val result = getDayInfoFromPython(week, day)
            val jsonStr = result?.toString() ?: ""
            if (jsonStr.isNotEmpty() && jsonStr != "null") {
                try {
                    val jsonObj = JSONObject(jsonStr)
                    DayInfo(
                        title = jsonObj.optString("title", "День $day"),
                        theory = jsonObj.optString("theory", ""),
                        practice = jsonObj.optString("practice", ""),
                        tasks = jsonObj.optString("tasks", "")
                    )
                } catch (e: Exception) {
                    DayInfo(title = "День $day")
                }
            } else {
                DayInfo(title = "День $day")
            }
        } catch (e: Exception) {
            DayInfo(title = "День $day")
        }
    }
    
    suspend fun getMaxDay(week: String, getMaxDayFromPython: (String) -> Int): Int {
        return try {
            getMaxDayFromPython(week)
        } catch (e: Exception) {
            6
        }
    }
}
