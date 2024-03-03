package com.ibaevzz.pcr.data.db

import androidx.room.*
import androidx.room.Dao
import com.ibaevzz.pcr.data.db.entity.DevInfoEntity
import com.ibaevzz.pcr.data.db.entity.MeterImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class Dao {

    //INSERT
    @Insert
    abstract suspend fun insertDevInfo(devInfoEntity: DevInfoEntity)

    @Insert
    abstract suspend fun insertImage(meterImageEntity: MeterImageEntity)

    //DELETE
    @Query("DELETE FROM dev_info")
    abstract suspend fun deleteAllDevInfo()

    @Query("DELETE FROM meter_image")
    abstract suspend fun deleteAllMeterImage()

    @Transaction
    open suspend fun deleteAllInfo(){
        deleteAllDevInfo()
        deleteAllMeterImage()
    }

    //GET
    @Query("SELECT * FROM dev_info")
    abstract fun getAllDevInfo(): Flow<List<DevInfoEntity>>

    @Query("SELECT * FROM meter_image WHERE address = :address AND channel = :channel")
    abstract fun getMeterImages(address: Int, channel: Int): Flow<List<MeterImageEntity>>

    @Query("SELECT imageId FROM meter_image " +
            "ORDER BY imageId DESC " +
            "LIMIT 1")
    abstract fun getLastImage(): Int

}