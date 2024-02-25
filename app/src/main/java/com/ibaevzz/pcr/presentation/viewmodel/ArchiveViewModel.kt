package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import java.util.*
import javax.inject.Inject

class ArchiveViewModel(private val PCRRepository: PCRRepository): ViewModel(){

    class Factory @Inject constructor(private val PCRRepository: PCRRepository)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ArchiveViewModel::class.java){
                return ArchiveViewModel(PCRRepository) as T
            }
            return super.create(modelClass)
        }
    }

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    fun getArchive(channel: Int, startDate: Date, endDate: Date, type: PCRRepository.Companion.ArchiveTypes) = flow {
        try {
            emit(
                PCRRepository.readArchive(
                    channel = channel,
                    startDate = startDate,
                    endDate = endDate,
                    type = type
                )
            )
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getAddress() = flow{
        try {
            if (PCRRepository.address != 0) {
                emit(PCRRepository.address)
            } else {
                emit(PCRRepository.getPCRAddress())
            }
        }catch (ex: Exception){
            _errorsSharedFlow.emit(ex)
        }
    }

    fun getCount() = if(PCRRepository.is10) 10 else 16
}