package com.pylearn.app

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
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.pylearn.app.ui.editor.EditorViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class EditorActivity : AppCompatActivity() {
    private val viewModel: EditorViewModel by viewModel()
    
    private lateinit var codeEditor: EditText
    private lateinit var outputView: TextView
    private lateinit var runButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setContentView(createEditorView())
        
        observeViewModel()
    }
    
    private fun observeViewModel() {
        viewModel.output.observe(this) { output ->
            outputView.text = output
        }
        
        viewModel.isRunning.observe(this) { isRunning ->
            runButton.isEnabled = !isRunning
            runButton.text = if (isRunning) "⏳ Запуск..." else getString(R.string.run)
        }
        
        viewModel.toastMessage.observe(this) { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToastMessage()
            }
        }
        
        viewModel.code.observe(this) { code ->
            if (codeEditor.text.toString() != code) {
                codeEditor.setText(code)
            }
        }
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
                viewModel.clearOutput()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun createEditorView(): LinearLayout {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ContextCompat.getColor(this@EditorActivity, R.color.background))
        }
        
        val toolbar = createToolbar()
        mainLayout.addView(toolbar)
        
        val editorCard = createEditorCard()
        mainLayout.addView(editorCard)
        
        val buttonLayout = createButtonLayout()
        mainLayout.addView(buttonLayout)
        
        val outputCard = createOutputCard()
        mainLayout.addView(outputCard)
        
        return mainLayout
    }
    
    private fun createToolbar(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            setBackgroundColor(ContextCompat.getColor(this@EditorActivity, R.color.primary))
        }.apply {
            addView(TextView(this@EditorActivity).apply {
                text = "  ${getString(R.string.editor_title)}"
                textSize = 18f
                setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.white))
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
    }
    
    private fun createEditorCard(): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@EditorActivity, R.color.editor_background))
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            params.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(8))
            layoutParams = params
        }.apply {
            codeEditor = EditText(this@EditorActivity).apply {
                setText(viewModel.code.value ?: viewModel.getDefaultCode())
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.editor_text))
                setBackgroundColor(ContextCompat.getColor(this@EditorActivity, R.color.editor_background))
                gravity = Gravity.TOP or Gravity.START
                setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(24))
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams = params
                
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        viewModel.setCode(text.toString())
                    }
                }
            }
            
            addView(codeEditor)
        }
    }
    
    private fun createButtonLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
            layoutParams = params
        }.apply {
            runButton = Button(this@EditorActivity).apply {
                text = getString(R.string.run)
                textSize = 14f
                setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.white))
                setBackgroundResource(R.drawable.btn_primary)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = params
                
                setOnClickListener {
                    viewModel.setCode(codeEditor.text.toString())
                    viewModel.runCode()
                }
            }
            addView(runButton)
            
            addView(Button(this@EditorActivity).apply {
                text = getString(R.string.clear_code)
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.white))
                setBackgroundResource(R.drawable.btn_secondary)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, dpToPx(8), 0)
                layoutParams = params
                
                setOnClickListener {
                    codeEditor.setText("")
                    viewModel.setCode("")
                }
            })
            
            addView(Button(this@EditorActivity).apply {
                text = getString(R.string.clear_output)
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.text_dark))
                setBackgroundResource(R.drawable.btn_light)
                setPadding(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                
                setOnClickListener {
                    viewModel.clearOutput()
                }
            })
        }
    }
    
    private fun createOutputCard(): CardView {
        return CardView(this).apply {
            setCardBackgroundColor(ContextCompat.getColor(this@EditorActivity, R.color.output_background))
            radius = dpToPx(12).toFloat()
            cardElevation = dpToPx(2).toFloat()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(16))
            layoutParams = params
        }.apply {
            outputView = TextView(this@EditorActivity).apply {
                text = getString(R.string.output_label)
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@EditorActivity, R.color.output_text))
                setBackgroundColor(ContextCompat.getColor(this@EditorActivity, R.color.output_background))
                gravity = Gravity.TOP or Gravity.START
                setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16))
                isVerticalScrollBarEnabled = true
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                layoutParams = params
            }
            
            addView(outputView)
        }
    }
    
    private fun showSaveDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.filename_hint)
            setText(getString(R.string.default_filename))
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.save_script)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                val filename = input.text.toString()
                if (filename.isNotEmpty()) {
                    viewModel.setCode(codeEditor.text.toString())
                    viewModel.saveScript(filename)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
