package dev.bananaumai.android.suburi.mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class EmitterService : Service() {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private lateinit var randomIntStream: Flow<Int>
    private lateinit var job: Job

    override fun onCreate() {
        super.onCreate()
        Log.v("EmitterService", "onCreate")

        randomIntStream = flow {
            while(true) {
                delay(1000)
                val num = Random.nextInt(0, 100)
                emit(num)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.v("EmitterService", "onStartCommand")

        if(::job.isInitialized) {
            Log.d("EmitterService","Job is already started")
        } else {
            job = serviceScope.launch {
                randomIntStream.collect { num ->
                    Log.d("EmitterService", "consume random stream: $num")
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v("EmitterService", "onDestroy")
        serviceJob.cancel()
    }
}
