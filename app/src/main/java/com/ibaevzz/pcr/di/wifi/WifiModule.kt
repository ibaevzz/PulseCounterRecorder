package com.ibaevzz.pcr.di.wifi

import android.content.Context
import android.net.wifi.WifiManager
import com.ibaevzz.pcr.data.repository.PCRRepository
import com.ibaevzz.pcr.data.repository.WifiPCRRepository
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(includes = [BindsModule::class])
class WifiModule {

    @Provides
    @WifiScope
    fun provideWifiManager(context: Context): WifiManager{
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

}

@Module
interface BindsModule{
    @Binds
    fun wifiPCRRepositoryToPCRRepository(wifiPCRRepository: WifiPCRRepository): PCRRepository
}