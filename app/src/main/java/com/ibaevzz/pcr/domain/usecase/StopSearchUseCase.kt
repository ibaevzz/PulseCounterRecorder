package com.ibaevzz.pcr.domain.usecase

import com.ibaevzz.pcr.domain.repository.StopSearch
import javax.inject.Inject

class StopSearchUseCase @Inject constructor(private val searchDeviceRepository: StopSearch){
    operator fun invoke(){
        searchDeviceRepository.stopSearch()
    }
}