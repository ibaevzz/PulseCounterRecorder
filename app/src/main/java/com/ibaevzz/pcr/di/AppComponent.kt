package com.ibaevzz.pcr.di

import android.content.Context
import com.ibaevzz.pcr.data.repository.SearchDeviceRepository
import com.ibaevzz.pcr.domain.repository.SearchDevice
import com.ibaevzz.pcr.domain.repository.StopSearch
import com.ibaevzz.pcr.presentation.activity.BluetoothConnectActivity
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module

@Component(modules = [SearchModules::class])
interface AppComponent {

    fun inject(activity: BluetoothConnectActivity)

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
    fun bindSearchDeviceRepository(searchDeviceRepository: SearchDeviceRepository): SearchDevice

    @Binds
    fun bindStopSearchDeviceRepository(searchDeviceRepository: SearchDeviceRepository): StopSearch
}