package com.ibaevzz.pcr.di.app

import android.content.Context
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.activity.SendReportActivity
import com.ibaevzz.pcr.presentation.activity.PhotoActivity
import com.ibaevzz.pcr.presentation.activity.SendDatabaseActivity
import com.ibaevzz.pcr.presentation.activity.UsernameActivity
import com.ibaevzz.pcr.presentation.service.SendErrorService
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [AppModule::class])
interface AppComponent {

    fun getWifiComponentBuilder(): WifiComponent.Builder
    fun getBluetoothComponentBuilder(): BluetoothComponent.Builder
    fun inject(usernameActivity: UsernameActivity)
    fun inject(photoActivity: PhotoActivity)
    fun inject(sendDatabaseActivity: SendDatabaseActivity)
    fun inject(sendErrorService: SendErrorService)
    fun inject(sendReportActivity: SendReportActivity)

    @Component.Builder
    interface Builder{

        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }

    companion object{

        @Volatile
        private var appComponent: AppComponent? = null

        @Synchronized
        fun init(context: Context): AppComponent{
            if(appComponent == null){
                appComponent = DaggerAppComponent
                    .builder()
                    .context(context)
                    .build()
            }
            return appComponent!!
        }

    }
}