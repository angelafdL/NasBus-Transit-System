package com.nasbus.app.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nasbus.app.data.local.dao.BusDao
import com.nasbus.app.data.local.dao.CompanyDao
import com.nasbus.app.data.local.dao.RouteDao
import com.nasbus.app.data.local.dao.ScheduleDao
import com.nasbus.app.data.local.dao.StopDao
import com.nasbus.app.data.local.dao.UserDao
import com.nasbus.app.data.local.entity.BusEntity
import com.nasbus.app.data.local.entity.CompanyEntity
import com.nasbus.app.data.local.entity.RouteEntity
import com.nasbus.app.data.local.entity.ScheduleEntity
import com.nasbus.app.data.local.entity.StopEntity
import com.nasbus.app.data.local.entity.UserEntity
import com.nasbus.app.util.Constants

@Database(
    entities = [
        UserEntity::class,
        CompanyEntity::class,
        BusEntity::class,
        RouteEntity::class,
        StopEntity::class,
        ScheduleEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun companyDao(): CompanyDao
    abstract fun busDao(): BusDao
    abstract fun routeDao(): RouteDao
    abstract fun stopDao(): StopDao
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
