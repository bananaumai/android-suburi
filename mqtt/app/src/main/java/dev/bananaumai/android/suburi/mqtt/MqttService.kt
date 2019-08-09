package dev.bananaumai.android.suburi.mqtt

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttService : Service() {
    private lateinit var client: MqttAndroidClient

    private val randomNumberReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.v("MqttService", "onReceive")
            if (intent != null) {
                val randomNum = intent.getIntExtra("num", 0)
                if (client.isConnected) {
                    Log.v("MqttService", "try to publish message with $randomNum")
                    MqttMessage().also { msg ->
                        msg.payload = randomNum.toString().toByteArray()
                        client.publish("/test", msg)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        client = MqttAndroidClient(
            this,
            "tcp://10.0.2.2:1883", MqttClient.generateClientId()
        )

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.v("MqttService", "connectComplete")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.v("MqttService", "connectionLost")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.v("MqttService", "deliveryComplete")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.v("MqttService", "messageArrived")
            }
        })

        LocalBroadcastManager.getInstance(this).registerReceiver(randomNumberReceiver, IntentFilter("randomNumber"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("MqttService", "onStartCommand")

        if (!client.isConnected) {
            Log.d("MqttService", "Try to connect")

            val connectOptions = MqttConnectOptions().apply {
                isAutomaticReconnect = true
            }

            client.connect(connectOptions, "banana", object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val ctx = asyncActionToken!!.userContext as String
                    Log.i("MqttService", "connected ${ctx}")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    val ctx = asyncActionToken!!.userContext as String
                    Log.e("MqttService", "failed to connect ${ctx}")
                }
            })
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        client.unregisterResources()
        client.close()
        client.disconnect()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(randomNumberReceiver)
    }
}
