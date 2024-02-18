package com.ibaevzz.pcr.di.bluetooth

import android.content.Context
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.activity.BluetoothSearchActivity
import com.ibaevzz.pcr.presentation.activity.ConnectActivity
import dagger.Subcomponent

@BluetoothScope
@Subcomponent(modules = [BluetoothModule::class])
interface BluetoothComponent {

    fun inject(searchActivity: BluetoothSearchActivity)
    fun inject(connectActivity: ConnectActivity)

    @Subcomponent.Builder
    interface Builder{
        fun build(): BluetoothComponent
    }

    companion object{

        @Volatile
        private var bluetoothComponent: BluetoothComponent? = null

        @Synchronized
        fun init(context: Context): BluetoothComponent {
            if(bluetoothComponent == null){
                bluetoothComponent = AppComponent
                    .init(context)
                    .getBluetoothComponentBuilder()
                    .build()
            }
            return bluetoothComponent!!
        }

    }

}