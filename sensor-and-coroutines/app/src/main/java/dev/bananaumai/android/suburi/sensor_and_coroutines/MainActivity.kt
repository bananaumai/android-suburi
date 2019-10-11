package dev.bananaumai.android.suburi.sensor_and_coroutines

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import android.widget.Button
import kotlinx.coroutines.*
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    private val sj = SupervisorJob()
    private val scope = CoroutineScope(sj + Dispatchers.Default)

    private var working = false
    private var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            button.isEnabled = false

            working = !working

            if (working) {
                button.text = "stop"
                start()
            } else {
                stop()
                button.text = "run"
            }

            button.isEnabled = true
        }
    }

    private fun start() {
        job = scope.launch {
            accelerometerFlow(this@MainActivity)
                .collect {
                    Log.i("MainActivity", "$it")
                }
        }
    }

    private fun stop() {
        job?.cancel()
    }
}

fun accelerometerFlow(context: Context) = channelFlow {
    val handlerThread =
        HandlerThread("test", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }

    val handler = Handler(handlerThread.looper)

    val handlerScope = CoroutineScope(coroutineContext + handler.asCoroutineDispatcher())

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            Log.v("accelerometerFlow", "$event")

            val sensorType = event?.sensor?.type
            if (sensorType != Sensor.TYPE_LINEAR_ACCELERATION) {
                return
            }

            val lat = event.values[0]
            val lon = event.values[2]
            val vert = event.values[1]

            handlerScope.launch {
                send(Triple(lat, lon, vert))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)

    awaitClose {
        sensorManager.unregisterListener(listener, sensor)
        handlerThread.quitSafely()
    }
}
