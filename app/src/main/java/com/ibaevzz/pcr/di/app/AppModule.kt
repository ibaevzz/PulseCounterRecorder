package com.ibaevzz.pcr.di.app

import android.content.Context
import androidx.room.Room
import com.ibaevzz.pcr.DATABASE
import com.ibaevzz.pcr.data.db.PulsarDatabase
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module(subcomponents = [WifiComponent::class, BluetoothComponent::class])
class AppModule{
    @Provides
    @Singleton
    fun provideAppScope(): CoroutineScope{
        return CoroutineScope(Executors.newFixedThreadPool(3).asCoroutineDispatcher())
    }
    @Provides
    @Singleton
    fun provideDatabase(context: Context): PulsarDatabase{
        return Room.databaseBuilder(context, PulsarDatabase::class.java, DATABASE)
            .build()
    }
}