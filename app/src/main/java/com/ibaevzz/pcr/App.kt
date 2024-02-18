package com.ibaevzz.pcr

import android.app.Application
import com.ibaevzz.pcr.di.app.AppComponent

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        AppComponent.init(this)
    }

}