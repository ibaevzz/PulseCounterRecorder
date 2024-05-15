package com.ibaevzz.pcr.di.wifi

import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import androidx.core.content.getSystemService
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

    @Provides
    @WifiScope
    fun provideLocationManager(context: Context): LocationManager{
        return context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

}

@Module
interface BindsModule{
    @Binds
    fun wifiPCRRepositoryToPCRRepository(wifiPCRRepository: WifiPCRRepository): PCRRepository
}