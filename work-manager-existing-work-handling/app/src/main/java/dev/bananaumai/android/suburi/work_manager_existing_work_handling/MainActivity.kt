package dev.bananaumai.android.suburi.work_manager_existing_work_handling

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val workManager = WorkManager.getInstance(this)

        val keepButton = findViewById<Button>(R.id.keep_button)
        keepButton.setOnClickListener { requestWork(workManager, ExistingWorkPolicy.KEEP) }

        val appendButton = findViewById<Button>(R.id.append_button)
        appendButton.setOnClickListener { requestWork(workManager, ExistingWorkPolicy.APPEND) }

        val replaceButton = findViewById<Button>(R.id.replace_button)
        replaceButton.setOnClickListener { requestWork(workManager, ExistingWorkPolicy.REPLACE) }

        val cancelButton = findViewById<Button>(R.id.cancel_button)
        cancelButton.setOnClickListener {
            Log.i("Activity", "cancel work")
            workManager.cancelUniqueWork("work")

            val workInfos = workManager.getWorkInfosForUniqueWork("work").get()
            for (workInfo in workInfos) {
                Log.i("Activity", "cancel $workInfo")
                workManager.cancelWorkById(workInfo.id)
            }
        }

        workManager
            .getWorkInfosForUniqueWorkLiveData("work")
            .observe(this, Observer { workInfos ->
                for (workInfo in workInfos) {
                    Log.i("Observer", "workInfo - ${workInfo.id} : ${workInfo.state}")
                }
            })
    }

    private fun requestWork(workManager: WorkManager, existingWorkPolicy: ExistingWorkPolicy) {
        val workInfos = workManager.getWorkInfosForUniqueWork("work").get()
        for (workInfo in workInfos) {
            Log.i("Activity", "workInfo - ${workInfo.id} : ${workInfo.state}")
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val value = "${existingWorkPolicy.name}: ${Random.nextInt()}"
        Log.i("Activity", "request work with $value")

        val request = OneTimeWorkRequestBuilder<SampleWork>()
            .setConstraints(constraints)
            .setInputData(workDataOf("value" to value))
            .build()

        workManager.enqueueUniqueWork("work", existingWorkPolicy, request)
    }
}

class SampleWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @InternalCoroutinesApi
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val value = inputData.getString("value")
            Log.i("Worker", "Worker($id) start with $value")

            when(Random.nextInt(3)) {
                0 -> {
                    Log.i("Worker", "Worker($id) will fail")
                    delay(5000)
                    //Result.failure()
                    throw RuntimeException("Worker failed")
                }
                1 -> {
                    Log.i("Worker", "Worker($id) will succeed")
                    delay(5000)
                    Result.success()
                }
                else -> {
                    Log.i("Worker", "Worker($id) will retry")
                    delay(5000)
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            Log.e("Worker", "$e")
            throw e
        }
    }
}
