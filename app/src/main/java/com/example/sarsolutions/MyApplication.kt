package com.example.sarsolutions

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class MyApplication : Application() {

    companion object {
        public final val CHANNEL_ID = "SARLocationServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()

        // Timber for logging
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())

        createNotificationChannel()

        // Koin Dependency Injection
        val appModule = module {
            viewModel { MainViewModel() }
            viewModel { CasesViewModel() }
        }

        startKoin {
            androidContext(this@MyApplication)
            modules(appModule)
        }
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