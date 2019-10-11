package dev.bananaumai.android.suburi.sensor_and_coroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    private val sj = SupervisorJob()
    private val scope = CoroutineScope(sj + Dispatchers.Default)

    private var working = false
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            button.isEnabled = false

            working = !working

            if (working) {
                button.text = "stop"
                start()
            } else {
                stop()
                button.text = "run"
            }

            button.isEnabled = true
        }
    }

    private fun start() {
        job = scope.launch {
            accelerometerFlow(this@MainActivity)
                .collect {
                    Log.i("Activity", "$it")
                }
        }
    }

    private fun stop() {
        job?.cancel()
    }
}
