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
    version = 2
)
abstract class IntermediateDatabase: RoomDatabase(){
    abstract fun getDao(): Dao
}