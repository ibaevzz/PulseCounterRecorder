package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.domain.entity.Device
import com.ibaevzz.pcr.domain.usecase.GetFoundDevicesUseCase
import com.ibaevzz.pcr.domain.usecase.StopSearchUseCase
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class BluetoothConnectViewModel(private val getFoundDevices: GetFoundDevicesUseCase,
                                private val stopSearch: StopSearchUseCase): ViewModel(){

    class Factory @Inject constructor(private val getFoundDevices: GetFoundDevicesUseCase,
                                      private val stopSearch: StopSearchUseCase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == BluetoothConnectViewModel::class.java){
                return BluetoothConnectViewModel(getFoundDevices, stopSearch) as T
            }
            return super.create(modelClass)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSearch()
    }

    fun getDevices(): SharedFlow<List<Device>>{
        return getFoundDevices()
    }

    fun callback(address: String){
        //TODO
    }
}