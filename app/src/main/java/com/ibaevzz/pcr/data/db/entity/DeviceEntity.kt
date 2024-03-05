package com.ibaevzz.pcr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey
    val id: Long,
    val address: Int,
    val name: String,
)