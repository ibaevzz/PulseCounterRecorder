package com.ibaevzz.pcr.data.db

import androidx.room.*
import androidx.room.Dao
import com.ibaevzz.pcr.data.db.entity.DevInfoEntity
import com.ibaevzz.pcr.data.db.entity.DeviceEntity
import com.ibaevzz.pcr.data.db.entity.MeterImageEntity
import com.ibaevzz.pcr.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class Dao {

    //INSERT
    @Insert
    abstract suspend fun insertDevInfo(devInfoEntity: DevInfoEntity)

    @Insert
    abstract suspend fun insertImage(meterImageEntity: MeterImageEntity)

    @Insert
    abstract suspend fun insertUser(userEntity: UserEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertDevice(deviceEntity: DeviceEntity)

    //DELETE
    @Query("DELETE FROM dev_info")
    abstract suspend fun deleteAllDevInfo()

    @Query("DELETE FROM meter_image")
    abstract suspend fun deleteAllMeterImage()

    @Query("DELETE FROM devices")
    abstract suspend fun deleteAllDevices()

    //GET
    @Query("SELECT * FROM dev_info")
    abstract fun getAllDevInfo(): Flow<List<DevInfoEntity>>

    @Query("SELECT * FROM meter_image, dev_info, devices " +
            "WHERE dev_info.devId = devices.id AND meter_image.devInfoId = dev_info.id AND " +
            "devices.address = :address AND dev_info.channel = :channel")
    abstract fun getMeterImages(address: Int, channel: Int): Flow<List<MeterImageEntity>>

    @Query("SELECT username FROM user LIMIT 1")
    abstract suspend fun getUsername(): String?

    @Query("SELECT * FROM user LIMIT 1")
    abstract suspend fun getUser(): UserEntity

}