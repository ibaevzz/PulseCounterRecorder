package com.ibaevzz.pcr

import android.app.Application
import com.ibaevzz.pcr.di.AppComponent
import com.ibaevzz.pcr.di.DaggerAppComponent

class App: Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent
            .builder()
            .context(this)
            .build()
    }

}