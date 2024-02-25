package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
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

    fun getWeight(channel: Int) = flow{
        emit(PCRRepository.getChannelWeight(channel = channel))
    }

    fun getWeights(channel: Int) = flow{
        emit(PCRRepository.getChannelsWeights())
    }

    fun getValue(channel: Int) = flow{
        emit(PCRRepository.getChannelsValues(channel = channel))
    }

    fun getValues() = flow{
        emit(PCRRepository.getChannelsValues())
    }

    fun getAddress() = flow{
        if(PCRRepository.address!=0){
            emit(PCRRepository.address)
        }
        emit(PCRRepository.getPCRAddress())
    }

    fun writeValue(channel: Int, value: Double) = flow {
        emit(PCRRepository.writeChannelValue(channel = channel, value = value))
    }

    fun writeValues(values: Map<Int, Double>) = flow {
        emit(PCRRepository.writeChannelsValues(values = values))
    }

}