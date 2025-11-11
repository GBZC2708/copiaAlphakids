package com.example.alphakids

import android.app.Application
import com.example.alphakids.data.seed.DemoDataSeeder
import com.example.alphakids.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class AlphakidsApp : Application() {

    @Inject
    lateinit var demoDataSeeder: DemoDataSeeder

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEMO_MODE) {
            applicationScope.launch {
                demoDataSeeder.seedIfNeeded()
            }
        }
    }
}
