package dev.bananaumai.android.suburi.mqtt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : AppCompatActivity() {
    private lateinit var state: StateViewModel
    private lateinit var randomNumber: NumberViewModel

    private val randomNumberReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                randomNumber.currentNumber.value = intent.getIntExtra("num", 0)
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

        val button = findViewById<Button>(R.id.button)
        state = ViewModelProviders.of(this).get(StateViewModel::class.java)
        state.currentState.observe(this, Observer<StateViewModel.State> { state ->
            button.text = when (state) {
                StateViewModel.State.RUNNING -> {
                    Intent(this, EmitterService::class.java).also { intent ->
                        startService(intent)
                    }
                    "RUNNING"
                }
                else -> {
                    Intent(this, EmitterService::class.java).also { intent ->
                        stopService(intent)
                    }
                    "STOPPED"
                }
            }
        })
        button.setOnClickListener { state.flip() }

        val randomNumberText = findViewById<TextView>(R.id.randomNumber)
        randomNumber = ViewModelProviders.of(this).get(NumberViewModel::class.java)
        randomNumber.currentNumber.observe(this, Observer<Int> { num ->
            if (num == null) {
                randomNumberText.text = ""
            } else {
                randomNumberText.text = num.toString()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(randomNumberReceiver)
    }
}
