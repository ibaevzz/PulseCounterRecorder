package com.ibaevzz.pcr.domain.usecase

import com.ibaevzz.pcr.domain.repository.ConnectToDevice
import javax.inject.Inject

class ConnectToDeviceUseCase @Inject constructor(private val connectToDevice: ConnectToDevice){
    suspend operator fun invoke(address: String){
        connectToDevice.connect(address)
    }
}