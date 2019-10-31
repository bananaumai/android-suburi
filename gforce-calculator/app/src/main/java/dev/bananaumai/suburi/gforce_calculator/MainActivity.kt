package dev.bananaumai.suburi.gforce_calculator

import android.app.Application
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
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import dev.bananaumai.suburi.gforce_calculator.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewModelFactory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        val viewModelProvide = ViewModelProvider(this, viewModelFactory)

        val binding : ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        binding.acceleration = viewModelProvide.get(AccelerationViewModel::class.java)
        binding.gravity = viewModelProvide.get(GravityViewModel::class.java)
    }
}

@ExperimentalCoroutinesApi
class AccelerationViewModel(application: Application) : AndroidViewModel(application) {
    val data by lazy {
        sensorFlow(application, Sensor.TYPE_ACCELEROMETER) { Acceleration(it[0], it[1], it[2]) }.asLiveData()
    }
}

data class Acceleration(val x: Float, val y: Float, val z: Float) {
    val gforce = sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

@ExperimentalCoroutinesApi
class GravityViewModel(application: Application) : AndroidViewModel(application) {
    val data by lazy {
        sensorFlow(application, Sensor.TYPE_GRAVITY) { Gravity(it[0], it[1], it[2]) }.asLiveData()
    }
}

data class Gravity(val x: Float, val y: Float, val z: Float) {
    val gforce = sqrt(x.pow(2) + y.pow(2) + z.pow(2))
}

@ExperimentalCoroutinesApi
fun <T> sensorFlow(context: Context, sensorType: Int, creator: (values: List<Float>) -> T) = channelFlow {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.values == null) {
                return
            }
            CoroutineScope(coroutineContext).launch {
                send(creator(event.values.toList()))
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    val sensor = sensorManager.getDefaultSensor(sensorType)

    val handlerThread = HandlerThread("accelerometer", Process.THREAD_PRIORITY_BACKGROUND).apply { start() }

    val handler = Handler(handlerThread.looper)

    try {
        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
        awaitClose()
    } finally {
        sensorManager.unregisterListener(sensorEventListener, sensor)
        handlerThread.quitSafely()
    }
}

