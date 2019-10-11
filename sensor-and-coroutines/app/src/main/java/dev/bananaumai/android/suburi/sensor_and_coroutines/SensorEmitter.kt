package dev.bananaumai.android.suburi.sensor_and_coroutines

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

private val tag = "SENSOR"

fun accelerometerFlow(context: Context) = channelFlow {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            Log.v(tag, "received event - $event")

            val sensorType = event?.sensor?.type
            if (sensorType != Sensor.TYPE_LINEAR_ACCELERATION) {
                Log.e(tag, "sensor type is not expected one $sensorType")
                return
            }

            val lat = event.values[0].toDouble() / 9.80665
            val lon = event.values[2].toDouble() / 9.80665
            val vert = event.values[1].toDouble() / 9.80665

            offer(Triple(lat, lon, vert))
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    val handlerThread =
        HandlerThread("test", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }

    val handler = Handler(handlerThread.looper)

    withContext(handler.asCoroutineDispatcher()) {
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, null)
    }

    awaitClose {
        sensorManager.unregisterListener(listener)
        handlerThread.quitSafely()
    }
}
