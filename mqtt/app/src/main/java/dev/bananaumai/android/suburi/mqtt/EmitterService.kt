package dev.bananaumai.android.suburi.mqtt

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class EmitterService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.d("EmitterService", "onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("EmitterService", "onStartCommand")
        return START_STICKY
    }
}
