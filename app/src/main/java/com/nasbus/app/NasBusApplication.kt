package com.nasbus.app

import android.app.Application
import com.nasbus.app.data.seed.DatabaseSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre

class NasBusApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()

        // Initialize MapLibre GL Native SDK globally
        MapLibre.getInstance(applicationContext)

        applicationScope.launch {
            DatabaseSeeder.seedDatabase(applicationContext)
        }
    }
}
