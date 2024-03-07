package com.ibaevzz.pcr.data.db

import androidx.room.*
import androidx.room.Dao
import com.ibaevzz.pcr.data.db.entity.*
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
    abstract suspend fun insertMeterDevice(meterDeviceEntity: MeterDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertDevice(deviceEntity: DeviceEntity)

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
    @Query("SELECT image.id, image.dev_info_id, image.name_of_image " +
            "FROM image, dev_info, device, meter_device " +
            "WHERE dev_info.device_address = device.address AND image.dev_info_id = dev_info.id AND " +
            "device.address = :devAddress AND dev_info.channel = :channel AND "+
            "meter_device.address = dev_info.meter_device_address AND meter_device.address = :meterDevAddress")
    abstract fun getImages(devAddress: Long, meterDevAddress: Long, channel: Int): Flow<List<MeterImageEntity>>

    @Query("SELECT id FROM image WHERE dev_info_id = :devInfoId ORDER BY id DESC LIMIT 1")
    abstract suspend fun getImagesByDevInfoId(devInfoId: Long): Long?

    @Query("SELECT username FROM user LIMIT 1")
    abstract suspend fun getUsername(): String?

    @Query("SELECT * FROM user LIMIT 1")
    abstract suspend fun getUser(): UserEntity

}