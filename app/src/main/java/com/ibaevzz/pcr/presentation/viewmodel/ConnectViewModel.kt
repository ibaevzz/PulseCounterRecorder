package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.domain.usecase.CloseConnectionDeviceUseCase
import com.ibaevzz.pcr.domain.usecase.ConnectToDeviceUseCase
import javax.inject.Inject

class ConnectViewModel(private val connectToDeviceUseCase: ConnectToDeviceUseCase,
                       private val closeConnectionDeviceUseCase: CloseConnectionDeviceUseCase): ViewModel() {

    class Factory @Inject constructor(private val connectToDeviceUseCase: ConnectToDeviceUseCase,
                                      private val closeConnectionDeviceUseCase: CloseConnectionDeviceUseCase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ConnectViewModel::class.java){
                return ConnectViewModel(connectToDeviceUseCase, closeConnectionDeviceUseCase) as T
            }
            return super.create(modelClass)
        }
    }

    suspend fun connect(address: String){
        connectToDeviceUseCase(address)
    }

    fun closeConnection(){
        closeConnectionDeviceUseCase()
    }

}