package com.ibaevzz.pcr.domain.repository

import com.ibaevzz.pcr.domain.entity.Device
import kotlinx.coroutines.flow.SharedFlow

interface SearchDevice {
    fun search(): SharedFlow<List<Device>>
}