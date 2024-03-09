package com.ibaevzz.pcr.di.bluetooth

import android.content.Context
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.activity.*
import com.ibaevzz.pcr.presentation.service.RssiService
import dagger.Subcomponent

@BluetoothScope
@Subcomponent(modules = [BluetoothModule::class])
interface BluetoothComponent {

    fun inject(searchActivity: BluetoothSearchActivity)
    fun inject(connectActivity: ConnectActivity)
    fun inject(rssiService: RssiService)
    fun inject(menuActivity: MenuPCRActivity)
    fun inject(writeWeightActivity: WriteWeightActivity)
    fun inject(findChannelActivity: FindChannelActivity)
    fun inject(archiveActivity: ArchiveActivity)
    fun inject(channelActivity: ChannelActivity)
    fun inject(writeValuesActivity: WriteValuesActivity)

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