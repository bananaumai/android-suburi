package dev.bananaumai.android.suburi.mqtt

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttClient


class MainActivity : AppCompatActivity() {
    lateinit var mqttClient: MqttAndroidClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mqttClient = MqttAndroidClient(
            this,
            "tcp://10.0.2.2:1883", MqttClient.generateClientId()
        )

        mqttClient.connect(this, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                Log.d("MQTT", "connected")
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.d("MQTT", "failed to connect")
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttClient.close()
    }
}
