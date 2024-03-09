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

class WriteValuesViewModel(private val PCRRepository: PCRRepository,
                           private val appScope: CoroutineScope): ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val appScope: CoroutineScope)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == WriteValuesViewModel::class.java){
                return WriteValuesViewModel(PCRRepository, appScope) as T
            }
            return super.create(modelClass)
        }
    }

    private val _writeResult = MutableSharedFlow<Boolean>()
    val writeResult = _writeResult.asSharedFlow()

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    fun getValues() = flow {
        try {
            emit(PCRRepository.getChannelsValues())
        }catch(ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun writeValues(values: Map<Int, Double>){
        appScope.launch(Dispatchers.IO){
            try {
                val correctValues = values.mapKeys { it.key - 1 }
                val results = PCRRepository.writeChannelsValues(values = correctValues)
                _writeResult.emit(results)
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

}