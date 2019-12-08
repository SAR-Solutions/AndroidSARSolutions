package com.example.sarsolutions

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import timber.log.Timber

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Timber for logging
        if (BuildConfig.DEBUG)
            Timber.plant(Timber.DebugTree())


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
}