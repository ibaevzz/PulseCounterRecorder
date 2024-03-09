package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.db.PulsarDatabase
import com.ibaevzz.pcr.data.db.entity.DevInfoEntity
import com.ibaevzz.pcr.data.db.entity.MeterDeviceEntity
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import java.util.Date
import javax.inject.Inject

class ChannelViewModel(private val PCRRepository: PCRRepository,
                       private val pulsarDatabase: PulsarDatabase): ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val pulsarDatabase: PulsarDatabase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ChannelViewModel::class.java){
                return ChannelViewModel(PCRRepository, pulsarDatabase) as T
            }
            return super.create(modelClass)
        }
    }

    var id: Long = Date().time

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    fun getAddress() = flow {
        try{
            if(PCRRepository.address == 0) {
                emit(PCRRepository.getPCRAddress())
            }else{
                emit(PCRRepository.address)
            }
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun writeWeight(channel: Int, weight: Double) = flow{
        try {
            emit(PCRRepository.writeChannelWeight(channel = channel, weight = weight))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getWeight(channel: Int) = flow{
        try{
            emit(PCRRepository.getChannelWeight(channel = channel))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun writeValue(channel: Int, value: Double) = flow{
        try {
            emit(PCRRepository.writeChannelValue(channel = channel, value = value))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getValue(channel: Int) = flow{
        try{
            val value = PCRRepository.getChannelsValues(channel = channel)
            emit(value?.get(channel))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    suspend fun writeToDB(channel: Int, meterNumber: Long){
        try {
            val address = PCRRepository.getPCRAddress()?:-1
            val date = Date()
            val value = PCRRepository.getChannelsValues(channel = channel)
            val weight = PCRRepository.getChannelWeight(channel = channel - 1)
            val user = pulsarDatabase.getDao().getUser().id

            val meterDeviceEntity = MeterDeviceEntity(meterNumber)
            val devInfoEntity = DevInfoEntity(
                id,
                address.toLong(),
                meterNumber,
                user,
                channel,
                value?.get(channel)?:-1.0,
                weight?:-1.0,
                date
            )

            pulsarDatabase.getDao().insertMeterDevice(meterDeviceEntity)
            pulsarDatabase.getDao().insertDevInfo(devInfoEntity)

            id = Date().time
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

}