package dev.bananaumai.android.suburi.log

import android.app.Application
import android.util.Log
import java.io.File

class MyApplication : Application() {
    companion object {
        const val TAG = "LogToFile"
    }

    private lateinit var logfile: File
    private lateinit var loggingProcess: Process

    override fun onCreate() {
        super.onCreate()
        logfile = File(filesDir, "log.txt")
        loggingProcess = Runtime.getRuntime().exec("logcat -f ${logfile.absolutePath} -r 100 -n 2 -v time *:S ${TAG}:I")
        Log.d(TAG, "This should be ignored")
        Log.i(TAG, "hello world")
    }

    override fun onTerminate() {
        super.onTerminate()
        // I'm not sure whether or not this is meaningful.
        loggingProcess.destroy()
    }
}
