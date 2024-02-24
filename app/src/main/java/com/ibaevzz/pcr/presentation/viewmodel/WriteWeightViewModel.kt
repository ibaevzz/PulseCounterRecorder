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
import javax.inject.Inject

class WriteWeightViewModel(private val PCRRepository: PCRRepository,
                           private val appScope: CoroutineScope): ViewModel(){

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

    fun getWeights() = flow {
        emit(PCRRepository.getChannelsWeights())
    }

    fun getWeight(channel: Int) = flow {
        emit(PCRRepository.getChannelWeight(channel = channel))
    }

    fun writeChannels(weights: Map<Int, Double>){
        appScope.launch(Dispatchers.IO){
            val results = PCRRepository.writeChannelsWeights(values = weights)
            val mapOfResult = mutableMapOf<Int, Boolean>()
            for(i in results.indices){
                mapOfResult[i+1] = results[i]
            }
            _writeResult.emit(mapOfResult)
        }
    }

    fun writeChannel(channel: Int, weight: Double){
        appScope.launch {
            _writeResult.emit(
                mapOf(channel to PCRRepository.writeChannelWeight(channel = channel, weight = weight))
            )
        }
    }

    suspend fun getAddress(): Int? {
        return if(PCRRepository.address != 0){
            PCRRepository.address
        }else{
            PCRRepository.getPCRAddress()
        }
    }

}