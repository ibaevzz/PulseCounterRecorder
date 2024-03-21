package com.ibaevzz.pcr

import android.os.Build
import kotlin.reflect.KClass

const val IBAEVZZ_ID = 854936011L

fun sendErrorInClass(kClass: KClass<*>, errorType: KClass<out Exception>, errorMessage: String): String{
    val model = Build.MODEL
    val version = Build.VERSION.RELEASE
    return "Версия и тип устройства: $model  $version\nКласс: ${kClass.simpleName}\nТип ошибки: ${errorType.simpleName}\nТекст ошибки: $errorMessage"
}