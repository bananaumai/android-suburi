package dev.bananaumai.android.suburi.workmanager_cancellation

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val liveData = WorkManager.getInstance(this).getWorkInfosForUniqueWorkLiveData("sample_work")
        liveData.observe(this, Observer {
            it.forEach { info ->
                Log.i("Activity", "WorkInfo state is ${info?.state}")
            }

        })

        val button = findViewById<Button>(R.id.button)

        button.setOnClickListener {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<SampleWork>().setConstraints(constraints).build()
            WorkManager.getInstance(this).enqueueUniqueWork("sample_work", ExistingWorkPolicy.REPLACE, request)
        }
    }
}


class SampleWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = coroutineScope {
        val l = Loop(coroutineContext)

        repeat(11) {
            Log.d("SampleWork", "send - $it")
            l.send(it)
            delay(1000)
        }

        l.close()

        Result.success()
    }

    class Loop(coroutineContext: CoroutineContext) {
        val chan = Channel<Int>()

        private val scope = CoroutineScope(coroutineContext)

        init {
            chan.invokeOnClose { Log.d("Loop","channel was closed") }

            scope.launch {
                chan.consumeEach {
                    Log.d("Loop", "consume - $it")
                    if (it >= 10) {
                        throw Exception("won't consume any more")
                    }
                }
            }.invokeOnCompletion {
                Log.d("Loop", "launched job was completed", it)
            }
        }

        fun send(i: Int) {
            scope.launch {
                Log.d("Loop", "send - $i")
                chan.send(i)
            }
        }

        fun close() {
            chan.close()
        }
    }
}
