package com.pylearn.app.ui.editor

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorViewModel(
    private val context: Context
) : ViewModel() {
    
    private val python by lazy { Python.getInstance() }
    private val pyModule by lazy { python.getModule("main") }
    
    private val _output = MutableLiveData("Вывод программы:\n")
    val output: LiveData<String> = _output
    
    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning
    
    private val _toastMessage = MutableLiveData<String?>()
    val toastMessage: LiveData<String?> = _toastMessage
    
    private val _code = MutableLiveData(getDefaultCode())
    val code: LiveData<String> = _code
    
    fun getDefaultCode(): String = """# Добро пожаловать в Python Editor!
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
    
    fun setCode(newCode: String) {
        _code.value = newCode
    }
    
    fun runCode(codeToRun: String = _code.value ?: "") {
        if (codeToRun.isBlank()) {
            _toastMessage.value = "Введите код"
            return
        }
        
        _isRunning.value = true
        _output.value = "${_output.value}\n--- Запуск ---\n"
        
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    pyModule.callAttr("run_code", codeToRun)
                }
                
                val outputText = result?.toString() ?: ""
                val finalOutput = if (outputText.isNotEmpty() && outputText != "null") {
                    outputText
                } else {
                    "Программа выполнена"
                }
                
                _output.value = "${_output.value}$finalOutput\n"
            } catch (e: Exception) {
                _output.value = "${_output.value}Ошибка: ${e.message}\n"
            } finally {
                _isRunning.value = false
            }
        }
    }
    
    fun clearOutput() {
        _output.value = "Вывод программы:\n"
    }
    
    fun saveScript(filename: String) {
        if (filename.isBlank()) {
            _toastMessage.value = "Введите имя файла"
            return
        }
        
        viewModelScope.launch {
            try {
                val file = File(context.filesDir, filename)
                file.writeText(_code.value ?: "")
                _toastMessage.value = "Сохранено: $filename"
            } catch (e: Exception) {
                _toastMessage.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }
    
    fun clearToastMessage() {
        _toastMessage.value = null
    }
}
