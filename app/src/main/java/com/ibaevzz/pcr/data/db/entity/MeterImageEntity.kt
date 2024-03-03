package com.ibaevzz.pcr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meter_image")
data class MeterImageEntity(
    @PrimaryKey
    val imageId: Int,
    val address: Int,
    val channel: Int,
    val localPath: String
)