package com.ibaevzz.pcr.di.wifi

import android.content.Context
import android.net.wifi.WifiManager
import com.ibaevzz.pcr.data.repository.PCRRepository
import com.ibaevzz.pcr.data.repository.WifiPCRRepository
import com.ibaevzz.pcr.di.InputQualifier
import com.ibaevzz.pcr.di.OutputQualifier
import com.ibaevzz.pcr.di.bluetooth.BluetoothScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

@Module(includes = [BindsModule::class])
class WifiModule {

    @Provides
    @WifiScope
    fun provideWifiManager(context: Context): WifiManager{
        return context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    @Provides
    @WifiScope
    @InputQualifier
    fun provideWifiInputDispatcher(): CoroutineDispatcher {
        return Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    @Provides
    @WifiScope
    @OutputQualifier
    fun provideWifiOutputDispatcher(): CoroutineDispatcher {
        return Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

}

@Module
interface BindsModule{
    @Binds
    fun wifiPCRRepositoryToPCRRepository(wifiPCRRepository: WifiPCRRepository): PCRRepository
}