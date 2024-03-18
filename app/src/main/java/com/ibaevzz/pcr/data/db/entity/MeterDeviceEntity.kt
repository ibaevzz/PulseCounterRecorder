package com.ibaevzz.pcr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meter_device")
data class MeterDeviceEntity(
    @PrimaryKey
    val address: Long,
    val resource: String?
)
