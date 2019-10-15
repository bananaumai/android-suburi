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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {
    private val supervisor = SupervisorJob()
    private val backgroundScope = CoroutineScope(supervisor + Dispatchers.Default)

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
        job = backgroundScope.launch {
            accelerometerFlow(this@MainActivity)
                .onEach { Log.d("Throttle", it.toString() )}
                .collect {
                    Log.d("MainActivity", "$it - ${Thread.currentThread().name}")
                }
        }
    }

    private fun stop() {
        job?.cancel()
    }
}

@ExperimentalCoroutinesApi
fun accelerometerFlow(context: Context) = channelFlow {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            Log.v("accelerometerFlow", "$event - ${Thread.currentThread().name}")
            CoroutineScope(coroutineContext).launch {
                Log.v("accelerometerFlow", "inside launch - ${Thread.currentThread().name}")
                offer(event?.values?.toList())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    val handlerThread = HandlerThread("test", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }

    val handler = Handler(handlerThread.looper)

    sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)

    awaitClose {
        sensorManager.unregisterListener(sensorEventListener, sensor)
        handlerThread.quitSafely()
    }
}
