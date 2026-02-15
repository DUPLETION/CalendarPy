package com.pylearn.app.data.model

data class ProgressData(
    val currentWeek: String = "Неделя 1 — База Python",
    val currentDay: Int = 1,
    val completedDays: Map<String, Boolean> = emptyMap()
)

data class DayInfo(
    val title: String = "",
    val theory: String = "",
    val practice: String = "",
    val tasks: String = ""
)

data class WeekInfo(
    val name: String,
    val maxDay: Int
)

data class NotificationSettings(
    val enabled: Boolean = true,
    val hour: Int = 9,
    val minute: Int = 0
)
