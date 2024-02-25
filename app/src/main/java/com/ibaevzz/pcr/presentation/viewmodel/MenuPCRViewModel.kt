package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MenuPCRViewModel(private val PCRRepository: PCRRepository,
                       private val appScope: CoroutineScope): ViewModel(){

    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val appScope: CoroutineScope)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == MenuPCRViewModel::class.java){
                return MenuPCRViewModel(PCRRepository, appScope) as T
            }
            return super.create(modelClass)
        }
    }

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    init {
        appScope.launch {
            PCRRepository.getChannelsValues(channel = -1)
        }
    }

    fun getAddress(): Flow<Int?> = flow {
        try {
            emit(PCRRepository.getPCRAddress())
        }catch(ex: Exception){_errorsSharedFlow.emit(ex)}
    }
}