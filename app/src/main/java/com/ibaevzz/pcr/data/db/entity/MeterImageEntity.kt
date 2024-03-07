package com.ibaevzz.pcr.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image")
data class MeterImageEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo("dev_info_id")
    val devInfoId: Long,
    @ColumnInfo("name_of_image")
    val localPath: String
)