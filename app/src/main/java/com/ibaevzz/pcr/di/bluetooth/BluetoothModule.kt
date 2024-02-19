package com.ibaevzz.pcr.di.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import com.ibaevzz.pcr.data.repository.BluetoothPCRRepository
import com.ibaevzz.pcr.data.repository.PCRRepository
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(includes = [BindsModule::class])
class BluetoothModule {

    @Provides
    fun provideBluetoothManager(context: Context): BluetoothManager{
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
}

@Module
interface BindsModule{
    @Binds
    fun bluetoothPCRRepositoryToPCRRepository(bluetoothPCRRepository: BluetoothPCRRepository): PCRRepository
}