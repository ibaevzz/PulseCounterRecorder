package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ibaevzz.pcr.data.dto.Device
import com.ibaevzz.pcr.data.repository.SearchDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class BluetoothSearchViewModel(private val searchDeviceRepository: SearchDeviceRepository): ViewModel(){

    class Factory @Inject constructor(private val searchDeviceRepository: SearchDeviceRepository)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == BluetoothSearchViewModel::class.java){
                return BluetoothSearchViewModel(searchDeviceRepository) as T
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
        return searchDeviceRepository.search()
    }

    fun stopSearch(){
        searchDeviceRepository.stopSearch()
    }

    fun callback(address: String){
        viewModelScope.launch(Dispatchers.Default){
            _startConnectActivity.emit(address)
        }
    }
}