package com.ibaevzz.pcr.di.app

import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import dagger.Module

@Module(subcomponents = [WifiComponent::class, BluetoothComponent::class])
interface AppModule