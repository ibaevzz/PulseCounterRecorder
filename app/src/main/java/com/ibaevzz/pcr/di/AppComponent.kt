package com.ibaevzz.pcr.di

import android.content.Context
import com.ibaevzz.pcr.data.repository.BluetoothPCRRepository
import com.ibaevzz.pcr.data.repository.PCRRepository
import com.ibaevzz.pcr.data.repository.SearchDeviceRepository
import com.ibaevzz.pcr.data.repository.SearchRepository
import com.ibaevzz.pcr.domain.repository.CloseConnectionDevice
import com.ibaevzz.pcr.domain.repository.ConnectToDevice
import com.ibaevzz.pcr.domain.repository.SearchDevice
import com.ibaevzz.pcr.domain.repository.StopSearch
import com.ibaevzz.pcr.presentation.activity.BluetoothConnectActivity
import com.ibaevzz.pcr.presentation.activity.ConnectActivity
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Singleton

@Singleton
@Component(modules = [SearchModules::class])
interface AppComponent {

    fun inject(activity: BluetoothConnectActivity)
    fun inject(activity: ConnectActivity)

    @Component.Builder
    interface Builder{

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }
}

@Module
interface SearchModules{

    @Binds
    fun bindSearchDeviceRepository_SearchRepository(searchDeviceRepository: SearchDeviceRepository): SearchRepository

    @Binds
    fun bindSearchRepository_SearchDevice(searchRepository: SearchRepository): SearchDevice

    @Binds
    fun bindStopSearchRepository_StopSearch(searchRepository: SearchRepository): StopSearch

    @Binds
    fun bindBluetoothPCRRepository_PCRRepository(bluetoothPCRRepository: BluetoothPCRRepository): PCRRepository

    @Binds
    fun bindPCRRepository_ConnectToDevice(PCRRepository: PCRRepository): ConnectToDevice

    @Binds
    fun bindPCRRepository_CloseConnectionDevice(PCRRepository: PCRRepository): CloseConnectionDevice
}