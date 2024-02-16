package com.ibaevzz.pcr.domain.usecase

import com.ibaevzz.pcr.domain.entity.Device
import com.ibaevzz.pcr.domain.repository.SearchDevice
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

class GetFoundDevicesUseCase @Inject constructor(private val searchDeviceRepository: SearchDevice) {
    operator fun invoke(): SharedFlow<List<Device>> = searchDeviceRepository.search()
}