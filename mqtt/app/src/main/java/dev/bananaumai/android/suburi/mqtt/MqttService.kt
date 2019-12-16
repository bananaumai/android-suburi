package dev.bananaumai.android.suburi.mqtt

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
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

    @Volatile
    private lateinit var client: IMqttAsyncClient

    @Volatile
    private var isRunning = false

    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        client = createClient()

        scope.launch {
            connect()
        }
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

    @ExperimentalCoroutinesApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("MqttService", "onStartCommand")
        scope.launch {
            var i = 0
            while(true) {
                if (isRunning) {
                    Log.v("MqttService", "Count: $i")
                    publish(i.toString().toByteArray())
                    i++
                }
                delay(1000)
            }
        }
        return START_STICKY
    }

    @ExperimentalCoroutinesApi
    suspend fun start() {
        isRunning = true
        connect()
    }

    @ExperimentalCoroutinesApi
    suspend fun stop() {
        isRunning = false
        disconnect()
    }

    @ExperimentalCoroutinesApi
    private suspend fun connect() {
        Log.d("MqttService", "connect")

        if (client.isConnected) {
            Log.d("MqttService", "already connected")
            return
        }

        val connectOptions = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            connectionTimeout = 3
        }

        withContext(Dispatchers.IO) {
            val listener = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i("MqttService", "connected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.e("MqttService", "connection error", exception)
                }
            }

            try {
                val token = client.connect(connectOptions, this, listener)
                token.waitForCompletion(5_000L)
            } catch(e: MqttException) {
                if (e.reasonCode == MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt()) {
                    Log.d("MqttService", "connection process timed out")
                } else {
                    Log.e("MqttService", "failed to connect")
                }
            }

            Log.d("MqttService", "connection completed")
        }
    }


    @ExperimentalCoroutinesApi
    private suspend fun disconnect() {
        Log.d("MqttService", "disconnect")

        if (!client.isConnected) {
            Log.d("MqttService", "already disconnected")
            return
        }

        val expectedErrorReasonCode = listOf(
            MqttException.REASON_CODE_CLIENT_ALREADY_DISCONNECTED.toInt(),
            MqttException.REASON_CODE_CLIENT_CLOSED.toInt(),
            MqttException.REASON_CODE_CLIENT_DISCONNECTING.toInt()
        )

        withContext(Dispatchers.IO) {
            val listener = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.i("MqttService", "disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.i("MqttService", "disconnection error")
                    if (exception is MqttException && expectedErrorReasonCode.contains(exception.reasonCode)) {
                        Log.i("MqttService", "expected errors on disconnection")
                    } else {
                        Log.e("MqttService", "failed to disconnect", exception)
                    }
                }
            }


            try {
                val token = client.disconnect(1_000, this, listener)
                token.waitForCompletion(10_000L)
            } catch(e: MqttException) {
                if (e.reasonCode == MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt()) {
                    Log.d("MqttService", "disconnection process timed out", e)
                } else {
                    Log.e("MqttService", "failed to disconnect", e)
                    return@withContext
                }
            }

            if (client is MqttAndroidClient) {
                (client as MqttAndroidClient).unregisterResources()
            } else {
                client.close()
                client = createClient()
            }

            Log.d("MqttService", "disconnection completed")
        }
    }

    private fun publish(payload: ByteArray) {
        Log.i("MqttService", "publish")
        if (!client.isConnected) {
            Log.i("MqttService", "not published because client is not connected")
            return
        }
        val msg = MqttMessage()
        msg.payload = payload

        try {
            client.publish("/test", msg)
        } catch (e: MqttException) {
            if (e.reasonCode == MqttException.REASON_CODE_CLIENT_NOT_CONNECTED.toInt()) {
                Log.e("MqttService", "failed to publish", e)
            }
        }
    }

    private fun createClient(): IMqttAsyncClient {
        val url = BuildConfig.MQTT_SERVER_URL
        val clientId = MqttClient.generateClientId()

        val client = when (BuildConfig.MQTT_CLIENT) {
            "ANDROID" -> {
                MqttAndroidClient(this, url, clientId)
            }
            "NATIVE" -> {
                val dir = applicationContext.getExternalFilesDir("mqtt")
                val persistence = MqttDefaultFilePersistence(dir!!.absolutePath)
                MqttAsyncClient(url, clientId, persistence)
            }
            else -> {
                throw RuntimeException("invalid client")
            }
        }

        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.v("MqttService", "connectComplete(reconnect = $reconnect, serverURI = $serverURI)")
            }

            override fun connectionLost(cause: Throwable?) {
                Log.v("MqttService", "connectionLost", cause)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Log.v("MqttService", "deliveryComplete(token = $token)")
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                Log.v("MqttService", "messageArrived(topic = $topic, message = $message)")
            }
        })

        return client
    }
}
