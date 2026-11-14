package com.nasbus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nasbus.app.data.local.entity.BusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buses: List<BusEntity>)

    @Query("SELECT * FROM buses WHERE companyId = :companyId ORDER BY busNumber ASC")
    fun getByCompany(companyId: Int): Flow<List<BusEntity>>
}
