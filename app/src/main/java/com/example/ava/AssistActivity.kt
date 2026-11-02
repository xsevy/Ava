package com.example.ava

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.example.ava.tasker.WakeSatelliteRunner

class AssistActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!WakeSatelliteRunner.wake()) {
            startActivity(Intent(this, MainActivity::class.java))
        }
        finish()
    }
}
