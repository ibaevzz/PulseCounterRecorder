package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ibaevzz.pcr.data.repository.ConnectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ConnectViewModel(private val connectRepository: ConnectRepository): ViewModel() {

    class Factory @Inject constructor(private val connectRepository: ConnectRepository)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ConnectViewModel::class.java){
                return ConnectViewModel(connectRepository) as T
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
                connectRepository.connect(data, port)
                _isConnect.emit(true)
            }catch (ex: Exception) {
                _errorsSharedFlow.emit(ex)
                _isConnect.emit(false)
            }
        }
    }

    fun closeConnection(){
        viewModelScope.launch(Dispatchers.IO){
            try {
                connectRepository.closeConnection()
                _isConnect.emit(false)
            }catch (ex: Exception){
                _errorsSharedFlow.emit(ex)
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
        super.onCleared()
        closeConnection()
    }

}