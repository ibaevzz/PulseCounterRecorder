package com.ibaevzz.pcr

import android.app.Application
import android.os.Build
import com.ibaevzz.pcr.di.app.AppComponent

var isClose = false

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        AppComponent.init(this)
        isClose = false

        Thread.setDefaultUncaughtExceptionHandler{_, error ->
            isClose = true
            val errorPref = getSharedPreferences(ERROR_SHARED_PREF, MODE_PRIVATE)
            val errors = errorPref.getStringSet(ERROR_SET, mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
            val edit = errorPref.edit()
            val model = Build.MODEL
            val version = Build.VERSION.RELEASE
            errors.add("Версия и тип устройства: $model  $version\nТекст ошибки: ${error.stackTraceToString()}")
            edit.putStringSet(ERROR_SET, errors).commit()
        }
    }

}