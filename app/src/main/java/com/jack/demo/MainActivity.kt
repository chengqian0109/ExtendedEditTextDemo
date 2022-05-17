package com.jack.demo

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jack.edit.ExtendedEditText

class MainActivity : AppCompatActivity() {
    private val defaultSeparatorEdit: ExtendedEditText by lazy { findViewById(R.id.default_edit_text) }

    private val customSeparatorEdit: ExtendedEditText by lazy { findViewById(R.id.custom_edit_text) }

    private val showSeparatorEdit: ExtendedEditText by lazy { findViewById(R.id.show_separator_edit_text) }

    private val customMarkerEdit1: ExtendedEditText by lazy { findViewById(R.id.custom_marker_edit_text1) }

    private val textView1: TextView by lazy { findViewById(R.id.text1) }

    private val textView2: TextView by lazy { findViewById(R.id.text2) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        customSeparatorEdit.setPattern(intArrayOf(4, 4, 4, 4))
        customSeparatorEdit.setSeparator(" ")

        defaultSeparatorEdit.setOnTextChangeListener(object : ExtendedEditText.OnTextChangeListener {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                textView1.text = defaultSeparatorEdit.getNonSeparatorText()
            }
        })

        customSeparatorEdit.setOnTextChangeListener(object : ExtendedEditText.OnTextChangeListener {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                textView2.text = customSeparatorEdit.getNonSeparatorText()
            }
        })

        findViewById<Button>(R.id.show_pattern_btn).setOnClickListener {
            showSeparatorEdit.setTextToSeparate("13800000000")
        }

        customMarkerEdit1.setCustomizeMarkerEnable(true)
        customMarkerEdit1.setOnMarkerClickListener(object : ExtendedEditText.OnMarkerClickListener {
            override fun onMarkerClick(x: Float, y: Float) {
                showMarkerPop(x.toInt(), y.toInt())
            }
        })
    }

    private fun showMarkerPop(
        x: Int,
        y: Int
    ) {
        val view = View.inflate(this, R.layout.layout_pop_up, null)
        val popup = PopupWindow(view)
        popup.setBackgroundDrawable(ColorDrawable(0x00000000))
        popup.isFocusable = true
        popup.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
        popup.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        popup.isOutsideTouchable = true
        popup.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popup.animationStyle = R.style.Anim
        popup.showAtLocation(customMarkerEdit1, Gravity.BOTTOM, x, y)
    }
}