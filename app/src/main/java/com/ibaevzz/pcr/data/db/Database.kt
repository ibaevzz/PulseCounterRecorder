package com.ibaevzz.pcr.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ibaevzz.pcr.data.db.entity.DevInfoEntity
import com.ibaevzz.pcr.data.db.entity.DeviceEntity
import com.ibaevzz.pcr.data.db.entity.MeterImageEntity
import com.ibaevzz.pcr.data.db.entity.UserEntity

@Database(
    entities = [DevInfoEntity::class, MeterImageEntity::class, DeviceEntity::class, UserEntity::class],
    version = 1
)
abstract class PulsarDatabase: RoomDatabase(){
    abstract fun getDao(): Dao
}