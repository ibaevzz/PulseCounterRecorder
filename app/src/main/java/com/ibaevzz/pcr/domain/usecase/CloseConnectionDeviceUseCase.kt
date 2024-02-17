package com.ibaevzz.pcr.domain.usecase

import com.ibaevzz.pcr.domain.repository.CloseConnectionDevice
import javax.inject.Inject

class CloseConnectionDeviceUseCase @Inject constructor(private val closeConnectionDevice: CloseConnectionDevice) {
    operator fun invoke(){
        closeConnectionDevice.closeConnection()
    }
}