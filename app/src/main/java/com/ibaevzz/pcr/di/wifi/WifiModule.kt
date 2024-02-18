package com.ibaevzz.pcr.di.wifi

import com.ibaevzz.pcr.data.repository.ConnectRepository
import com.ibaevzz.pcr.data.repository.WifiConnectRepository
import dagger.Binds
import dagger.Module

@Module
interface WifiModule {
    @Binds
    fun wifiConnectRepositoryToConnectRepository(wifiConnectRepository: WifiConnectRepository): ConnectRepository
}