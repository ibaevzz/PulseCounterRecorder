package com.ibaevzz.pcr.data.repository

import android.content.Context
import com.ibaevzz.pcr.di.wifi.WifiScope
import javax.inject.Inject

@WifiScope
class WifiConnectRepository @Inject constructor(private val context: Context): ConnectRepository {

    override suspend fun connect(address: String, port: String) {

    }

    override suspend fun closeConnection() {

    }

}