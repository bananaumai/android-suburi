package dev.bananaumai.android.suburi.mqtt

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val mqttServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mqttService = (service as MqttService.MqttServiceBinder).getService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mqttService = null
        }
    }

    private var isRunning = false

    private var mqttService: MqttService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Intent(this, MqttService::class.java).also { intent ->
            startService(intent)
            bindService(intent, mqttServiceConnection, Context.BIND_AUTO_CREATE)
        }

        val text = findViewById<TextView>(R.id.textView)
        val button = findViewById<Button>(R.id.button)
        button.setOnClickListener {
            lifecycleScope.launch {
                button.isEnabled = false

                isRunning = !isRunning

                if (isRunning) {
                    mqttService?.start()
                    text.text = "RUNNING"
                    button.text = "STOP"
                } else {
                    mqttService?.stop()
                    text.text = "STOPPED"
                    button.text = "START"
                }

                button.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Intent(this, MqttService::class.java).also { intent ->
            unbindService(mqttServiceConnection)
            stopService(intent)
        }
    }
}
