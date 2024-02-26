package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class WriteWeightViewModel(private val PCRRepository: PCRRepository,
                           private val appScope: CoroutineScope): ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val appScope: CoroutineScope)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == WriteWeightViewModel::class.java){
                return WriteWeightViewModel(PCRRepository, appScope) as T
            }
            return super.create(modelClass)
        }
    }

    private val _writeResult = MutableSharedFlow<Map<Int, Boolean>>(extraBufferCapacity = 16)
    val writeResult = _writeResult.asSharedFlow()

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    fun getWeights() = flow {
        try {
            emit(PCRRepository.getChannelsWeights())
        }catch(ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun writeChannels(weights: Map<Int, Double>){
        appScope.launch(Dispatchers.IO){
            try {
                val results = PCRRepository.writeChannelsWeights(values = weights)
                val mapOfResult = mutableMapOf<Int, Boolean>()
                for (i in results) {
                    mapOfResult[i.key] = i.value
                }
                _writeResult.emit(mapOfResult)
            }catch(ex: Exception){
                _errorsSharedFlow.emit(ex)
            }
        }
    }

    suspend fun getAddress(): Int? {
        return if(PCRRepository.address != 0){
            PCRRepository.address
        }else{
            try {
                PCRRepository.getPCRAddress()
            }catch(ex: Exception){
                _errorsSharedFlow.emit(ex)
                null
            }
        }
    }

    fun writeDate(date: Date? = Date()){
        appScope.launch {
            PCRRepository.writeDate(date = date).toString()
        }
    }

}