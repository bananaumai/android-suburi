package dev.bananaumai.android.suburi.workmanager

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlin.random.Random

class SampleWork(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val n = inputData.getInt("COUNT", 0)

        val delayMilli = Random.nextLong(1000, 5000)

        Log.d("BNN:SampleWork", "#$n : will delay $delayMilli")

        delay(delayMilli)

        return when (Random.nextInt(0, 3)) {
            0 -> {
                Log.i("BNN:SampleWork", "#$n : will fail")
                Result.failure()
            }
            1 -> {
                Log.i("BNN:SampleWork", "#$n : will retry")
                Result.retry()
            }
            else -> {
                Log.i("BNN:SampleWork", "$n : will succeed")
                val data = workDataOf("COUNT" to n)
                Result.success(data)
            }
        }
    }
}
