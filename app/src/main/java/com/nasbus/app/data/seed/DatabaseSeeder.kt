package com.nasbus.app.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nasbus.app.data.local.db.AppDatabase
import com.nasbus.app.data.local.entity.BusEntity
import com.nasbus.app.data.local.entity.CompanyEntity
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.ScheduleEntity
import com.nasbus.app.data.local.entity.StopEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseSeeder {

    private const val TAG = "DatabaseSeeder"
    private const val PREFS_NAME = "nasbus_prefs"
    private const val KEY_SEED_VERSION = "seed_version"
    private const val SEED_VERSION = 3

    suspend fun seedDatabase(context: Context) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val storedVersion = prefs.getInt(KEY_SEED_VERSION, 0)

            if (storedVersion == SEED_VERSION) {
                Log.d(TAG, "Database is already at seed version $SEED_VERSION. Skipping reseeding.")
                return@withContext
            }

            val gson = Gson()
            val db = AppDatabase.getInstance(context)

            try {
                val companiesJson = readAssetJson(context, "companies.json")
                val busesJson = readAssetJson(context, "buses.json")
                val routesJson = readAssetJson(context, "routes.json")
                val stopsJson = readAssetJson(context, "stops.json")
                val schedulesJson = readAssetJson(context, "schedules.json")

                if (companiesJson == null || busesJson == null || routesJson == null || stopsJson == null || schedulesJson == null) {
                    Log.e(TAG, "Seeding aborted: One or more seed JSON assets could not be loaded.")
                    return@withContext
                }

                val companyType = object : TypeToken<List<CompanyEntity>>() {}.type
                val busType = object : TypeToken<List<BusEntity>>() {}.type
                val routeType = object : TypeToken<List<RouteEntity>>() {}.type
                val stopType = object : TypeToken<List<StopEntity>>() {}.type
                val scheduleType = object : TypeToken<List<ScheduleEntity>>() {}.type

                val companies: List<CompanyEntity> = gson.fromJson(companiesJson, companyType) ?: emptyList()
                val buses: List<BusEntity> = gson.fromJson(busesJson, busType) ?: emptyList()
                val routes: List<RouteEntity> = gson.fromJson(routesJson, routeType) ?: emptyList()
                val stops: List<StopEntity> = gson.fromJson(stopsJson, stopType) ?: emptyList()
                val schedules: List<ScheduleEntity> = gson.fromJson(schedulesJson, scheduleType) ?: emptyList()

                db.withTransaction {
                    val sdb = db.openHelper.writableDatabase
                    sdb.execSQL("DELETE FROM schedules")
                    sdb.execSQL("DELETE FROM stops")
                    sdb.execSQL("DELETE FROM routes")
                    sdb.execSQL("DELETE FROM buses")
                    sdb.execSQL("DELETE FROM companies")
                    // Note: users table is intentionally NOT deleted!

                    db.companyDao().insertAll(companies)
                    db.busDao().insertAll(buses)
                    db.routeDao().insertAll(routes)
                    db.stopDao().insertAll(stops)
                    db.scheduleDao().insertAll(schedules)
                }

                prefs.edit().putInt(KEY_SEED_VERSION, SEED_VERSION).apply()

                Log.i(TAG, "Database reseeding to version $SEED_VERSION completed successfully.")
                Log.i(TAG, "Inserted ${companies.size} companies")
                Log.i(TAG, "Inserted ${buses.size} buses")
                Log.i(TAG, "Inserted ${routes.size} routes")
                Log.i(TAG, "Inserted ${stops.size} stops")
                Log.i(TAG, "Inserted ${schedules.size} schedules")

            } catch (e: Exception) {
                Log.e(TAG, "Error occurred during database reseeding", e)
            }
        }
    }

    private fun readAssetJson(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read asset file: $fileName", e)
            null
        }
    }
}
