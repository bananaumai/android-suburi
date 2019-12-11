package dev.bananaumai.android.suburi.mqtt

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence

class MqttService : Service() {
    inner class MqttServiceBinder : Binder() {
        fun getService(): MqttService {
            return this@MqttService
        }
    }

    private val binder = MqttServiceBinder()

    private val job = SupervisorJob()

    private val scope = CoroutineScope(job)

    private val mutex = Mutex()

    private lateinit var client: IMqttAsyncClient

    override fun onCreate() {
        super.onCreate()
        client = createClient()
    }

    @ExperimentalCoroutinesApi
    override fun onDestroy() {
        super.onDestroy()

        scope.launch {
            disconnect()
            job.cancel()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("MqttService", "onStartCommand")
        scope.launch {
            var i = 0
            while(true) {
                Log.v("MqttService", "Count: $i")
                publish(i.toString().toByteArray())
                i++
                delay(1000)
            }
        }
        return START_STICKY
    }

    @ExperimentalCoroutinesApi
    suspend fun connect() {
        Log.d("MqttService", "connect")

        val connectOptions = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            connectionTimeout = 5
        }

        mutex.withLock {
            if (client.isConnected) {
                Log.d("MqttService", "connect")
                return
            }

            withTimeout(10_000L) {
                channelFlow {
                    val listener = object : IMqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            Log.i("MqttService", "connected")
                            launch { send(null) }
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            Log.e("MqttService", "connection error", exception)
                            launch { send(null) }
                        }
                    }

                    client.connect(connectOptions, this, listener)

                    awaitClose()
                }.first()
            }
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun disconnect() {
        Log.d("MqttService", "disconnect")

        mutex.withLock {
            val e = channelFlow {
                val listener = object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.i("MqttService", "disconnected")
                        launch { send(null) }
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.i("MqttService", "disconnection error")
                        launch { send(exception) }
                    }
                }

                client.disconnect(1000, this, listener)

                awaitClose()
            }.first()

            if (e != null) {
                val expectedErrorReasonCode = listOf(
                    MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED.toInt(),
                    MqttException.REASON_CODE_CLIENT_CLOSED.toInt(),
                    MqttException.REASON_CODE_CLIENT_DISCONNECTING.toInt()
                )

                if (e is MqttException && expectedErrorReasonCode.contains(e.reasonCode)) {
                    Log.i("MqttService", "expected errors on disconnection")
                } else {
                    Log.e("MqttService", "failed to disconnect")
                    return
                }
            }

            if (client is MqttAndroidClient) {
                (client as MqttAndroidClient).unregisterResources()
            } else {
                client.close()
                client = createClient()
                Log.i("MqttService", "disconnection completed")
            }

            Unit
        }
    }

    private suspend fun publish(payload: ByteArray) {
        mutex.withLock {
            Log.i("MqttService", "publish")
            if (!client.isConnected) {
                Log.i("MqttService", "not published because client is not connected")
                return
            }
            val msg = MqttMessage()
            msg.payload = payload
            client.publish("/test", msg)
        }
    }

    private fun createClient(): IMqttAsyncClient {
        val url = "tcp://10.0.2.2:1883"
        val clientId = MqttClient.generateClientId()

        val client = if (BuildConfig.MQTT_CLIENT == "ANDROID") {
            MqttAndroidClient(this, url, clientId)
        } else {
            val dir = applicationContext.getExternalFilesDir("mqtt")
            val persistence = MqttDefaultFilePersistence(dir!!.absolutePath)
            MqttAsyncClient(url, clientId, persistence)
        }

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.v("MqttService", "connectComplete - ${client.clientId}")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.v("MqttService", "connectionLost - ${client.clientId}")
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.v("MqttService", "deliveryComplete - ${client.clientId}")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.v("MqttService", "messageArrived - ${client.clientId}")
            }
        })

        return client
    }
}
