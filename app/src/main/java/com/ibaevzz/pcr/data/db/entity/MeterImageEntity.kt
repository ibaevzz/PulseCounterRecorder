package com.ibaevzz.pcr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meter_image")
data class MeterImageEntity(
    @PrimaryKey
    val id: Long,
    val devInfoId: Long,
    val localPath: String
)