package com.pylearn.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.chaquo.python.Python

class EditorActivity : AppCompatActivity() {
    private lateinit var codeEditor: EditText
    private lateinit var outputView: TextView
    private lateinit var runButton: Button
    private lateinit var python: Python
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createEditorView())
        
        python = Python.getInstance()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                showSaveDialog()
                true
            }
            R.id.action_clear -> {
                codeEditor.setText("")
                outputView.text = "Вывод программы:\n"
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun createEditorView(): LinearLayout {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(AppColors.BG)
        }
        
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(AppColors.PRIMARY)
        }
        
        val title = TextView(this).apply {
            text = "  Редактор Python"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        toolbar.addView(title)
        mainLayout.addView(toolbar)
        
        val editorCard = CardView(this).apply {
            setCardBackgroundColor(0xFF1E1E1E.toInt())
            radius = 12f
            cardElevation = 2f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            params.setMargins(16, 16, 16, 8)
            layoutParams = params
        }
        
        codeEditor = EditText(this).apply {
            setText(defaultCode)
            textSize = 14f
            setTextColor(0xFFD4D4D4.toInt())
            setBackgroundColor(0xFF1E1E1E.toInt())
            gravity = Gravity.TOP or Gravity.START
            setPadding(24, 24, 24, 24)
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
        }
        
        editorCard.addView(codeEditor)
        mainLayout.addView(editorCard)
        
        val buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(16, 8, 16, 8)
            layoutParams = params
        }
        
        runButton = Button(this).apply {
            text = "▶ Запустить"
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(AppColors.PRIMARY)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            layoutParams = params
        }
        
        val clearButton = Button(this).apply {
            text = "Очистить вывод"
            textSize = 14f
            setTextColor(AppColors.TEXT_DARK)
            setBackgroundColor(0xFFE5E7EB.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        runButton.setOnClickListener { runCode() }
        clearButton.setOnClickListener { outputView.text = "Вывод программы:\n" }
        
        buttonLayout.addView(runButton)
        buttonLayout.addView(clearButton)
        mainLayout.addView(buttonLayout)
        
        val outputCard = CardView(this).apply {
            setCardBackgroundColor(0xFF0D0D0D.toInt())
            radius = 12f
            cardElevation = 2f
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            params.setMargins(16, 8, 16, 16)
            layoutParams = params
        }
        
        outputView = TextView(this).apply {
            text = "Вывод программы:\n"
            textSize = 13f
            setTextColor(0xFF00FF00.toInt())
            setBackgroundColor(0xFF0D0D0D.toInt())
            gravity = Gravity.TOP or Gravity.START
            setPadding(24, 16, 24, 16)
            isVerticalScrollBarEnabled = true
            
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
        }
        
        outputCard.addView(outputView)
        mainLayout.addView(outputCard)
        
        return mainLayout
    }
    
    private fun runCode() {
        val code = codeEditor.text.toString()
        if (code.isBlank()) {
            Toast.makeText(this, "Введите код", Toast.LENGTH_SHORT).show()
            return
        }
        
        runButton.isEnabled = false
        outputView.append("\n--- Запуск ---\n")
        
        Thread {
            try {
                val pyObj = Python.getInstance().getModule("main")
                val result = pyObj.callAttr("run_code", code)
                
                runOnUiThread {
                    val output = result.toString()
                    if (output.isNotEmpty() && output != "None") {
                        outputView.append(output)
                    } else {
                        outputView.append("Программа выполнена\n")
                    }
                    outputView.append("\n")
                    runButton.isEnabled = true
                }
            } catch (e: Exception) {
                runOnUiThread {
                    outputView.append("Ошибка: ${e.message}\n")
                    runButton.isEnabled = true
                }
            }
        }.start()
    }
    
    private fun showSaveDialog() {
        val input = EditText(this)
        input.hint = "Имя файла"
        input.setText("script.py")
        
        AlertDialog.Builder(this)
            .setTitle("Сохранить скрипт")
            .setView(input)
            .setPositiveButton("Сохранить") { _, _ ->
                val filename = input.text.toString()
                if (filename.isNotEmpty()) {
                    saveScript(filename)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun saveScript(filename: String) {
        try {
            val file = java.io.File(filesDir, filename)
            file.writeText(codeEditor.text.toString())
            Toast.makeText(this, "Сохранено: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка сохранения: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val defaultCode = """# Добро пожаловать в Python Editor!
# Напишите свой код здесь и нажмите "Запустить"

print("Привет, мир!")

# Пример: простой калькулятор
a = 10
b = 5
print(f"{a} + {b} = {a + b}")
print(f"{a} - {b} = {a - b}")
print(f"{a} * {b} = {a * b}")
print(f"{a} / {b} = {a / b}")
"""
}
