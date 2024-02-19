package com.ibaevzz.pcr.di.app

import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

@Module(subcomponents = [WifiComponent::class, BluetoothComponent::class])
class AppModule{
    @Provides
    fun provideAppScope(): CoroutineScope{
        return CoroutineScope(Executors.newFixedThreadPool(3).asCoroutineDispatcher())
    }
}