package ru.railbrake.calculator.shell

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                setText(R.string.modular_rebuild_baseline)
                textSize = 20f
                setPadding(48, 48, 48, 48)
            }
        )
    }
}
