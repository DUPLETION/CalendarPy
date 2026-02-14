package com.pylearn.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.chaquo.python.Python
import org.json.JSONObject
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var python: Python
    private lateinit var progressData: JSONObject
    private lateinit var prefs: SharedPreferences
    
    companion object {
        const val CHANNEL_ID = "python_learn_channel"
        const val PREFS_NAME = "python_learn_prefs"
        const val PREF_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val PREF_NOTIFICATION_HOUR = "notification_hour"
        const val PREF_NOTIFICATION_MINUTE = "notification_minute"
    }
    
    private var isDayDetailShown = false

    private fun handleBack(): Boolean {
        return when {
            isDayDetailShown -> {
                isDayDetailShown = false
                setupMainScreen()
                true
            }
            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        python = Python.getInstance()
        
        loadProgress()
        setupMainScreen()
        createNotificationChannel()
        requestNotificationPermission()
        
        if (prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true)) {
            scheduleNotifications()
        }
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!handleBack()) {
            showExitConfirmDialog()
        }
    }
    
    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Выйти") { _, _ ->
                finish()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun loadProgress() {
        try {
            val file = java.io.File(filesDir, "progress.json")
            if (file.exists()) {
                val json = file.readText()
                progressData = JSONObject(json)
            } else {
                progressData = JSONObject()
                progressData.put("current_week", "Неделя 1 — База Python")
                progressData.put("current_day", 1)
                progressData.put("completed_days", JSONObject())
            }
        } catch (e: Exception) {
            progressData = JSONObject()
            progressData.put("current_week", "Неделя 1 — База Python")
            progressData.put("current_day", 1)
            progressData.put("completed_days", JSONObject())
        }
    }
    
    private fun saveProgress() {
        try {
            val file = java.io.File(filesDir, "progress.json")
            file.writeText(progressData.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun setupMainScreen() {
        isDayDetailShown = false
        setContentView(createMainContentView())
    }
    
    private fun createMainContentView(): ScrollView {
        val scrollView = ScrollView(this).apply {
            id = 888
            setBackgroundColor(0xFFffffff.toInt())
            isFocusable = true
            descendantFocusability = android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true
            descendantFocusability = android.view.ViewGroup.FOCUS_BEFORE_DESCENDANTS
        }
        
        val titleBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 60, 20, 20)
            setBackgroundColor(AppColors.PRIMARY)
        }
        
        val title = TextView(this).apply {
            text = "Python Learn"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(title)
        
        val settingsBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 24f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(20, 0, 0, 0)
        }
        settingsBtn.setOnClickListener {
            showSettingsDialog()
        }
        titleBar.addView(settingsBtn)
        
        layout.addView(titleBar)
        
        val editorCard = CardView(this).apply {
            setCardBackgroundColor(AppColors.PRIMARY_LIGHT)
            radius = 16f
            cardElevation = 2f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 16, 16, 8)
            layoutParams = params
            isClickable = true
            isFocusable = true
        }
        
        val editorContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(20, 20, 20, 20)
        }
        
        val editorIcon = TextView(this).apply {
            text = "⚡"
            textSize = 28f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        editorContent.addView(editorIcon)
        
        val editorText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 0, 0, 0)
        }
        
        editorText.addView(TextView(this).apply {
            text = "Редактор кода"
            textSize = 16f
            setTextColor(AppColors.TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        editorText.addView(TextView(this).apply {
            text = "Пиши и запускай Python-код"
            textSize = 13f
            setTextColor(AppColors.TEXT_MEDIUM)
        })
        
        editorContent.addView(editorText)
        editorCard.addView(editorContent)
        
        editorCard.setOnClickListener {
            startActivity(android.content.Intent(this, EditorActivity::class.java))
        }
        
        layout.addView(editorCard)
        
        val currentWeek = progressData.getString("current_week")
        val currentDay = progressData.getInt("current_day")
        val pyModule = python.getModule("main")
        
        val weeks = listOf(
            "Неделя 1 — База Python",
            "Неделя 2 — Структуры данных и функции",
            "Неделя 3 — ООП",
            "Неделя 4 — Практика и алгоритмы",
            "Неделя 5 — Библиотеки",
            "Неделя 6 — Telegram-бот",
            "Неделя 7 — База данных",
            "Неделя 8 — Финальный проект"
        )
        
        for (weekName in weeks) {
            val maxDay = try {
                pyModule.callAttr("get_max_day", weekName).toInt()
            } catch (e: Exception) {
                6
            }
            
            val weekCard = CardView(this).apply {
                setCardBackgroundColor(AppColors.CARD)
                radius = 20f
                cardElevation = 2f
                setContentPadding(20, 16, 20, 16)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(16, 10, 16, 10)
                layoutParams = params
            }
            
            val weekLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            val weekTitle = TextView(this).apply {
                text = weekName
                textSize = 15f
                setTextColor(AppColors.PRIMARY)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }
            weekLayout.addView(weekTitle)
            
            val daysGrid = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            
            for (dayNum in 1..maxDay) {
                val dayInfo = pyModule.callAttr("get_day_info", weekName, dayNum)
                
                val key = "${weekName}_$dayNum"
                val completedObj = progressData.optJSONObject("completed_days")
                val isCompleted = if (completedObj != null) completedObj.optBoolean(key, false) else false
                val isCurrentDay = weekName == currentWeek && dayNum == currentDay
                
                val dayCard = CardView(this).apply {
                    radius = 16f
                    cardElevation = 3f
                    setCardBackgroundColor(
                        when {
                            isCompleted -> AppColors.COMPLETED
                            isCurrentDay -> AppColors.CURRENT
                            else -> 0xFFF3F4F6.toInt()
                        }
                    )
                    val params = LinearLayout.LayoutParams(0, 200, 1f)
                    params.setMargins(12, 0, 12, 0)
                    layoutParams = params
                    isClickable = true
                    isFocusable = true
                }
                
                dayCard.setOnClickListener {
                    showDayDetails(weekName, dayNum)
                }
                
                val dayContent = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                }
                
                val dayNumText = TextView(this).apply {
                    text = dayNum.toString()
                    textSize = 28f
                    setTextColor(AppColors.TEXT_DARK)
                    gravity = android.view.Gravity.CENTER
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                dayContent.addView(dayNumText)

                if (dayInfo != null) {
                    val titleValue = dayInfo.toString()
                    if (titleValue.isNotEmpty() && titleValue != "None") {
                        try {
                            val jsonObj = JSONObject(titleValue)
                            val shortTitle = jsonObj.optString("title", "").replace("День $dayNum: ", "").take(14)
                            if (shortTitle.isNotEmpty()) {
                                val daySubtitle = TextView(this).apply {
                                    text = shortTitle
                                    textSize = 11f
                                    setTextColor(AppColors.TEXT_MEDIUM)
                                    gravity = android.view.Gravity.CENTER
                                    maxLines = 2
                                }
                                dayContent.addView(daySubtitle)
                            }
                        } catch (e: Exception) {
                            val titleShort = titleValue.take(14)
                            val daySubtitle = TextView(this).apply {
                                text = titleShort
                                textSize = 11f
                                setTextColor(AppColors.TEXT_MEDIUM)
                                gravity = android.view.Gravity.CENTER
                                maxLines = 2
                            }
                            dayContent.addView(daySubtitle)
                        }
                    }
                }
                
                dayCard.addView(dayContent)
                daysGrid.addView(dayCard)
            }
            
            weekLayout.addView(daysGrid)
            weekCard.addView(weekLayout)
            layout.addView(weekCard)
        }
        
        val motivation = TextView(this).apply {
            text = "💡 ${Messages.motivationQuotes.random()}"
            textSize = 14f
            setTextColor(AppColors.TEXT_MEDIUM)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(20, 28, 20, 28)
            layoutParams = params
        }
        layout.addView(motivation)

        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 0, 16, 40)
            layoutParams = params
        }
        
        legend.addView(TextView(this).apply {
            text = "🟢 Выполнено"
            textSize = 12f
            setTextColor(AppColors.TEXT_MEDIUM)
        })
        legend.addView(TextView(this).apply {
            text = "  🔵 Сегодня  "
            textSize = 12f
            setTextColor(AppColors.PRIMARY)
        })
        legend.addView(TextView(this).apply {
            text = "  ⚪ Не выполнено"
            textSize = 12f
            setTextColor(AppColors.TEXT_LIGHT)
        })
        layout.addView(legend)
        
        scrollView.addView(layout)
        return scrollView
    }
    
    private fun showDayDetails(week: String, day: Int) {
        isDayDetailShown = true
        setContentView(createDayDetailView(week, day))
    }
    
    private fun createDayDetailView(week: String, day: Int): ScrollView {
        val scrollView = ScrollView(this).apply {
            id = 888
            setBackgroundColor(AppColors.BG)
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 52, 20, 20)
            setBackgroundColor(AppColors.PRIMARY)
        }
        
        val backBtn = TextView(this).apply {
            text = "←"
            textSize = 26f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 20, 0)
        }
        backBtn.setOnClickListener {
            setupMainScreen()
        }
        headerBar.addView(backBtn)
        
        val title = TextView(this).apply {
            text = "Назад"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerBar.addView(title)
        layout.addView(headerBar)
        
        val contentView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        
        val pyModule = python.getModule("main")
        val dayData = pyModule.callAttr("get_day_info", week, day)
        
        var titleText: String = "День $day"
        var theoryText: String = ""
        var practiceText: String = ""
        var tasksText: String = ""
        
        try {
            val jsonStr = dayData.toString()
            val jsonObj = JSONObject(jsonStr)
            titleText = jsonObj.optString("title", "День $day")
            theoryText = jsonObj.optString("theory", "")
            practiceText = jsonObj.optString("practice", "")
            tasksText = jsonObj.optString("tasks", "")
        } catch (e: Exception) {
        }
        
        val weekTitleView = TextView(this).apply {
            text = week
            textSize = 14f
            setTextColor(AppColors.PRIMARY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 8
            layoutParams = params
        }
        contentView.addView(weekTitleView)
        
        val dayTitleView = TextView(this).apply {
            text = titleText
            textSize = 24f
            setTextColor(AppColors.TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 24
            layoutParams = params
        }
        contentView.addView(dayTitleView)
        
        if (theoryText.isNotEmpty()) {
            val theoryCard = CardView(this).apply {
                setCardBackgroundColor(AppColors.CARD)
                radius = 16f
                cardElevation = 1f
                setContentPadding(20, 16, 20, 16)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 16
                layoutParams = params
            }
            
            val theoryLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            val theoryHeader = TextView(this).apply {
                text = "📚 Теория"
                textSize = 16f
                setTextColor(AppColors.PRIMARY)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                layoutParams = params
            }
            theoryLayout.addView(theoryHeader)
            
            val theoryContent = TextView(this).apply {
                text = theoryText.replace(", ", "\n• ").let { "• $it" }
                textSize = 15f
                setTextColor(AppColors.TEXT_DARK)
                setLineSpacing(4f, 1.3f)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = params
            }
            theoryLayout.addView(theoryContent)
            theoryCard.addView(theoryLayout)
            contentView.addView(theoryCard)
        }
        
        if (practiceText.isNotEmpty()) {
            val practiceCard = CardView(this).apply {
                setCardBackgroundColor(AppColors.CARD)
                radius = 16f
                cardElevation = 1f
                setContentPadding(20, 16, 20, 16)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 16
                layoutParams = params
            }
            
            val practiceLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            val practiceHeader = TextView(this).apply {
                text = "👉 Практика"
                textSize = 16f
                setTextColor(AppColors.PRIMARY)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                layoutParams = params
            }
            practiceLayout.addView(practiceHeader)
            
            val practiceContent = TextView(this).apply {
                text = practiceText
                textSize = 15f
                setTextColor(AppColors.TEXT_DARK)
                setLineSpacing(4f, 1.3f)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = params
            }
            practiceLayout.addView(practiceContent)
            practiceCard.addView(practiceLayout)
            contentView.addView(practiceCard)
        }
        
        if (tasksText.isNotEmpty()) {
            val tasksCard = CardView(this).apply {
                setCardBackgroundColor(AppColors.CARD)
                radius = 16f
                cardElevation = 1f
                setContentPadding(20, 16, 20, 16)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 24
                layoutParams = params
            }
            
            val tasksLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            val tasksHeader = TextView(this).apply {
                text = "📝 Задачи"
                textSize = 16f
                setTextColor(AppColors.PRIMARY)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                layoutParams = params
            }
            tasksLayout.addView(tasksHeader)
            
            val tasksContent = TextView(this).apply {
                text = tasksText.replace(", ", "\n• ").let { "• $it" }
                textSize = 15f
                setTextColor(AppColors.TEXT_DARK)
                setLineSpacing(4f, 1.3f)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams = params
            }
            tasksLayout.addView(tasksContent)
            tasksCard.addView(tasksLayout)
            contentView.addView(tasksCard)
        }
        
        val key = "${week}_$day"
        val completedObj = progressData.optJSONObject("completed_days")
        val isCompleted = if (completedObj != null) completedObj.optBoolean(key, false) else false
        
        val buttonCard = CardView(this).apply {
            setCardBackgroundColor(if (isCompleted) AppColors.COMPLETED else AppColors.PRIMARY_LIGHT)
            radius = 16f
            cardElevation = 0f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 8
            layoutParams = params
        }
        
        val actionButton = Button(this).apply {
            text = if (isCompleted) "↩️ Отменить" else "✅ Выполнено"
            textSize = 16f
            setTextColor(if (isCompleted) AppColors.SUCCESS else AppColors.PRIMARY)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(20, 18, 20, 18)
            layoutParams = params
        }
        
        actionButton.setOnClickListener {
            try {
                var completedObj = progressData.optJSONObject("completed_days")
                if (completedObj == null) {
                    completedObj = JSONObject()
                    progressData.put("completed_days", completedObj)
                }
                val newState = !isCompleted
                completedObj.put(key, newState)
                saveProgress()
                if (newState) {
                    Toast.makeText(this, "День $day выполнен!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Выполнение отменено", Toast.LENGTH_SHORT).show()
                }
                showDayDetails(week, day)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        buttonCard.addView(actionButton)
        contentView.addView(buttonCard)
        
        layout.addView(contentView)
        scrollView.addView(layout)
        
        return scrollView
    }
    
    private fun showSettingsDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 30, 40, 20)
        }
        
        val title = TextView(this).apply {
            text = "Настройки"
            textSize = 22f
            setTextColor(AppColors.TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        dialogView.addView(title)
        
        val notificationsCard = CardView(this).apply {
            setCardBackgroundColor(0xFFF8FAFC.toInt())
            radius = 16f
            cardElevation = 1f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 24
            layoutParams = params
        }
        
        val notificationsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
        }
        
        val notificationsSwitch = CheckBox(this).apply {
            text = "Включить уведомления"
            textSize = 16f
            setTextColor(AppColors.TEXT_DARK)
            isChecked = prefs.getBoolean(PREF_NOTIFICATIONS_ENABLED, true)
        }
        notificationsLayout.addView(notificationsSwitch)

        notificationsCard.addView(notificationsLayout)
        dialogView.addView(notificationsCard)
        
        val timeCard = CardView(this).apply {
            setCardBackgroundColor(0xFFF8FAFC.toInt())
            radius = 16f
            cardElevation = 1f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 16
            layoutParams = params
        }
        
        val timeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
        }
        
        val timeLabel = TextView(this).apply {
            text = "Время уведомлений:"
            textSize = 15f
            setTextColor(AppColors.TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12
            layoutParams = params
        }
        timeLayout.addView(timeLabel)
        
        val savedHour = prefs.getInt(PREF_NOTIFICATION_HOUR, 9)
        val savedMinute = prefs.getInt(PREF_NOTIFICATION_MINUTE, 0)
        
        val timeButton = Button(this).apply {
            text = String.format("%02d:%02d", savedHour, savedMinute)
            textSize = 20f
            setTextColor(AppColors.PRIMARY)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setBackgroundColor(0xFFFFFFFF.toInt())
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = params
        }
        
        timeButton.setOnClickListener {
            val timePicker = TimePickerDialog(this,
                { _, hourOfDay, minute ->
                    timeButton.text = String.format("%02d:%02d", hourOfDay, minute)
                    prefs.edit().putInt(PREF_NOTIFICATION_HOUR, hourOfDay).apply()
                    prefs.edit().putInt(PREF_NOTIFICATION_MINUTE, minute).apply()
                }, savedHour, savedMinute, true)
            timePicker.show()
        }
        timeLayout.addView(timeButton)
        
        timeCard.addView(timeLayout)
        dialogView.addView(timeCard)
        
        val testCard = CardView(this).apply {
            setCardBackgroundColor(0xFFF8FAFC.toInt())
            radius = 16f
            cardElevation = 1f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 16
            layoutParams = params
        }
        
        val testLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
        }
        
        val testLabel = TextView(this).apply {
            text = "Тест уведомлений:"
            textSize = 15f
            setTextColor(AppColors.TEXT_DARK)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = 12
            layoutParams = params
        }
        testLayout.addView(testLabel)
        
        val testButton = Button(this).apply {
            text = "Отправить тестовое уведомление"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(AppColors.PRIMARY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = params
        }
        
        testButton.setOnClickListener {
            sendTestNotification()
            Toast.makeText(this@MainActivity, "Тестовое уведомление отправлено!", Toast.LENGTH_SHORT).show()
        }
        testLayout.addView(testButton)
        
        testCard.addView(testLayout)
        dialogView.addView(testCard)
        
        val resetBtn = Button(this).apply {
            text = "Сбросить прогресс"
            textSize = 15f
            setTextColor(AppColors.DANGER)
            setBackgroundColor(AppColors.DANGER_BG)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 24
            layoutParams = params
        }
        resetBtn.setOnClickListener {
            showResetConfirmDialog()
        }
        dialogView.addView(resetBtn)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val notificationsEnabled = notificationsSwitch.isChecked
                prefs.edit().putBoolean(PREF_NOTIFICATIONS_ENABLED, notificationsEnabled).apply()
                
                if (notificationsEnabled) {
                    scheduleNotifications()
                    Toast.makeText(this, "Уведомления включены", Toast.LENGTH_SHORT).show()
                } else {
                    cancelNotifications()
                    Toast.makeText(this, "Уведомления выключены", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()
        
        dialog.show()
    }
    
    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Сброс прогресса")
            .setMessage("Вы уверены, что хотите сбросить весь прогресс? Это действие нельзя отменить.")
            .setPositiveButton("Сбросить") { _, _ ->
                resetProgress()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun resetProgress() {
        progressData = JSONObject()
        progressData.put("current_week", "Неделя 1 — База Python")
        progressData.put("current_day", 1)
        progressData.put("completed_days", JSONObject())
        saveProgress()
        setupMainScreen()
        Toast.makeText(this, "Прогресс сброшен", Toast.LENGTH_SHORT).show()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о Python",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ежедневные напоминания для изучения Python"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }
    
    private fun scheduleNotifications() {
        try {
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = getSystemService(AlarmManager::class.java)
            alarmManager.cancel(pendingIntent)
            
            val calendar = Calendar.getInstance()
            val savedHour = prefs.getInt(PREF_NOTIFICATION_HOUR, 9)
            val savedMinute = prefs.getInt(PREF_NOTIFICATION_MINUTE, 0)
            
            calendar.set(Calendar.HOUR_OF_DAY, savedHour)
            calendar.set(Calendar.MINUTE, savedMinute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            var triggerTime = calendar.timeInMillis
            if (triggerTime <= System.currentTimeMillis()) {
                triggerTime += 24 * 60 * 60 * 1000
            }
            
            val interval = AlarmManager.INTERVAL_DAY
            
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                interval,
                pendingIntent
            )
            
            Toast.makeText(this, "Уведомление запланировано на $savedHour:$savedMinute", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка планирования: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun sendTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о Python",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Python Learning")
            .setContentText(Messages.testNotificationMessages.random())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(0, notification)
    }
    
    private fun cancelNotifications() {
        try {
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = getSystemService(AlarmManager::class.java)
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MainActivity.CHANNEL_ID,
                "Напоминания о Python",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = android.app.Notification.Builder(context, MainActivity.CHANNEL_ID)
            .setContentTitle("Python Learning")
            .setContentText(Messages.notificationMessages.random())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(1, notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.getBoolean(MainActivity.PREF_NOTIFICATIONS_ENABLED, true)) {
                val calendar = Calendar.getInstance()
                val savedHour = prefs.getInt(MainActivity.PREF_NOTIFICATION_HOUR, 9)
                val savedMinute = prefs.getInt(MainActivity.PREF_NOTIFICATION_MINUTE, 0)
                
                calendar.set(Calendar.HOUR_OF_DAY, savedHour)
                calendar.set(Calendar.MINUTE, savedMinute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                var triggerTime = calendar.timeInMillis
                if (triggerTime <= System.currentTimeMillis()) {
                    triggerTime += 24 * 60 * 60 * 1000
                }
                
                val alarmIntent = Intent(context, NotificationReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    0,
                    alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    24 * 60 * 60 * 1000L,
                    pendingIntent
                )
            }
        }
    }
}
