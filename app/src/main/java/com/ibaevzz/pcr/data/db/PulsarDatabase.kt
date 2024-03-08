package com.ibaevzz.pcr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ibaevzz.pcr.data.db.entity.*

@Database(
    entities = [
        DevInfoEntity::class,
        MeterImageEntity::class,
        DeviceEntity::class,
        MeterDeviceEntity::class,
        UserEntity::class],
    version = 1
)
abstract class PulsarDatabase: RoomDatabase(){
    abstract fun getDao(): Dao
}