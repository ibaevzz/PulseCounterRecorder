package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FindChannelViewModel(private val PCRRepository: PCRRepository, ): ViewModel(){

    class Factory @Inject constructor(private val PCRRepository: PCRRepository)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == FindChannelViewModel::class.java){
                return FindChannelViewModel(PCRRepository) as T
            }
            return super.create(modelClass)
        }
    }

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    fun getWeight(channel: Int) = flow{
        try {
            emit(PCRRepository.getChannelWeight(channel = channel))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getValue(channel: Int) = flow{
        try {
            emit(PCRRepository.getChannelsValues(channel = channel))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getValues() = flow{
        try {
            emit(PCRRepository.getChannelsValues())
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getAddress() = flow{
        try {
            if (PCRRepository.address != 0) {
                emit(PCRRepository.address)
            }else {
                emit(PCRRepository.getPCRAddress())
            }
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun writeValues(values: Map<Int, Double>) = flow {
        try {
            emit(PCRRepository.writeChannelsValues(values = values))
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

}