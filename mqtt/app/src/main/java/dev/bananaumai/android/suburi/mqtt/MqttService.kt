package dev.bananaumai.android.suburi.mqtt

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.internal.ClientComms
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

    @Volatile
    private lateinit var client: IMqttAsyncClient

    @Volatile
    private var isRunning = false

    @ExperimentalCoroutinesApi
    override fun onCreate() {
        super.onCreate()
        client = createClient()

        scope.launch {
            launch { connect() }

            launch {
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        disconnect()

        if (client is MqttAsyncClient) {
            client.close()
        }

        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("MqttService", "onStartCommand")
        return START_STICKY
    }

    suspend fun start() {
        isRunning = true
        withContext(Dispatchers.IO) { connect() }
    }

    suspend fun stop() {
        isRunning = false
        withContext(Dispatchers.IO) { disconnect() }
    }

    private fun connect() {
        Log.d("MqttService", "connect")

        if (client.isConnected) {
            Log.d("MqttService", "already connected")
            return
        }

        val connectOptions = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            connectionTimeout = 3
        }

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
                Log.e("MqttService", "failed to connect", e)
            }
        }

        Log.d("MqttService", "connection completed")
    }


    private fun disconnect() {
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
                return
            }
        }

        if (client is MqttAndroidClient) {
            (client as MqttAndroidClient).unregisterResources()
        }

        Log.d("MqttService", "disconnection completed")
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
                val pingSender = AlarmPingSender(this)
                MqttAsyncClient(url, clientId, persistence, pingSender)
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

class AlarmPingSender(private val service: Service) : MqttPingSender {
    companion object {
        private const val TAG = "AlarmPingSender"
    }

    private lateinit var comms: ClientComms
    private lateinit var alarmReceiver: BroadcastReceiver

    @Volatile
    private var hasStarted = false

    private var pendingIntent: PendingIntent? = null

    override fun init(comms: ClientComms) {
        this.comms = comms
        alarmReceiver = AlarmReceiver()
    }

    override fun start() {
        if (hasStarted) {
            return
        }

        Log.d(TAG, "register alarm receiver to ${service.javaClass.simpleName}")

        val action = "${service.javaClass.simpleName}_${comms.client.clientId}"

        service.registerReceiver(alarmReceiver, IntentFilter(action))

        pendingIntent = PendingIntent.getBroadcast(
            service,
            0,
            Intent(action),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        schedule(comms.keepAlive)

        hasStarted = true
    }

    override fun stop() {
        if (!hasStarted) {
            return
        }

        Log.d(TAG, "unregister alarm receiver to ${service.javaClass.simpleName}")

        if (pendingIntent != null) {
            val alarmManager = service.getSystemService(Service.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }

        hasStarted = false

        try {
            service.unregisterReceiver(alarmReceiver)
        } catch (e: IllegalArgumentException) {
            //Ignore unregister errors.
        }
    }

    override fun schedule(delayInMilliseconds: Long) {
        val alarmManager = service.getSystemService(Service.ALARM_SERVICE) as AlarmManager

        val nextAlarmInMilliseconds = (System.currentTimeMillis() + delayInMilliseconds)

        Log.d(TAG, "schedules alarm in next $delayInMilliseconds milliseconds, at $nextAlarmInMilliseconds")

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextAlarmInMilliseconds,
            pendingIntent
        )
    }

    internal inner class AlarmReceiver : BroadcastReceiver() {
        private var wakelock: WakeLock? = null
        private val wakeLockTag = "${service.javaClass.simpleName}-${javaClass.simpleName}-${comms.client.clientId}"

        override fun onReceive(context: Context, intent: Intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.
            Log.d(TAG, "sending Ping at: ${System.currentTimeMillis()}")
            val pm = service.getSystemService(Service.POWER_SERVICE) as PowerManager

            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)

            wakelock?.acquire(60_000L)

            // Assign new callback to token to execute code after PingResq
            // arrives. Get another wakelock even receiver already has one,
            // release it until ping response returns.
            val listener = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.d(TAG, "succeeded, release lock($wakeLockTag): ${System.currentTimeMillis()}")
                    wakelock?.release()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.d(TAG, "failed, release lock($wakeLockTag): ${System.currentTimeMillis()}")
                    wakelock?.release()
                }
            }
            val token: IMqttToken? = comms.checkForActivity(listener)

            if (token == null) {
                val isHeld = wakelock?.isHeld ?: false
                if (isHeld) {
                    wakelock?.release()
                }
            }
        }
    }
}
