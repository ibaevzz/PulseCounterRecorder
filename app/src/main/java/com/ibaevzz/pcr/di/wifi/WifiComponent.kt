package com.ibaevzz.pcr.di.wifi

import android.content.Context
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.activity.ConnectActivity
import com.ibaevzz.pcr.presentation.activity.MenuPCRActivity
import com.ibaevzz.pcr.presentation.activity.WifiConnectActivity
import dagger.Subcomponent

@WifiScope
@Subcomponent(modules = [WifiModule::class])
interface WifiComponent {

    fun inject(connectActivity: ConnectActivity)
    fun inject(wifiConnectActivity: WifiConnectActivity)
    fun inject(menuActivity: MenuPCRActivity)

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