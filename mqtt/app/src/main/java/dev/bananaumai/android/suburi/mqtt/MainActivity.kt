package dev.bananaumai.android.suburi.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val activityJob = Job()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)
    private var isRunning = false
    private var randomNum = 0

    private val randomNumberReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                randomNum = intent.getIntExtra("num", 0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LocalBroadcastManager.getInstance(this).registerReceiver(randomNumberReceiver, IntentFilter("randomNumber"))
        Intent(this, MqttService::class.java).also { intent ->
            startService(intent)
        }

        startUpdateButtonUI()
        startUpdateRandomNumberUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityJob.cancel()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(randomNumberReceiver)
    }

    fun toggle(view: View) {
        isRunning = if (isRunning) {
            Intent(this, EmitterService::class.java).also { intent ->
                stopService(intent)
            }
            false
        } else {
            Intent(this, EmitterService::class.java).also { intent ->
                startService(intent)
            }
            true
        }
    }

    private fun startUpdateButtonUI() {
        val button = findViewById<Button>(R.id.button)
        var prevState = isRunning

        activityScope.launch {
            while (true) {
                delay(100)

                val newText = if (isRunning) {
                    "Stop"
                } else {
                    "Run"
                }
                if (prevState != isRunning) {
                    button.text = newText
                    prevState = isRunning
                }
            }
        }
    }

    private fun startUpdateRandomNumberUI() {
        val txt = findViewById<TextView>(R.id.randomNumber)
        var prevState = randomNum

        activityScope.launch {
            while (true) {
                delay(50)

                if (prevState != randomNum) {
                    txt.text = randomNum.toString()
                    prevState = randomNum
                }
            }
        }
    }
}
