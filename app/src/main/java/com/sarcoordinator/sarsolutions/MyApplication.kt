package com.sarcoordinator.sarsolutions

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import timber.log.Timber

class MyApplication : Application() {

    companion object {
        val CHANNEL_ID = "SARLocationServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()

        // Timber for logging
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Location Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(serviceChannel)
        }
    }
}