package com.ibaevzz.pcr.di.wifi

import android.content.Context
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.activity.*
import dagger.Subcomponent

@WifiScope
@Subcomponent(modules = [WifiModule::class])
interface WifiComponent {

    fun inject(connectActivity: ConnectActivity)
    fun inject(wifiConnectActivity: WifiConnectActivity)
    fun inject(menuActivity: MenuPCRActivity)
    fun inject(writeWeightActivity: WriteWeightActivity)
    fun inject(findChannelActivity: FindChannelActivity)
    fun inject(archiveActivity: ArchiveActivity)

    @Subcomponent.Builder
    interface Builder{
        fun build(): WifiComponent
    }

    companion object{

        @Volatile
        private var wifiComponent: WifiComponent? = null

        @Synchronized
        fun init(context: Context): WifiComponent {
            if(wifiComponent == null){
                wifiComponent = AppComponent
                    .init(context)
                    .getWifiComponentBuilder()
                    .build()
            }
            return wifiComponent!!
        }

    }

}