package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.db.PulsarDatabase
import com.ibaevzz.pcr.data.db.entity.MeterImageEntity
import javax.inject.Inject

class PhotoViewModel(private val pulsarDatabase: PulsarDatabase): ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val pulsarDatabase: PulsarDatabase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == PhotoViewModel::class.java){
                return PhotoViewModel(pulsarDatabase) as T
            }
            return super.create(modelClass)
        }
    }

    suspend fun getIdForImage(devInfoId: Long): Long{
        val lastId = pulsarDatabase.getDao().getImagesByDevInfoId(devInfoId)
        return (lastId?:0L) + 1L
    }

    suspend fun writeToDb(id: Long, devInfoId: Long, path: String){
        pulsarDatabase.getDao().insertImage(
            MeterImageEntity(
                id,
                devInfoId,
                path
            )
        )
    }

}