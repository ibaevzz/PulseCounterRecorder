package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class ConnectViewModel(private val PCRRepository: PCRRepository,
                       private val appScope: CoroutineScope): ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val appScope: CoroutineScope)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ConnectViewModel::class.java){
                return ConnectViewModel(PCRRepository, appScope) as T
            }
            return super.create(modelClass)
        }
    }

    private val _errorsSharedFlow = MutableSharedFlow<Exception>(replay = 1)
    val errorsSharedFlow = _errorsSharedFlow.asSharedFlow()

    private val _isConnect = MutableStateFlow<Boolean?>(false)
    val isConnect = _isConnect.asStateFlow()

    private val _rssi: MutableStateFlow<Short> = MutableStateFlow(0)
    val rssi = _rssi.asStateFlow()

    fun connect(data: String, port: String){
        viewModelScope.launch(Dispatchers.IO){
            try {
                _isConnect.emit(null)
                PCRRepository.connect(data, port)
                PCRRepository.getPCRAddress()
                PCRRepository.getDeviceType()
                PCRRepository.getChannelsValues()
                _isConnect.emit(true)
            }catch (ex: Exception) {
                _errorsSharedFlow.emit(ex)
                _isConnect.emit(false)
            }
        }
    }

    private fun closeConnection(){
        appScope.launch{
            try {
                PCRRepository.closeConnection()
            }catch (ex: Exception){
                _errorsSharedFlow.emit(ex)
            }finally {
                _isConnect.emit(false)
            }
        }
    }

    fun sendRssi(rssi: Short){
        viewModelScope.launch(Dispatchers.Default){
            _rssi.emit(rssi)
        }
    }

    override fun onCleared() {
        closeConnection()
        super.onCleared()
    }

}