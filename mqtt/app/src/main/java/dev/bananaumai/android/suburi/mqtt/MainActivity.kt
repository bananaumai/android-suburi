package dev.bananaumai.android.suburi.mqtt

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.w3c.dom.Text


class MainActivity : AppCompatActivity() {

    private val activityJob = Job()
    private val activityScope = CoroutineScope(Dispatchers.Main + activityJob)
    private lateinit var mqttClient: MqttAndroidClient
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

        mqttClient = MqttAndroidClient(
            this,
            "tcp://10.0.2.2:1883", MqttClient.generateClientId()
        )

        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d("MQTT", "connectComplete")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.d("MQTT", "connectionLost")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.d("MQTT", "deliveryComplete")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.d("MQTT", "messageArrived")
            }
        })

        val mqttConnectOptions = MqttConnectOptions().apply {
            isAutomaticReconnect = true
        }

        mqttClient.connect(mqttConnectOptions, "banana", object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                val ctx = asyncActionToken!!.userContext as String
                Log.d("MQTT", "connected ${ctx}")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                val ctx = asyncActionToken!!.userContext as String
                Log.d("MQTT", "failed to connect ${ctx}")
            }
        })

        LocalBroadcastManager.getInstance(this).registerReceiver(randomNumberReceiver, IntentFilter("randomNumber"))

        startUpdateButtonUI()
        startUpdateRandomNumberUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.unregisterResources()
        mqttClient.close()
        activityJob.cancel()
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
