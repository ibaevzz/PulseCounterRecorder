package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ChannelViewModel(private val PCRRepository: PCRRepository): ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val PCRRepository: PCRRepository)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ChannelViewModel::class.java){
                return ChannelViewModel(PCRRepository) as T
            }
            return super.create(modelClass)
        }
    }

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

    fun getDeviceType() = flow{
        try {
            emit(PCRRepository.getDeviceType())
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

}