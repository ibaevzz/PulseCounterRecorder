package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.db.IntermediateDatabase
import com.ibaevzz.pcr.data.db.PulsarDatabase
import java.util.*
import javax.inject.Inject

class SendDatabaseViewModel(
    private val pulsarDatabase: PulsarDatabase,
    private val intermediateDatabase: IntermediateDatabase
): ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(
        private val pulsarDatabase: PulsarDatabase,
        private val intermediateDatabase: IntermediateDatabase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == SendDatabaseViewModel::class.java){
                return SendDatabaseViewModel(pulsarDatabase, intermediateDatabase) as T
            }
            return super.create(modelClass)
        }
    }

    suspend fun updateIntermediateDatabase(otDate: Date, doDate: Date): List<String>{
        intermediateDatabase.getDao().apply {
            deleteAllDevices()
            deleteAllDevInfo()
            deleteAllMeterDevices()
            deleteAllMeterImage()
        }
        val devInfo = pulsarDatabase.getDao().getAllDevInfo().filter {
            it.date.time in otDate.time..doDate.time
        }
        intermediateDatabase.getDao().insertAllDevInfo(devInfo)
        val imagesPath = mutableListOf<String>()
        for(i in devInfo){
            val device = pulsarDatabase.getDao().getDeviceByAddress(i.devId)
            val meterDevice = pulsarDatabase.getDao().getMeterDeviceByAddress(i.meterDevId)
            val images = pulsarDatabase.getDao().getAllImagesByDevInfoId(i.id)

            for(path in images){
                imagesPath.add(path.localPath)
            }

            intermediateDatabase.getDao().apply {
                if(device != null) insertDevice(device)
                if(meterDevice != null) insertMeterDevice(meterDevice)
                insertAllImage(images)
            }
        }
        return imagesPath
    }

}