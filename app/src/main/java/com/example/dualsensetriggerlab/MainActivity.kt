package com.example.dualsensetriggerlab

import android.app.Activity
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.graphics.Color
import android.view.Gravity

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var l2Value: TextView
    private lateinit var r2Value: TextView
    private lateinit var l2Bar: ProgressBar
    private lateinit var r2Bar: ProgressBar
    private var l2 = 0f
    private var r2 = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.setBackgroundColor(Color.rgb(13, 18, 30))
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 42, 28, 28)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        fun makeText(value: String, size: Float, color: Int = Color.WHITE) =
            TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 12)
            }
        root.addView(makeText("DUALSENSE TRIGGER LAB", 24f))
        status = makeText("Connect your PS5 controller using Bluetooth", 16f, Color.LTGRAY)
        root.addView(status)
        root.addView(makeText("L2 — AIM TRIGGER", 18f))
        l2Value = makeText("0%", 22f); root.addView(l2Value)
        l2Bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(l2Bar, LinearLayout.LayoutParams(-1, 24))
        root.addView(makeText("R2 — FIRE TRIGGER", 18f))
        r2Value = makeText("0%", 22f); root.addView(r2Value)
        r2Bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        root.addView(r2Bar, LinearLayout.LayoutParams(-1, 24))
        root.addView(makeText("This app reads trigger input only. Android's standard gamepad API does not provide a public command for DualSense adaptive-trigger resistance.", 14f, Color.LTGRAY))
        root.addView(makeText("Press L2 and R2 to test input.", 16f))
        setContentView(root)
        refresh()
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.isFromSource(InputDevice.SOURCE_JOYSTICK) || event.isFromSource(InputDevice.SOURCE_GAMEPAD)) {
            l2 = event.getAxisValue(MotionEvent.AXIS_LTRIGGER).coerceIn(0f, 1f)
            r2 = event.getAxisValue(MotionEvent.AXIS_RTRIGGER).coerceIn(0f, 1f)
            refresh(event.device?.name)
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_L2 || event.keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            val value = if (event.action == KeyEvent.ACTION_DOWN) 1f else 0f
            if (event.keyCode == KeyEvent.KEYCODE_BUTTON_L2) l2 = value else r2 = value
            refresh(event.device?.name)
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun refresh(deviceName: String? = null) {
        if (deviceName != null) status.text = "Controller: $deviceName"
        l2Value.text = "${(l2 * 100).toInt()}%"
        r2Value.text = "${(r2 * 100).toInt()}%"
        l2Bar.progress = (l2 * 100).toInt()
        r2Bar.progress = (r2 * 100).toInt()
    }
}
