package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.db.PulsarDatabase
import com.ibaevzz.pcr.data.db.entity.DeviceEntity
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MenuPCRViewModel(private val PCRRepository: PCRRepository,
                       appScope: CoroutineScope,
                       private val pulsarDatabase: PulsarDatabase): ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val appScope: CoroutineScope,
                                      private val pulsarDatabase: PulsarDatabase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == MenuPCRViewModel::class.java){
                return MenuPCRViewModel(PCRRepository, appScope, pulsarDatabase) as T
            }
            return super.create(modelClass)
        }
    }

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    private val _completeSharedFlow = MutableSharedFlow<Int>()
    val completeSharedFlow = _completeSharedFlow.asSharedFlow()

    init {
        appScope.launch {
            try {
                val address = PCRRepository.getPCRAddress() ?: -1
                val devName = PCRRepository.getDeviceType() ?: "Неизвестное устройство"
                PCRRepository.getChannelsValues(channel = -1)
                pulsarDatabase.getDao()
                    .insertDevice(DeviceEntity(address.toLong(), devName))
                _completeSharedFlow.emit(address)
            }catch (ex: Exception){
                _completeSharedFlow.emit(-1)
                _errorsSharedFlow.emit(ex)
            }
        }
    }

    fun getAddress(): Flow<Int?> = flow {
        try {
            PCRRepository.clearDevice()
            val address = PCRRepository.getPCRAddress() ?: -1
            val devName = PCRRepository.getDeviceType() ?: "Неизвестное устройство"
            PCRRepository.getChannelsValues()
            pulsarDatabase.getDao()
                .insertDevice(DeviceEntity(address.toLong(), devName))
            emit(PCRRepository.getPCRAddress())
        }catch(ex: Exception){_errorsSharedFlow.emit(ex)}
    }
}