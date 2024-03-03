package com.ibaevzz.pcr.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ibaevzz.pcr.data.db.DateConverter
import java.util.Date

@Entity(tableName = "dev_info")
@TypeConverters(DateConverter::class)
data class DevInfoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val address: Int,
    val user: String,
    val channel: Int,
    val name: String,
    val nameValue: Int,
    val value: Double,
    val weight: Double,
    val date: Date
)