package com.pylearn.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.pylearn.app.data.model.NotificationSettings
import com.pylearn.app.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModel()
    
    private var isDayDetailShown = false
    private var currentWeek: String = ""
    private var currentDay: Int = 0
    
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (!handleBack()) {
                showExitConfirmDialog()
            }
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scheduleNotifications()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        
        setupMainScreen()
        createNotificationChannel()
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage()
            }
        }
    }
    
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
    
    private fun showExitConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.exit)
            .setMessage(R.string.exit_confirm)
            .setPositiveButton(R.string.exit) { _, _ ->
                finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun setupMainScreen() {
        isDayDetailShown = false
        setContentView(createMainContentView())
    }
    
    private fun createMainContentView(): ScrollView {
        val scrollView = ScrollView(this).apply {
            id = 888
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
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
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(20), dpToPx(24), dpToPx(20), dpToPx(16))
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
        }
        
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleBar.addView(title)
        
        val settingsBtn = TextView(this).apply {
            text = "⚙️"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
            gravity = android.view.Gravity.CENTER
        }
        settingsBtn.setOnClickListener {
            showSettingsDialog()
        }
        titleBar.addView(settingsBtn)
        
        layout.addView(titleBar)
        
        val editorCard = createEditorCard()
        layout.addView(editorCard)
        
        viewModel.weeks.observe(this) { weeks ->
            viewModel.progressData.observe(this) { progress ->
                weeks.forEach { week ->
                    val weekCard = createWeekCard(week.name, week.maxDay, progress.currentWeek, progress.currentDay, progress.completedDays)
                    layout.addView(weekCard)
                }
            }
        }
        
        val motivation = createMotivationText()
        layout.addView(motivation)
        
        val legend = createLegend()
        layout.addView(legend)
        
        scrollView.addView(layout)
        return scrollView
    }
    
    private fun createEditorCard(): CardView {
        val card = CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary_light))
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(8))
            layoutParams = params
            isClickable = true
            isFocusable = true
        }
        
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
        }
        
        content.addView(TextView(this).apply {
            text = "⚡"
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })
        
        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(12), 0, 0, 0)
        }
        
        textLayout.addView(TextView(this).apply {
            text = getString(R.string.code_editor)
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        textLayout.addView(TextView(this).apply {
            text = getString(R.string.code_editor_subtitle)
            textSize = 12f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_medium))
        })
        
        content.addView(textLayout)
        card.addView(content)
        
        card.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }
        
        return card
    }
    
    private fun createWeekCard(weekName: String, maxDay: Int, currentWeek: String, currentDay: Int, completedDays: Map<String, Boolean>): CardView {
        val card = CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setContentPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6))
            layoutParams = params
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        layout.addView(TextView(this).apply {
            text = weekName
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, dpToPx(6))
            layoutParams = params
        })
        
        val daysGrid = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        for (dayNum in 1..maxDay) {
            val key = "${weekName}_$dayNum"
            val isCompleted = completedDays[key] ?: false
            val isCurrentDay = weekName == currentWeek && dayNum == currentDay
            
            val dayCard = createDayCard(dayNum, isCompleted, isCurrentDay, weekName)
            daysGrid.addView(dayCard)
        }
        
        layout.addView(daysGrid)
        card.addView(layout)
        
        return card
    }
    
    private fun createDayCard(dayNum: Int, isCompleted: Boolean, isCurrentDay: Boolean, weekName: String): CardView {
        val bgColor = when {
            isCompleted -> ContextCompat.getColor(this, R.color.completed)
            isCurrentDay -> ContextCompat.getColor(this, R.color.current)
            else -> 0xFFF3F4F6.toInt()
        }
        
        val size = dpToPx(36)
        val card = CardView(this).apply {
            radius = (size / 2).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(bgColor)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            layoutParams = params
            isClickable = true
            isFocusable = true
        }
        
        card.setOnClickListener {
            currentWeek = weekName
            currentDay = dayNum
            showDayDetails(weekName, dayNum)
        }
        
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        content.addView(TextView(this).apply {
            text = dayNum.toString()
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        
        card.addView(content)
        return card
    }
    
    private fun createMotivationText(): TextView {
        val quotes = listOf(
            getString(R.string.motivation_1),
            getString(R.string.motivation_2),
            getString(R.string.motivation_3),
            getString(R.string.motivation_4),
            getString(R.string.motivation_5)
        )
        
        return TextView(this).apply {
            text = "💡 ${quotes.random()}"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_medium))
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            layoutParams = params
        }
    }
    
    private fun createLegend(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(16), 0, dpToPx(16), dpToPx(20))
            layoutParams = params
        }.apply {
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.legend_completed)
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_medium))
            })
            addView(TextView(this@MainActivity).apply {
                text = "  ${getString(R.string.legend_today)}  "
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
            })
            addView(TextView(this@MainActivity).apply {
                text = "  ${getString(R.string.legend_not_completed)}"
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_light))
            })
        }
    }
    
    private fun showDayDetails(week: String, day: Int) {
        isDayDetailShown = true
        currentWeek = week
        currentDay = day
        viewModel.loadDayInfo(week, day)
        setContentView(createDayDetailView(week, day))
    }
    
    private fun createDayDetailView(week: String, day: Int): ScrollView {
        val scrollView = ScrollView(this).apply {
            id = 888
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.background))
        }
        
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        val headerBar = createHeaderBar(week)
        layout.addView(headerBar)
        
        val contentView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
        }
        
        viewModel.currentDayInfo.observe(this) { dayInfo ->
            contentView.removeAllViews()
            
            contentView.addView(TextView(this).apply {
                text = week
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(8)
                layoutParams = params
            })
            
            contentView.addView(TextView(this).apply {
                text = dayInfo.title.ifEmpty { "День $day" }
                textSize = 24f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(24)
                layoutParams = params
            })
            
            if (dayInfo.theory.isNotEmpty()) {
                contentView.addView(createTheoryCard(dayInfo.theory))
            }
            
            if (dayInfo.practice.isNotEmpty()) {
                contentView.addView(createPracticeCard(dayInfo.practice))
            }
            
            if (dayInfo.tasks.isNotEmpty()) {
                contentView.addView(createTasksCard(dayInfo.tasks))
            }
            
            val isCompleted = viewModel.isDayCompleted(week, day)
            contentView.addView(createActionButton(week, day, isCompleted))
        }
        
        layout.addView(contentView)
        scrollView.addView(layout)
        
        return scrollView
    }
    
    private fun createHeaderBar(week: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(20), dpToPx(36), dpToPx(20), dpToPx(16))
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
        }.apply {
            addView(TextView(this@MainActivity).apply {
                text = "←"
                textSize = 26f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 0, dpToPx(20), 0)
                setOnClickListener { setupMainScreen() }
            })
            
            addView(TextView(this@MainActivity).apply {
                text = getString(R.string.back)
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }
    
    private fun createTheoryCard(theory: String): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setContentPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(16)
            layoutParams = params
        }.apply {
            val layout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            layout.addView(TextView(this@MainActivity).apply {
                text = getString(R.string.theory)
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(12)
                layoutParams = params
            })
            
            layout.addView(TextView(this@MainActivity).apply {
                text = theory.replace(", ", "\n• ").let { "• $it" }
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
                setLineSpacing(4f, 1.3f)
            })
            
            addView(layout)
        }
    }
    
    private fun createPracticeCard(practice: String): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setContentPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(16)
            layoutParams = params
        }.apply {
            val layout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            layout.addView(TextView(this@MainActivity).apply {
                text = getString(R.string.practice)
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(12)
                layoutParams = params
            })
            
            layout.addView(TextView(this@MainActivity).apply {
                text = practice
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
                setLineSpacing(4f, 1.3f)
            })
            
            addView(layout)
        }
    }
    
    private fun createTasksCard(tasks: String): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.card))
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(1).toFloat()
            setContentPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.bottomMargin = dpToPx(24)
            layoutParams = params
        }.apply {
            val layout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            
            layout.addView(TextView(this@MainActivity).apply {
                text = getString(R.string.tasks)
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(12)
                layoutParams = params
            })
            
            layout.addView(TextView(this@MainActivity).apply {
                text = tasks.replace(", ", "\n• ").let { "• $it" }
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
                setLineSpacing(4f, 1.3f)
            })
            
            addView(layout)
        }
    }
    
    private fun createActionButton(week: String, day: Int, isCompleted: Boolean): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(
                if (isCompleted) ContextCompat.getColor(this@MainActivity, R.color.completed)
                else ContextCompat.getColor(this@MainActivity, R.color.primary_light)
            )
            radius = dpToPx(16).toFloat()
            cardElevation = 0f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(8)
            layoutParams = params
        }.apply {
            addView(Button(this@MainActivity).apply {
                text = if (isCompleted) getString(R.string.undo) else getString(R.string.mark_complete)
                textSize = 16f
                setTextColor(
                    if (isCompleted) ContextCompat.getColor(this@MainActivity, R.color.success)
                    else ContextCompat.getColor(this@MainActivity, R.color.primary)
                )
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(18))
                layoutParams = params
                
                setOnClickListener {
                    viewModel.toggleDayCompletion(week, day)
                    setContentView(createDayDetailView(week, day))
                }
            })
        }
    }
    
    private fun showSettingsDialog() {
        val settings = viewModel.notificationSettings.value ?: NotificationSettings()
        
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(40), dpToPx(30), dpToPx(40), dpToPx(20))
        }
        
        dialogView.addView(TextView(this).apply {
            text = getString(R.string.settings)
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        })
        
        val notificationsCard = createSettingsCard()
        val notificationsSwitch = CheckBox(this).apply {
            text = getString(R.string.enable_notifications)
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
            isChecked = settings.enabled
        }
        notificationsCard.findViewById<LinearLayout>(android.R.id.content)?.addView(notificationsSwitch)
        dialogView.addView(notificationsCard)
        
        val timeCard = createTimeCard(settings)
        dialogView.addView(timeCard)
        
        val testCard = createTestCard()
        dialogView.addView(testCard)
        
        dialogView.addView(Button(this).apply {
            text = getString(R.string.reset_progress)
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.danger))
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.danger_bg))
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(24)
            layoutParams = params
            setOnClickListener {
                showResetConfirmDialog()
            }
        })
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newSettings = NotificationSettings(
                    enabled = notificationsSwitch.isChecked,
                    hour = settings.hour,
                    minute = settings.minute
                )
                viewModel.saveNotificationSettings(newSettings)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
        
        dialog.show()
    }
    
    private fun createSettingsCard(): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(0xFFF8FAFC.toInt())
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(1).toFloat()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(24)
            layoutParams = params
        }.apply {
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(18))
            })
        }
    }
    
    private fun createTimeCard(settings: NotificationSettings): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(0xFFF8FAFC.toInt())
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(1).toFloat()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(16)
            layoutParams = params
        }.apply {
            val layout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(18))
            }
            
            layout.addView(TextView(this@MainActivity).apply {
                text = getString(R.string.notification_time)
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(12)
                layoutParams = params
            })
            
            layout.addView(Button(this@MainActivity).apply {
                text = String.format("%02d:%02d", settings.hour, settings.minute)
                textSize = 20f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                
                setOnClickListener {
                    TimePickerDialog(
                        this@MainActivity,
                        { _, hourOfDay, minute ->
                            text = String.format("%02d:%02d", hourOfDay, minute)
                        },
                        settings.hour,
                        settings.minute,
                        true
                    ).show()
                }
            })
            
            addView(layout)
        }
    }
    
    private fun createTestCard(): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(0xFFF8FAFC.toInt())
            radius = dpToPx(16).toFloat()
            cardElevation = dpToPx(1).toFloat()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dpToPx(16)
            layoutParams = params
        }.apply {
            val layout = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(20), dpToPx(18), dpToPx(20), dpToPx(18))
            }
            
            layout.addView(TextView(this@MainActivity).apply {
                text = getString(R.string.test_notification)
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dark))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = dpToPx(12)
                layoutParams = params
            })
            
            layout.addView(Button(this@MainActivity).apply {
                text = getString(R.string.test_notification)
                textSize = 15f
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.white))
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
                setOnClickListener {
                    sendTestNotification()
                }
            })
            
            addView(layout)
        }
    }
    
    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_progress)
            .setMessage(R.string.reset_confirm)
            .setPositiveButton(R.string.reset) { _, _ ->
                viewModel.resetProgress()
                setupMainScreen()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    viewModel.scheduleNotifications()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            viewModel.scheduleNotifications()
        }
    }
    
    private fun sendTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val messages = listOf(
            "Тестовое уведомление работает!",
            "Ты молодец! Всё настраивается правильно.",
            "Время учить Python!",
            "Продолжай обучение - ты на правильном пути!"
        )
        
        val notification = android.app.Notification.Builder(this, getString(R.string.channel_id))
            .setContentTitle(getString(R.string.python_learning))
            .setContentText(messages.random())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(0, notification)
        Toast.makeText(this, R.string.test_notification_sent, Toast.LENGTH_SHORT).show()
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
