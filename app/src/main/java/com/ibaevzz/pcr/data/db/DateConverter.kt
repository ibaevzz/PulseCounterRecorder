package com.ibaevzz.pcr.data.db

import androidx.room.TypeConverter
import java.text.SimpleDateFormat
import java.util.*

class DateConverter {

    private val simpleDateFormat = SimpleDateFormat("dd:MM:yyyy hh:mm:ss", Locale.getDefault())

    @TypeConverter
    fun stringFromDate(date: Date): String = simpleDateFormat.format(date)

    @TypeConverter
    fun stringToDate(date: String): Date = simpleDateFormat.parse(date)?:Date()

}