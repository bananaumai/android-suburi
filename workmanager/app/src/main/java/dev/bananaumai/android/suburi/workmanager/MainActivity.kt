package dev.bananaumai.android.suburi.workmanager

import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.*

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MainActivity : AppCompatActivity() {

    private val workManager = WorkManager.getInstance(this)

    private var counter = AtomicInteger()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

//        val pData = workDataOf("COUNT" to 999999)
//        val pReq = PeriodicWorkRequestBuilder<SampleWork>(15, TimeUnit.MINUTES).setInputData(pData).build()
//        workManager.enqueueUniquePeriodicWork("SampleWork", ExistingPeriodicWorkPolicy.KEEP, pReq)
//        workManager.getWorkInfoByIdLiveData(pReq.id).observe(this@MainActivity, Observer { info ->
//            if (info?.state == WorkInfo.State.SUCCEEDED) {
//                Log.i("BNN", "PERIOD : succeed! ${info.outputData.getInt("COUNT", 0)}")
//            } else {
//                Log.e("BNN", "PERIOD : info.state => ${info?.state}")
//            }
//        })

        val container = findViewById<CoordinatorLayout>(R.id.container)

        workManager.getWorkInfosForUniqueWorkLiveData("SampleWork").observe(this, Observer { info ->
            info?.stream()?.forEach { info ->
                when(info?.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val output = info.outputData.getInt("COUNT", 0)
                        Log.i("BNN:MainActivity", "${info.state} - $output")
                        Snackbar
                            .make(container, "${info.state} - $output", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    }
                    WorkInfo.State.FAILED, null -> {
                        Log.e("BNN:MainActivity", "${info?.state}")
                    }
                    else -> {
                        Log.i("BNN:MainActivity", "${info.state}")
                    }
                }
            }
        })

        fab.setOnClickListener { view ->
            //workManager.cancelUniqueWork("SampleWork")

            val n = counter.incrementAndGet()

            val data = workDataOf("COUNT" to n)
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<SampleWork>().setInputData(data).setConstraints(constraints).build()

            Log.i("BNN:MainActivity", "#$n : enqueue unique work [ id = ${request.id} ]")

            workManager.enqueueUniqueWork("SampleWork", ExistingWorkPolicy.KEEP, request)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
