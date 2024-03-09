package com.ibaevzz.pcr.data.db

import androidx.room.*
import androidx.room.Dao
import com.ibaevzz.pcr.data.db.entity.*

@Dao
abstract class Dao {

    //INSERT
    @Insert
    abstract suspend fun insertDevInfo(devInfoEntity: DevInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertImage(meterImageEntity: MeterImageEntity)

    @Insert
    abstract suspend fun insertUser(userEntity: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMeterDevice(meterDeviceEntity: MeterDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDevice(deviceEntity: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAllDevInfo(devInfoEntity: List<DevInfoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAllImage(meterImageEntity: List<MeterImageEntity>)

    //DELETE
    @Query("DELETE FROM dev_info")
    abstract suspend fun deleteAllDevInfo()

    @Query("DELETE FROM image")
    abstract suspend fun deleteAllMeterImage()

    @Query("DELETE FROM device")
    abstract suspend fun deleteAllDevices()

    @Query("DELETE FROM meter_device")
    abstract suspend fun deleteAllMeterDevices()

    //GET
    @Query("SELECT id FROM image ORDER BY id DESC LIMIT 1")
    abstract suspend fun getImagesByDevInfoId(): Long?

    @Query("SELECT username FROM user LIMIT 1")
    abstract suspend fun getUsername(): String?

    @Query("SELECT * FROM user LIMIT 1")
    abstract suspend fun getUser(): UserEntity

    @Query("SELECT * FROM dev_info")
    abstract suspend fun getAllDevInfo(): List<DevInfoEntity>

    @Query("SELECT * FROM image WHERE dev_info_id = :devInfoId")
    abstract suspend fun getAllImagesByDevInfoId(devInfoId: Long): List<MeterImageEntity>

    @Query("SELECT * FROM meter_device WHERE address = :address")
    abstract suspend fun getMeterDeviceByAddress(address: Long): MeterDeviceEntity?

    @Query("SELECT * FROM device WHERE address = :address")
    abstract suspend fun getDeviceByAddress(address: Long): DeviceEntity?

}