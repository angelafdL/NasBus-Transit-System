package com.nasbus.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nasbus.app.data.local.entity.CompanyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(companies: List<CompanyEntity>)

    @Query("SELECT * FROM companies ORDER BY companyName ASC")
    fun getAll(): Flow<List<CompanyEntity>>

    @Query("SELECT * FROM companies WHERE companyId = :companyId LIMIT 1")
    suspend fun getCompanyById(companyId: Int): CompanyEntity?
}
