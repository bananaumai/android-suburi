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
import kotlinx.coroutines.flow.onCompletion

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
            val handlerThread = HandlerThread("test", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }
            val handler = Handler(handlerThread.looper)
            accelerometerFlow(this@MainActivity, handler)
                .onCompletion {
                    Log.i("MainActivity", "accelerometerFlow completed")
                    handlerThread.quitSafely()
                }
                .collect {
                    Log.d("MainActivity", "$it")
                }
        }
    }

    private fun stop() {
        job?.cancel()
    }
}

fun accelerometerFlow(context: Context, handler: Handler) = channelFlow {
    val handlerScope = CoroutineScope(coroutineContext + handler.asCoroutineDispatcher())

    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            Log.v("accelerometerFlow", "$event")
            handlerScope.launch {
                send(event?.values?.toList())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)

    awaitClose {
        sensorManager.unregisterListener(sensorEventListener, sensor)
        Log.i("accelerometerFlow", "unregistered listener")
    }
}
