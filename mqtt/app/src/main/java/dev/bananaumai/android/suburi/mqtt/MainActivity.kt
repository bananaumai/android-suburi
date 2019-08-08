package dev.bananaumai.android.suburi.mqtt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class MainActivity : AppCompatActivity() {
    lateinit var mqttClient: MqttAndroidClient

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
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.unregisterResources();
        mqttClient.close()
    }
}
