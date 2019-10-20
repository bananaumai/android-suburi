package dev.bananaumai.android.suburi.workmanager_unique

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var isRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.button)
        button.text = "start"
        button.setOnClickListener {
            button.isEnabled = false

            isRunning = !isRunning

            if (isRunning) {
                button.text = "stop"
                requestOnetimeWork(ExistingWorkPolicy.REPLACE)
            } else {
                button.text = "start"
                requestPeriodicWork(ExistingPeriodicWorkPolicy.REPLACE)
            }

            button.isEnabled = true
        }

        WorkManager
            .getInstance(this)
            .getWorkInfosForUniqueWorkLiveData("sample_work")
            .observe(this, Observer { infoList ->
                for (info in infoList) {
                    Log.i("Observer", "info.state = ${info.state}")
                }
            })
    }

    private fun requestOnetimeWork(existingWorkPolicy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<SampleWork>().build()
        WorkManager
            .getInstance(this)
            .enqueueUniqueWork("sample_work", existingWorkPolicy, request)
    }

    private fun requestPeriodicWork(existingPeriodicWorkPolicy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<SampleWork>(15, TimeUnit.MINUTES).build()
        WorkManager
            .getInstance(this)
            .enqueueUniquePeriodicWork("sample_work", existingPeriodicWorkPolicy, request)
    }

}

class SampleWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val f = flow {
        repeat(10) { emit(it) }
    }

    override suspend fun doWork(): Result = coroutineScope {
        f.collect {
            Log.v("worker", "$it")
            delay(1000)
        }
        Result.success()
    }
}
