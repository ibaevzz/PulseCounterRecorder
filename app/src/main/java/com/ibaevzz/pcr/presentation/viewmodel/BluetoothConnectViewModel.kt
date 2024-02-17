package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ibaevzz.pcr.domain.entity.Device
import com.ibaevzz.pcr.domain.usecase.GetFoundDevicesUseCase
import com.ibaevzz.pcr.domain.usecase.StopSearchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private val _startConnectActivity = MutableSharedFlow<String?>()
    val startConnectActivity = _startConnectActivity.asSharedFlow()

    override fun onCleared() {
        super.onCleared()
        stopSearch()
    }

    fun getDevices(): SharedFlow<List<Device>>{
        return getFoundDevices()
    }

    fun callback(address: String){
        viewModelScope.launch(Dispatchers.Default){
            _startConnectActivity.emit(address)
        }
    }
}