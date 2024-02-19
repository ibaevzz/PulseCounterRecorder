package com.ibaevzz.pcr.data.repository

import android.net.wifi.WifiManager
import com.ibaevzz.pcr.data.exceptions.ConnectException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.di.InputQualifier
import com.ibaevzz.pcr.di.OutputQualifier
import com.ibaevzz.pcr.di.wifi.WifiScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import javax.inject.Inject

@WifiScope
class WifiPCRRepository @Inject constructor(private val wifiManager: WifiManager,
                                            @InputQualifier private val inputDispatcher: CoroutineDispatcher,
                                            @OutputQualifier private val outputDispatcher: CoroutineDispatcher): PCRRepository() {

    override var inputStream: InputStream? = null
    override var outputStream: OutputStream? = null
    private var socket: Socket? = null
    override val isConnect get() = socket?.isConnected?:false
    private val connectMutex = Mutex()
    private val closeMutex = Mutex()

    override suspend fun connect(address: String, port: String) {
        if(isConnect) return
        withContext(Dispatchers.IO) {
            connectMutex.lock()
            if(isConnect) return@withContext
            try {
                closeConnection()

                val ip = address.toInt()

                socket = Socket(InetAddress.getByAddress(byteArrayOf(
                    (ip).toByte(),
                    (ip ushr 8).toByte(),
                    (ip ushr 16).toByte(),
                    (ip ushr 24).toByte())), port.toInt())

                if (socket?.isConnected != true) throw ConnectException()
                inputStream = socket?.getInputStream()
                outputStream = socket?.getOutputStream()
            } finally {
                connectMutex.unlock()
            }
        }
    }

    override suspend fun closeConnection() {
        if(!wifiManager.isWifiEnabled) throw WifiTurnedOffException()
        if(!isConnect) return

        withContext(Dispatchers.IO){
            try {
                closeMutex.lock()
                if(!isConnect) return@withContext
                outputStream?.close()
                inputStream?.close()
                socket?.close()
            }finally {
                closeMutex.unlock()
            }
        }
    }

}