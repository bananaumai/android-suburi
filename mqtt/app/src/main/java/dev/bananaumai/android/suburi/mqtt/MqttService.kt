package dev.bananaumai.android.suburi.mqtt

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttService : Service() {
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val connectRetryCh = Channel<Boolean>()
    private val connectRetryJob = serviceScope.launch {
        Log.v("MqttService", "launch connect retry job")
        while (connectRetryCh.receive()) {
            Log.d("MqttService", "retry connect!")
            connect()
        }
    }
    private val client: MqttAndroidClient by lazy {
        val client = MqttAndroidClient(
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

        client
    }
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

    override fun onCreate() {
        super.onCreate()
        Log.v("MqttServide", "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("MqttService", "onStartCommand")
        connect()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        client.unregisterResources()
        client.close()
        client.disconnect()
        serviceJob.cancel()
        connectRetryCh.cancel()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(randomNumberReceiver)
    }

    private fun connect() {
        if (!client.isConnected) {
            Log.d("MqttService", "Try to connect")

            val connectOptions = MqttConnectOptions().apply {
                isAutomaticReconnect = true
            }

            client.connect(connectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i("MqttService", "connected")
                    LocalBroadcastManager
                        .getInstance(this@MqttService)
                        .registerReceiver(randomNumberReceiver, IntentFilter("randomNumber"))
                    connectRetryCh.cancel()
                    connectRetryJob.cancel()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttService", "failed to connect")
                    serviceScope.launch {
                        delay(1000)
                        connectRetryCh.send(true)
                    }
                }
            })
        }
    }
}
