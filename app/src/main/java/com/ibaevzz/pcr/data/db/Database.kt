package com.ibaevzz.pcr.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ibaevzz.pcr.data.db.entity.DevInfoEntity
import com.ibaevzz.pcr.data.db.entity.MeterImageEntity

@Database(
    entities = [DevInfoEntity::class, MeterImageEntity::class],
    version = 1
)
abstract class Database: RoomDatabase(){
    abstract fun getDao(): Dao
}