package com.ibaevzz.pcr.di.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import com.ibaevzz.pcr.data.repository.BluetoothPCRRepository
import com.ibaevzz.pcr.data.repository.PCRRepository
import com.ibaevzz.pcr.di.InputQualifier
import com.ibaevzz.pcr.di.OutputQualifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

@Module(includes = [BindsModule::class])
class BluetoothModule {

    @Provides
    @BluetoothScope
    fun provideBluetoothManager(context: Context): BluetoothManager{
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @Provides
    @BluetoothScope
    @InputQualifier
    fun provideBluetoothInputDispatcher(): CoroutineDispatcher{
        return Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    @Provides
    @BluetoothScope
    @OutputQualifier
    fun provideBluetoothOutputDispatcher(): CoroutineDispatcher{
        return Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

}

@Module
interface BindsModule{
    @Binds
    fun bluetoothPCRRepositoryToPCRRepository(bluetoothPCRRepository: BluetoothPCRRepository): PCRRepository
}